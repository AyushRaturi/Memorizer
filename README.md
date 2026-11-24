# Recall (Android)

Simple notepad + home-screen widget. Every time you unlock the phone, Recall picks the next
saved note (shuffled, no repeats until every note has been shown) and shows it on the widget.
Optionally it also posts a short pop-up notification.

## Build
1. Android Studio (current stable), JDK 17+ (the bundled JBR is fine), Android SDK 35.
2. Open this folder, let Gradle sync (wrapper uses Gradle 8.11.1).
3. Run on your phone.
4. Allow notifications when asked.
5. Long-press home screen -> Widgets -> Recall.

## Keep it working on Nothing OS
- Settings -> Apps -> Recall -> Battery -> set to **Unrestricted / No restrictions**.
- Leave the small "Recall is active" notification enabled (it's the background listener).
- For heads-up pop-ups: allow notifications and "pop-up / floating" notifications for Recall.

## How it works
- `UnlockService` is a foreground service that listens for `USER_PRESENT` (device unlocked).
  That broadcast can't be declared in the manifest on Android 8+, so it is registered at runtime.
- `BootReceiver` restarts the service after reboot or app update.
- Notes are stored locally in SharedPreferences, max 30 words each.
