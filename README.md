<div align="center">

<img src="docs/images/banner.svg" width="100%" alt="WiFi Switcher: Wi-Fi when it's there, hotspot when it's not">

<br><br>

[![Latest release](https://img.shields.io/github/v/release/dcoder-me/wifi-switcher?label=version&color=2563EB)](../../releases/latest)
[![Downloads](https://img.shields.io/github/downloads/dcoder-me/wifi-switcher/total?color=2563EB)](../../releases)
![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white)

### [⬇ Download the APK](../../releases/latest)

| Light | Dark | In action |
| :---: | :---: | :---: |
| <img src="docs/images/screenshot-light.png" width="240" alt="Main screen, light mode"> | <img src="docs/images/screenshot-dark.png" width="240" alt="Main screen, dark mode"> | <img src="docs/images/demo.gif" width="240" alt="Demo: app opens and the check interval changes"> |

</div>

A small Android app that flips your phone between Wi-Fi and mobile hotspot on its own:

| Your phone is… | WiFi Switcher does… |
| --- | --- |
| Connected to a saved Wi-Fi | Turns **off** mobile data and hotspot |
| Away from Wi-Fi | Turns **on** mobile data and hotspot |

Handy when your phone is the internet source for a laptop, tablet or a second phone, and you keep forgetting to switch.

## ✨ Features

<table>
  <tr>
    <td width="50%" valign="top">
      <h3>🔄 Switches on its own</h3>
      <p>Saved Wi-Fi in range: data and hotspot go off. Wi-Fi gone: both come back on.</p>
    </td>
    <td width="50%" valign="top">
      <h3>🔓 No root needed</h3>
      <p>Works with Shizuku. On a rooted phone it uses root automatically.</p>
    </td>
  </tr>
  <tr>
    <td width="50%" valign="top">
      <h3>⏱️ You set the pace</h3>
      <p>Checks for Wi-Fi every N minutes (default 10).</p>
    </td>
    <td width="50%" valign="top">
      <h3>🔁 Starts after a restart</h3>
      <p>Comes back by itself when the phone reboots.</p>
    </td>
  </tr>
</table>

## ⚙️ How it works

```mermaid
flowchart LR
    A["Every N minutes"] --> B["Pause hotspot ~25 s<br/>and scan"]
    B --> C{"Saved Wi-Fi<br/>nearby?"}
    C -- Yes --> D["Join Wi-Fi<br/>Data + hotspot off"]
    C -- No --> E["Data + hotspot on"]
```

- While the hotspot is on, the phone can't scan for Wi-Fi (both use the same radio). So every **N minutes** (default 10) the app pauses the hotspot for about 25 seconds to look for a saved Wi-Fi. Devices on your hotspot drop for those seconds.
- It only joins Wi-Fi networks already saved on your phone.
- It starts again by itself after the phone restarts.

## 📥 Download

Get the latest APK from the **[Releases page](../../releases/latest)**, copy it to your phone and install it (allow "Install unknown apps" when asked).

## 📋 Requirements

- Android 8.0+ (built and tested for **Android 11**)
- **[Shizuku](https://shizuku.rikka.app/)** (no root needed), **or** a rooted phone

## 🚀 Setup

### 1. Give it permission with Shizuku

1. Install **Shizuku** from the Play Store.
2. Settings → About phone → tap **Build number** 7 times (turns on Developer options).
3. Developer options → turn on **Wireless debugging**.
4. Open Shizuku → **Start via Wireless debugging** → follow the pairing steps.
5. Open WiFi Switcher → **Grant Shizuku permission** → Allow.

> After a phone restart, open Shizuku and start it again (an Android 11 limit).
> Rooted phone? Skip Shizuku; the app uses root automatically.

### 2. Turn it on

Flip the power switch. The status shows in the notification.

- With Shizuku, the hotspot uses the name and password from **Settings → Connections → Mobile Hotspot**. The name/password fields in the app apply only on rooted phones.
- Settings → Apps → WiFi Switcher → Battery → **Unrestricted**. Otherwise the phone may stop the app in the background.

## 🛠️ Troubleshooting

<details>
<summary><b>The hotspot doesn't turn on</b></summary>

Some brands (Xiaomi, Samsung, etc.) change the hotspot code. The status line shows the error. [Open an issue](../../issues) with that message and your phone model.

</details>

## 🧑‍💻 Build from source

<details>
<summary><b>Build steps</b></summary>

1. Install [Android Studio](https://developer.android.com/studio).
2. File → Open → this folder. Wait for Gradle sync.
3. Build → Build APK(s). The file lands in `app/build/outputs/apk/debug/`.

Or from a terminal: `./gradlew assembleDebug`

</details>

## 🏷️ Versions

Each release is a git tag (`v1.0.0`, …) with its APK attached on the [Releases page](../../releases). The app's version lives in `app/build.gradle` (`versionName`, `versionCode`).

## 🧱 Built with

Kotlin · [Shizuku API](https://github.com/RikkaApps/Shizuku-API) · [HiddenApiBypass](https://github.com/LSPosed/AndroidHiddenApiBypass)
