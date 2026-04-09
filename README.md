# android-parallax-app

Android Kotlin 範例專案，展示多層圖片 Parallax：

- 顯示多層圖片（前/中/後景）
- 透過 accelerometer 隨手機傾斜移動
- 可調整 Parallax 強度（SeekBar）
- 內含 `WallpaperService` 骨架，可延伸為 Live Wallpaper

## 專案結構

- `build.gradle.kts` / `settings.gradle.kts`: Gradle root 設定
- `app/`: Android app module
  - `MainActivity`: 即時預覽 + 強度調整
  - `ParallaxController`: 多層位移控制
  - `ParallaxWallpaperService`: Live Wallpaper Engine
