# Rainbow Countdown Timer - Specification

## 1. Project Overview
- **Project Name**: Rainbow Countdown Timer
- **Type**: Native Android Application (Kotlin, API 26+)
- **Core Functionality**: Visual countdown timer for toddlers featuring concentric rainbow rings that sweep away like clock hands as time elapses

## 2. Technology Stack & Choices

### Framework & Language
- **Language**: Kotlin 1.9.x
- **Min SDK**: API 26 (Android 8.0)
- **Target SDK**: 34 (latest stable)
- **Compile SDK**: 34

### Key Libraries/Dependencies
- **AndroidX Core KTX**: Core Kotlin extensions
- **AndroidX AppCompat**: Backward compatibility
- **AndroidX Activity KTX**: ViewModel integration
- **AndroidX Lifecycle ViewModel KTX**: MVVM ViewModel
- **AndroidX Lifecycle Runtime KTX**: Lifecycle-aware components
- **Material Components**: Material Design UI components
- **Kotlin Coroutines**: Asynchronous programming for timer ticks

### State Management
- **MVVM Architecture**: ViewModel holds all timer state
- **LiveData**: For UI state observation
- **Kotlin Coroutines**: For countdown tick mechanism (not Handler/postDelayed)

### Architecture Pattern
- **Clean MVVM**: Separate concerns
  - TimerViewModel: Timer logic and state
  - RingTimerView: Custom rendering (no business logic)
  - SoundManager: Audio handling
  - TimerRepository: Persistence (last-used time)

## 3. Feature List

### Setup Screen
- Custom number pad (0-9, backspace) with large touch targets
- MM:SS display field (right-to-left input like iOS Clock)
- Mode toggle (Mode 1: Fixed Rings / Mode 2: Fixed Rate)
- Start button (validates time > 0)
- Input validation and time capping for Mode 2 (max 5:00)

### Timer Screen
- Full-screen RingTimerView (concentric rainbow rings)
- Unobtrusive Reset button
- Keep screen on while timer runs (FLAG_KEEP_SCREEN_ON)
- No time digits, no mode label - immersive experience

### Ring Visual Behavior
- 20 rings maximum, concentric, centered
- Rainbow spectrum: red → orange → yellow → green → cyan → blue → violet
- Thin dark gap between adjacent rings
- Center: small solid black circle
- Sweep animation: counterclockwise from 12 o'clock position
- Rings deplete from outermost inward
- Only one ring active at a time
- Smooth animation driven by actual elapsed time

### Mode 1 (Fixed Rings)
- Always 20 rings active
- Each ring time slot = totalDuration / 20

### Mode 2 (Fixed Rate)
- Each ring = exactly 15 seconds
- Ring count = ceil(totalSeconds / 15), max 20
- Max input: 5:00 (300 seconds)
- Toast notification when capped

### Audio (End-of-Timer Alarm)
- Soft chime/bell sound (generated programmatically with AudioTrack)
- Volume ramps from 0 to full over 5 seconds (ease-in curve)
- Loops until user taps Reset
- Proper audio focus handling
- Lifecycle cleanup and graceful failure

### Lifecycle & Error Handling
- Screen rotation: timer continues, state survives via ViewModel
- App backgrounding: timer continues via coroutine scoped to app
- Return after expiry: alarm plays immediately
- Full error handling: invalid input, audio failures, lifecycle edge cases

### Logging
- TimerLogger wrapper class
- Toggleable for release builds

## 4. UI/UX Design Direction

### Overall Visual Style
- Clean, immersive, distraction-free
- Child-friendly but not childish
- High contrast rainbow colors on dark background

### Color Scheme
- Background: Near-black (#121212)
- Rings: Full rainbow spectrum (red → orange → yellow → green → cyan → blue → violet)
- Gaps between rings: Dark (#1A1A1A)
- Center circle: Solid black (#000000)
- UI elements: White/light gray on setup screen

### Layout Approach
- Setup Screen: Vertical stack - time display → number pad → mode toggle → start button
- Timer Screen: Full-screen RingTimerView with small reset button at bottom center

## 5. Constants (TimerConstants.kt)

```
MAX_RINGS = 20
SECONDS_PER_RING_MODE2 = 15
MAX_SECONDS_MODE2 = 300 (5:00)
ALARM_RAMP_DURATION_MS = 5000
GAP_BETWEEN_RINGS_DP = 2
CENTER_CIRCLE_RADIUS_DP = 8
```
