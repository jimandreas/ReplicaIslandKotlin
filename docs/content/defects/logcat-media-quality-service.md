---
title: "Logcat Media Quality Service"
date: 2025-01-01
draft: false
---

# Media Quality Service not found.

Logcat complaint about Media quality service.  Seen on an emulator. Appears to be a new bug courtesy of API 36:

MediaQualityManager

java.lang.Object
  android.media.quality.MediaQualityManager

Central system API to the overall media quality, which arbitrates interaction between applications and media quality service.

Don't see this running on a device with A12 / API 31.

Solution: Ignore for now
