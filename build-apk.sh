#!/usr/bin/env bash
set -euo pipefail

# Standard build for Android Studio / a machine with Android SDK configured.
# The signed APK delivered with this project was built with API 35.
gradle assembleDebug
