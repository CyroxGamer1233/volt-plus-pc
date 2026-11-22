//! Bounded, lossless storage of upstream control layout documents.
//! A recognized file is not necessarily executable: the relevant upstream
//! engine must validate and load it before the UI calls it active.
use sha2::{Digest, Sha256};
use std::{
    fs::{self, OpenOptions},
    io::Write,
    path::{Path, PathBuf},
    sync::atomic::{AtomicU64, Ordering},
};

pub const MAX_LAYOUT_BYTES: usize = 2 * 1024 * 1024;
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Format { Pojav, Zalith }
impl Format {
    fn directory(self) -> &'static str {
        match self { Self::Pojav => "pojav", Self::Zalith => "zalith" }
    }
}

/// Identify layouts that must never be routed into the unrelated Horizon
/// preset importer, including malformed or future upstream versions.
pub fn looks_like_java(bytes: &[u8]) -> bool {
    serde_json::from_slice::<serde_json::Value>(bytes).ok()
        .and_then(|value| value.as_object().map(|object| {
            object.contains_key("mControlDataList")
                || object.contains_key("layers")
                || object.contains_key("editorVersion")
        }))
        .unwrap_or(false)
}

/// Check only the format and version envelope. Full upstream validation still
/// belongs to the original engine; this function does not flatten any fields.
pub fn detect(bytes: &[u8]) -> Result<Format, String> {
    if bytes.len() > MAX_LAYOUT_BYTES { return Err("Control layout exceeds 2 MiB".into()); }
    let value: serde_json::Value = serde_json::from_slice(bytes)
        .map_err(|e| format!("Invalid control layout JSON: {e}"))?;
    let obj = value.as_object().ok_or("Control layout must be a JSON object")?;
    let pojav = obj.contains_key("mControlDataList");
    let zalith = obj.contains_key("layers") || obj.contains_key("editorVersion");
    if pojav && zalith { return Err("Ambiguous control layout format".into()); }
    if pojav {
        if !obj["mControlDataList"].is_array()
            || obj.get("mDrawerDataList").is_some_and(|v| !v.is_array())
        { return Err("Invalid Pojav controls or drawers".into()); }
        let version = match obj.get("version") {
            None => 1,
            Some(v) => v.as_u64().ok_or("Invalid Pojav layout version")?,
        };
        if !(1..=8).contains(&version) { return Err(format!("Unsupported Pojav layout version {version}")); }
        return Ok(Format::Pojav);
    }
    if zalith {
        if !obj.get("info").is_some_and(|v| v.is_object())
            || !obj.get("layers").is_some_and(|v| v.is_array())
            || obj.get("styles").is_some_and(|v| !v.is_array())
        { return Err("Invalid Zalith layout information, layers or styles".into()); }
        let version = obj.get("editorVersion").and_then(|v| v.as_u64())
            .ok_or("Missing or invalid Zalith editor version")?;
        if !(1..=12).contains(&version) { return Err(format!("Unsupported Zalith layout version {version}")); }
        return Ok(Format::Zalith);
    }
    Err("Unrecognized control layout format".into())
}

fn safe_directory(home: &Path, format: Format) -> Result<PathBuf, String> {
    let root = fs::canonicalize(home).map_err(|e| format!("Could not open profile directory: {e}"))?;
    let mut dir = root.clone();
    for component in ["controls", "java", format.directory()] {
        dir.push(component);
        match fs::symlink_metadata(&dir) {
            Ok(m) if m.is_dir() && !m.file_type().is_symlink() => {},
            Ok(_) => return Err("Refusing redirected control layout directory".into()),
            Err(e) if e.kind() == std::io::ErrorKind::NotFound => {
                match fs::create_dir(&dir) {
                    Ok(()) => {},
                    Err(e) if e.kind() == std::io::ErrorKind::AlreadyExists => {},
                    Err(e) => return Err(format!("Could not create control layout directory: {e}")),
                }
            }
            Err(e) => return Err(format!("Could not inspect control layout directory: {e}")),
        }
        let actual = fs::canonicalize(&dir).map_err(|e| e.to_string())?;
        if actual != dir { return Err("Refusing redirected control layout directory".into()); }
    }
    Ok(dir)
}

/// Store the exact supplied bytes under a content-derived name, without using
/// a provider-controlled filename or replacing an unrelated user's layout.
pub fn store(home: &Path, bytes: &[u8]) -> Result<(Format, PathBuf), String> {
    let format = detect(bytes)?;
    let dir = safe_directory(home, format)?;
    let digest = format!("{:x}", Sha256::digest(bytes));
    let target = dir.join(format!("{digest}.json"));
    if let Ok(m) = fs::symlink_metadata(&target) {
        if !m.is_file() || m.file_type().is_symlink() { return Err("Refusing redirected control layout file".into()); }
        if m.len() != bytes.len() as u64 { return Err("Existing control layout has an unexpected size".into()); }
        let existing = fs::read(&target).map_err(|e| e.to_string())?;
        if existing == bytes { return Ok((format, target)); }
        return Err("Existing control layout has an unexpected checksum".into());
    }
    static SERIAL: AtomicU64 = AtomicU64::new(0);
    let temp = dir.join(format!(".import-{}-{}.tmp", std::process::id(), SERIAL.fetch_add(1, Ordering::Relaxed)));
    let result = (|| -> Result<(), String> {
        let mut file = OpenOptions::new().write(true).create_new(true).open(&temp).map_err(|e| e.to_string())?;
        file.write_all(bytes).map_err(|e| e.to_string())?;
        file.sync_all().map_err(|e| e.to_string())?;
        drop(file);
        fs::rename(&temp, &target).map_err(|e| e.to_string())?;
        Ok(())
    })();
    if result.is_err() { let _ = fs::remove_file(&temp); }
    result?;
    Ok((format, target))
}

#[cfg(test)]
mod tests {
    use super::*;
    const POJAV: &[u8] = include_bytes!(concat!(env!("CARGO_MANIFEST_DIR"), "/../../tests/control_engine_build/fixtures/pojav-v4.json"));
    const ZALITH: &[u8] = include_bytes!(concat!(env!("CARGO_MANIFEST_DIR"), "/../../tests/control_engine_build/layer/src/test/resources/zalith-v11-fixture.json"));
    #[test]
    fn fixtures_are_recognized_and_stored_without_mutation() {
        let home = std::env::temp_dir().join(format!("volt-layout-store-{}", std::process::id()));
        let _ = fs::remove_dir_all(&home);
        fs::create_dir_all(&home).unwrap();
        for (bytes, expected) in [(POJAV, Format::Pojav), (ZALITH, Format::Zalith)] {
            assert!(looks_like_java(bytes));
            assert_eq!(detect(bytes).unwrap(), expected);
            let (kind, file) = store(&home, bytes).unwrap();
            assert_eq!(kind, expected);
            assert_eq!(fs::read(&file).unwrap(), bytes);
            assert_eq!(store(&home, bytes).unwrap().1, file);
        }
        assert!(!home.join("horizon-touch.json").exists());
        fs::remove_dir_all(home).unwrap();
    }
    #[test]
    fn malformed_ambiguous_and_newer_layouts_fail_closed() {
        for input in ["[]", "{}", "{", "{\"mControlDataList\": [], \"version\": 9}",
            "{\"layers\": [], \"editorVersion\": 99, \"info\": {}}",
            "{\"mControlDataList\": [], \"layers\": []}"] {
            assert!(detect(input.as_bytes()).is_err(), "{input}");
        }
        assert!(detect(&vec![b' '; MAX_LAYOUT_BYTES + 1]).is_err());
        assert!(!looks_like_java(br#"{"custom_buttons": []}"#));
        assert!(looks_like_java(br#"{"mControlDataList": [], "version": 99}"#));
    }
    #[cfg(unix)]
    #[test]
    fn redirected_directory_is_refused() {
        use std::os::unix::fs::symlink;
        let home = std::env::temp_dir().join(format!("volt-layout-symlink-{}", std::process::id()));
        let _ = fs::remove_dir_all(&home);
        fs::create_dir_all(&home).unwrap();
        symlink(std::env::temp_dir(), home.join("controls")).unwrap();
        assert!(store(&home, POJAV).is_err());
        fs::remove_dir_all(home).unwrap();
    }
}
