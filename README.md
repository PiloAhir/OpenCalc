# OpenCalc

Offline-first Android calculator made with Kotlin and native Android UI.

## Features
- Arithmetic, brackets, percentage, powers, factorial, pi and e
- Scientific functions: sin/cos/tan (degrees), sqrt, log, ln, abs
- Memory: MC, MR, M+, M−
- Local calculation history, copy and reopen prior expressions
- Unit conversion: length, weight, temperature, area, volume, speed, time, data
- Finance tools: GST add/remove, simple/compound interest and discount
- Dark/light themes; no account, paid API, or network permission

## Build with GitHub Actions
1. Create a repository and upload all project files while preserving folders.
2. Push to `main` or open **Actions → Build OpenCalc APK → Run workflow**.
3. Download the `OpenCalc-debug-apk` artifact.
4. Install `app-debug.apk` on your Android phone.

Build command: `gradle assembleDebug --no-daemon` with JDK 17 and Gradle 8.9.
APK: `app/build/outputs/apk/debug/app-debug.apk`

## License
MIT License. See `LICENSE`.
