# Rainbow Timer

A native Android countdown timer app designed for toddlers, featuring concentric rainbow rings that sweep away as time elapses.

## Features

- **Custom Number Pad**: Enter time in MM:SS format with a clean, intuitive input
- **Two Timer Modes**:
  - **Mode 1 (Fixed Rings)**: Always displays 20 rings, scaled to fit the total duration
  - **Mode 2 (Fixed Rate)**: Each ring represents 15 seconds (max 5:00)
- **Visual Ring Animation**: Rings sweep counterclockwise from 12 o'clock, depleting from outermost to innermost
- **Soft Chime Alarm**: Gentle programmatic chime plays when timer completes, with 5-second volume ramp-up
- **Immersive Experience**: No time digits or mode labels during countdown

## Requirements

- Android 8.0 (API 26) or higher
- Java 17
- Android SDK 34

## Building

```bash
# Generate Gradle wrapper (if needed)
export JAVA_HOME=/path/to/jdk-17
./gradlew assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`

## Project Structure

```
app/src/main/java/com/rainbowtimer/
├── audio/SoundManager.kt        # Alarm audio with volume ramp
├── data/TimerRepository.kt     # SharedPreferences persistence
├── ui/
│   ├── setup/SetupActivity.kt   # Time input, mode selection
│   └── timer/TimerActivity.kt   # Ring visualization
├── util/
│   ├── TimerConstants.kt       # Magic numbers
│   └── TimerLogger.kt         # Debug logging wrapper
├── view/
│   ├── RingTimerState.kt       # Data model
│   └── RingTimerView.kt        # Custom ring rendering
└── viewmodel/TimerViewModel.kt # MVVM logic
```

## Architecture

- **MVVM**: ViewModel holds all timer state
- **Kotlin Coroutines**: Timer ticks use coroutines (not Handler)
- **Custom View**: RingTimerView is fully decoupled from business logic

## Constants

All configurable values are in `TimerConstants.kt`:

| Constant | Value | Description |
|----------|-------|-------------|
| MAX_RINGS | 20 | Maximum number of rings |
| SECONDS_PER_RING_MODE2 | 15 | Seconds per ring in Mode 2 |
| MAX_SECONDS_MODE2 | 300 | Max input in Mode 2 (5:00) |
| MAX_INPUT_SECONDS_MODE1 | 3600 | Max input in Mode 1 (1 hour) |
| MIN_INPUT_SECONDS | 1 | Minimum valid input |
| ALARM_RAMP_DURATION_MS | 5000 | Volume ramp time (ms) |
| TICK_INTERVAL_MS | 16 | Animation tick interval (ms) |
| GAP_BETWEEN_RINGS_DP | 2 | Ring gap in dp |
| CENTER_CIRCLE_RADIUS_DP | 8 | Center circle radius in dp |

## License

MIT
