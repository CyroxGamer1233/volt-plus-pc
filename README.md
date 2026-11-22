<p align="center">
  <img src="packaging/android/res/drawable-nodpi/launcher_icon.png" width="160" height="160" alt="Volt Plus logo" />
</p>

<h1 align="center">Volt Plus</h1>

<p align="center">
  <b>A high-performance native Android port of Minecraft: Java Edition 26.3, rewritten in Rust with <code>wgpu</code> (Vulkan &amp; OpenGL ES).</b>
</p>

<p align="center">
  <a href="https://github.com/entitybrian69-bit/volt-plus/releases"><img src="https://img.shields.io/github/downloads/entitybrian69-bit/volt-plus/total?style=for-the-badge&label=Downloads&logo=github&logoColor=white" alt="Total downloads" /></a>
  <a href="https://github.com/entitybrian69-bit/volt-plus/releases/latest"><img src="https://img.shields.io/github/v/release/entitybrian69-bit/volt-plus?style=for-the-badge&label=Release&logo=github&logoColor=white" alt="Latest release" /></a>
  <a href="https://github.com/entitybrian69-bit/volt-plus/releases"><img src="https://img.shields.io/github/downloads/entitybrian69-bit/volt-plus/latest/total?style=for-the-badge&label=Latest%20APK%20downloads&logo=android&logoColor=white" alt="Downloads of the latest release" /></a>
</p>

<p align="center">
  <a href="https://discord.gg/WsBxGsKyhU"><img src="https://img.shields.io/badge/Join%20the%20Discord-WsBxGsKyhU-5865F2?style=for-the-badge&logo=discord&logoColor=white" alt="Join the Volt Plus Discord" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-4527A0?style=for-the-badge&logo=opensourceinitiative&logoColor=white" alt="License: MIT" /></a>
  <a href="https://github.com/entitybrian69-bit/volt-plus"><img src="https://img.shields.io/badge/Minecraft%20Java-26.3-5D9C3F?style=for-the-badge&logo=curseforge&logoColor=white" alt="Minecraft Java 26.3" /></a>
  <a href="https://github.com/entitybrian69-bit/volt-plus"><img src="https://img.shields.io/badge/Android-API%2028--35-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android 9.0+" /></a>
</p>

<p align="center">
  <a href="https://github.com/entitybrian69-bit/volt-plus/releases/latest"><b>⬇ Download the latest APK</b></a>
  &nbsp;·&nbsp;
  <a href="https://discord.gg/WsBxGsKyhU"><b>💬 Discord</b></a>
  &nbsp;·&nbsp;
  <a href="docs/android.md">Build guide</a>
  &nbsp;·&nbsp;
  <a href="CONTRIBUTING_ANDROID.md">Contributing</a>
</p>

**Android 9+ (API 28–35) · Vulkan 1.0+ / GLES 3.0 · Full SAF read & write · In-app updater · 260 FPS uncapped · Java multiplayer**

---

## Watch the intro

<p align="center">
  <a href="https://github.com/entitybrian69-bit/volt-plus/blob/android/packaging/android/media/intro.mp4?raw=true" title="Play the intro with sound">
    <img src="docs/media/volt-plus/intro.gif" width="720" alt="Volt Plus intro animation — click to play with sound" />
  </a>
  <br />
  <a href="https://github.com/entitybrian69-bit/volt-plus/blob/android/packaging/android/media/intro.mp4?raw=true"><b>▶ Play the intro with sound</b></a>
  <br />
  <sub>The same 5-second clip the launcher plays on first start (<code>packaging/android/media/intro.mp4</code>, 1080p60, 4.2 MB).</sub>
</p>

---

## Screenshots

<p align="center">
  <img src="docs/media/volt-plus/launcher.jpg" width="100%" alt="Volt Plus launcher home" />
</p>

| **Mods** | **Resource Packs** |
| :---: | :---: |
| ![Mods tab: 260 curated mods, one-tap install](docs/media/volt-plus/mods.jpg) | ![Resource packs catalog with Modrinth results](docs/media/volt-plus/resource-packs.jpg) |

| **Shader Packs** | **Content Library** |
| :---: | :---: |
| ![Shader packs: Volt and Modrinth catalogs](docs/media/volt-plus/shader-packs.jpg) | ![Installed content: mods, packs and shaders](docs/media/volt-plus/content-library.jpg) |

| **Skins & Capes Studio** | **Settings** |
| :---: | :---: |
| ![Skins and capes studio with the 3D character preview](docs/media/volt-plus/skins.jpg) | ![Settings: video, launcher, controls, diagnostics, account](docs/media/volt-plus/settings.jpg) |

---

## What's inside

- 🚀 **Full read/write storage**: import, export, move, rename and delete across external storage through the Storage Access Framework.
- 📁 **Content library**: mods (`.hmod`), resource packs and shaders in one place, with a built-in file manager for the folders behind them.
- 🔄 **In-app updater**: checks GitHub releases on start, downloads in the background, shows the changelog and installs the APK.
- ⚡ **Performance**: uncapped frame rate up to 260 FPS, battery-saver VSync mode, resolution scaling 25–100 %, fast chunk uploads, cave and entity culling.
- 🎮 **Touch controls**: four control styles including MCPE Bedrock, dynamic floating joystick, the Volt+ Control Hub and a full on-screen layout editor with opacity and sizing.
- 🎬 **Screen recorder**: 240p–1440p × 30–165 FPS, microphone / in-game / no audio, raw gameplay (HUD hidden) or with controls, straight into `Movies/VoltPlus`.
- 🌐 **Java multiplayer**: direct connections to Java Edition servers and LAN worlds, online or offline mode.

---

## Building from source

### Prerequisites
- **Android SDK** (API 35, Build-Tools 35.0.0+)
- **Android NDK** (28.2.13676358 / r28+)
- **Rust toolchain** (1.90+ with the `aarch64-linux-android` and `armv7-linux-androideabi` targets)
- **JDK** 17 or 21
- **Python** 3.10+

### Build the APK
```sh
# Signed ARM64 build
python3 tools/android_build.py --abi arm64-v8a --public --profile release

# 32-bit ARM build
python3 tools/android_build.py --abi armeabi-v7a --public --profile release
```

The output lands under `.local/android/apk/`. Full developer documentation is in [docs/android.md](docs/android.md).

---

## Contributing

Contributions, bug reports and optimisations are welcome.
- Read [CONTRIBUTING_ANDROID.md](CONTRIBUTING_ANDROID.md) before opening a pull request.
- Run the self-checks first:
  ```sh
  python3 -m unittest discover -s tests -p 'test_android*.py'
  python3 tools/check_public_tree.py
  ```

---

## License & legal

- Code is licensed under the **[MIT License](LICENSE)**.
- Minecraft is a trademark of Mojang Synergies AB. This project is not affiliated with, endorsed by or associated with Mojang or Microsoft.
- No proprietary game assets, jars or binary resources are stored in this repository.

---

<p align="center">
  <img src="docs/media/volt-plus/owner.jpg" width="220" alt="Volt Plus owner" />
</p>

<h3 align="center">Owner</h3>

<p align="center">
  <b>Volt Plus</b> is built and maintained by its owner.<br />
  Questions, bug reports and builds: <a href="https://discord.gg/WsBxGsKyhU">Discord</a> · <a href="https://github.com/entitybrian69-bit/volt-plus">GitHub</a>
</p>
