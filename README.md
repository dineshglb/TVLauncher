# Simple TV Launcher

Minimal Android TV home screen: clean app grid, clock, and the ability to HIDE apps
(hold OK on an app). No ads, no recommendations, no background services.

## Build
1. Open this folder in Android Studio (it will sync Gradle automatically).
2. Build > Build APK(s)  ->  app/build/outputs/apk/debug/app-debug.apk

## Install & set as Home (ADB)
    adb connect <TV_IP>:5555
    adb install app-debug.apk
    adb shell cmd package set-home-activity com.example.simpletvlauncher/.MainActivity
(If the TV blocks that, pressing Home should show a "choose launcher" prompt;
pick Simple Launcher > Always.)

## Debloat the stock apps (optional)
    adb shell pm list packages | grep -i <keyword>
    adb shell pm uninstall -k --user 0 <package.name>      # remove for current user
    adb shell cmd package install-existing <package.name>  # undo
Do NOT remove anything you don't recognise (Play Services, Settings, system UI,
the TV's input/HDMI apps). Keep the stock launcher installed so you can switch back.

## No Android Studio? Build online
Push this folder to a GitHub repo -> Actions tab -> "Build APK" -> download the
SimpleTVLauncher-debug-apk artifact. Then run the APK in a browser emulator
(Appetize.io free plan or apkonline.net).
