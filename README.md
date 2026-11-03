# YouTube Music Downloader

A standalone, on-device YouTube audio downloader built with Kotlin and Jetpack Compose (Material 3).

## Features
- Pure on-device execution without external backend servers.
- High-fidelity audio extraction and transcoding (MP3, M4A, OPUS, FLAC).
- Bundled yt-dlp runtime and FFmpeg binaries.
- Safe local cookie import (`cookies.txt`) via Storage Access Framework to bypass bot-checks and age restrictions.
- Automatic metadata embedding (title, artist, album cover art).
- Direct export to public `Music/YTDownloader` folder via Android MediaStore (Scoped Storage).
- Background Foreground Service with real-time download and transcoding progress notification.
- In-app Auto Updater from GitHub Releases.

## Architecture
- Kotlin + Jetpack Compose Material 3
- Architecture: SOLID principles, Clean Architecture with lightweight Dependency Inversion
- Core Engine: `youtubedl-android` (yt-dlp + FFmpeg)
- Minimum SDK: 24 | Target SDK: 36

