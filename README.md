# Cipher Launcher

A tiny Android launcher built for watches and other small Android devices.

## Concept

Cipher Launcher has two unlock paths:

- **Watch Mode** — a clean watch face with time, date, steps, and battery.
- **Launcher Mode** — the full Android home experience with installed apps and settings.

The two PINs are intentionally separate so the same device can have a minimal public-facing mode and a private full launcher mode.

## Current milestone

- Android launcher / Home activity
- First-run dual PIN setup
- PBKDF2-hashed PIN storage with per-PIN salts
- Minimal Watch Mode
- Full launcher grid
- Installed-app launching
- Step counter support
- Battery display
- AMOLED and seconds preferences
- PIN changes
- Default-launcher settings shortcut

## Build

Open the repository in Android Studio and let Gradle sync.

Requirements:

- JDK 17
- Android SDK 36
- Android Gradle Plugin 9.4.x

The project targets Android 10+ because the first release uses the activity recognition permission for the step counter.

## Roadmap

Cipher Launcher is intended to grow into a dedicated watch-oriented Android shell with richer watch faces, widgets, configurable launcher layouts, animations, and a more watch-native PIN keypad.
