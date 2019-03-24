# Android App Powered by HERE Mobile SDK
A sample app to demonstrate the use of HERE Mobile SDK. The app will persist any anchors placed by on the map via Android's RoomDatabase.

## Requirements
* HERE Mobile SDK for Android
* App id, token, key from HERE download portal
* Android 6.0+ (Marshmallow) phone
* Android Studio 2.2+

## Build
1. Place the HERE-sdk.aar file into the app/libs folder
2. Open the Android project
3. In the AndroidManifest, place the app id, token, and key in the appropriate places. Refer to HERE Quickstart guide.

## How to Use
* Ensure phone has GPS signal
* Long tap to place anchors
* Tap anchor to start navigation, and stop button to stop navigation
* Clear persistent anchors with the clear button