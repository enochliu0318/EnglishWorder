# EnglishWorder

Android vocabulary learning app with custom word lists, Excel/CSV import, dictionary auto-fetch, spaced repetition, and game-based review modes.

## Features

- **Word lists**: Create multiple custom vocabulary lists
- **Import**: Manual entry, CSV, or Excel (.xlsx) batch import
- **Auto-fetch**: Automatically fetch phonetics and definitions via Free Dictionary API
- **Learn mode**: Flashcard-style learning with phonetics, meanings, examples, and TTS
- **Spaced repetition**: Ebbinghaus forgetting curve (SM-2 variant) scheduling
- **Game review**: Quiz, link-match, spelling, and match-3 modes
- **Notifications**: Daily review reminders via WorkManager

## Requirements

- Android Studio Ladybug or newer
- JDK 17
- Android SDK 35
- Min SDK 26
- **JDK 17–21**（不要用系统自带的 Java 26/28，项目已配置使用 Android Studio 自带 JDK）

## Build

```bash
./gradlew assembleDebug
```

## Import Format

CSV/Excel first column should be the word. Optional columns: `phonetic`, `meaning`, `example`.

```
word,phonetic,meaning,example
abandon,/əˈbændən/,to leave permanently,They had to abandon the ship.
```

Chinese headers are also supported: `单词`, `音标`, `释义`, `例句`.

## Architecture

- Kotlin + Jetpack Compose + Material 3
- MVVM + Clean Architecture
- Room, Hilt, Retrofit, WorkManager
