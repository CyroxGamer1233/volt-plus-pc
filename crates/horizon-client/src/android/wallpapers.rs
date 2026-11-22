//! Original-quality wallpaper catalog and bounded, verified downloads.
//! Playback/compositing is deliberately not exposed until the Vulkan video path exists.
use sha2::{Digest, Sha256};
use std::{io::{Read, Write}, path::{Path, PathBuf}, time::Duration};

pub struct Wallpaper {
    pub name: &'static str,
    pub filename: &'static str,
    pub size: u64,
    pub sha256: &'static str,
    pub resolution: &'static str,
}

pub const WALLPAPERS: [Wallpaper; 5] = [
    Wallpaper { name: "Aurora night cabin", filename: "aurora.mp4", size: 8_272_647, sha256: "1785dbd85c19788dacdec03f0bc2f82656b4e66e90a071e7fbc97fcc39479d5a", resolution: "4K" },
    Wallpaper { name: "Cherry sunrise valley", filename: "cherry-sunrise.mp4", size: 5_930_493, sha256: "e87d9e48adebd0f5624255a9aecb6c78285fa85971b61a617135b48992f88f7e", resolution: "4K" },
    Wallpaper { name: "Minecraft Earth", filename: "earth.mp4", size: 14_365_508, sha256: "4253f906602eccad70b74f7db4c3bfb63e5a2e8c8d6a3082a307aadd308f11ad", resolution: "4K" },
    Wallpaper { name: "Peaceful greenhouse", filename: "greenhouse.mp4", size: 11_862_687, sha256: "95e479d8e9428630a8245e6371bcf810ba15e65ae4340047a893572a7f6a0d15", resolution: "1080p" },
    Wallpaper { name: "Sakura blossom house", filename: "sakura-house.mp4", size: 9_270_425, sha256: "ed7f53d57cb874725b870a8f571d30fb20e1b9047a51fe2bce3b254293bd285e", resolution: "1080p" },
];

fn digest_file(path: &Path, expected_size: u64) -> Result<String, String> {
    let mut file = std::fs::File::open(path).map_err(|e| e.to_string())?;
    if file.metadata().map_err(|e| e.to_string())?.len() != expected_size {
        return Err("File size does not match".into());
    }
    let mut hash = Sha256::new();
    let mut buf = [0u8; 64 * 1024];
    loop {
        let n = file.read(&mut buf).map_err(|e| e.to_string())?;
        if n == 0 { break; }
        hash.update(&buf[..n]);
    }
    Ok(format!("{:x}", hash.finalize()))
}

pub fn path(home: &Path, index: usize) -> Option<PathBuf> {
    Some(home.join("wallpapers").join(WALLPAPERS.get(index)?.filename))
}

/// Run off the UI thread. Only a complete, hash-verified file is installed.
pub fn download(home: &Path, index: usize) -> Result<String, String> {
    let item = WALLPAPERS.get(index).ok_or("Unknown wallpaper")?;
    let target = path(home, index).ok_or("Unknown wallpaper")?;
    let dir = target.parent().ok_or("Invalid wallpaper path")?;
    std::fs::create_dir_all(dir).map_err(|e| e.to_string())?;
    if digest_file(&target, item.size).is_ok_and(|v| v == item.sha256) {
        return Ok(format!("{} is already downloaded", item.name));
    }
    let tmp = target.with_extension("mp4.part");
    let result = (|| -> Result<(), String> {
        let url = format!("https://raw.githubusercontent.com/entitybrian69-bit/volt-plus/arena/e973ac10-volt-plus/wallpapers/{}", item.filename);
        let agent = ureq::AgentBuilder::new().timeout_connect(Duration::from_secs(20)).timeout_read(Duration::from_secs(45)).build();
        let response = agent.get(&url).call().map_err(|e| format!("Download failed: {e}"))?;
        let mut input = response.into_reader().take(item.size + 1);
        let mut output = std::fs::File::create(&tmp).map_err(|e| e.to_string())?;
        std::io::copy(&mut input, &mut output).map_err(|e| e.to_string())?;
        output.flush().map_err(|e| e.to_string())?;
        drop(output);
        if digest_file(&tmp, item.size)? != item.sha256 {
            return Err("Downloaded video failed its integrity check".into());
        }
        std::fs::rename(&tmp, &target).map_err(|e| e.to_string())?;
        Ok(())
    })();
    if result.is_err() { let _ = std::fs::remove_file(&tmp); }
    result.map(|()| format!("{} downloaded in original quality", item.name))
}
