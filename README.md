# NoTouchShoppingCart

Android controller prototype for an **unmanned / autonomous shopping cart**.

This project was developed as part of an undergraduate research project in 2019. The Android client discovers and connects to a cart over Bluetooth, provides manual controls, and exchanges motion-related data with the vehicle.

## Features

- Bluetooth device discovery, pairing, and serial communication
- Manual directional control
- Automatic / manual operation modes
- Foreground Bluetooth service
- Step detection and device-orientation sensing
- GPS-based movement estimation
- Android Navigation-based multi-screen UI

## Tech stack

- Kotlin / Java
- Android SDK
- Bluetooth Classic
- Android sensors and location APIs

## Related publication

This project is related to an undergraduate paper published in 2019.

- [KCI article page](https://www.kci.go.kr/kciportal/ci/sereArticleSearch/ciSereArtiView.kci?sereArticleSearchBean.artiId=ART002534750)

## Repository notes

This is a **historical prototype** and is not actively maintained. It targets an older Android SDK and may require changes to build or run on current Android versions.

No device-specific Bluetooth address or external service credential is committed in the repository; the target device address is selected at runtime.
