---
title: 'Jacquard Android SDK'
components:
  - type: 'pageContent'
---

## Getting started
Jacquard&#8482; by Google weaves new digital experiences into the things you
love, wear, and use every day to give you the power to do more and be
more.  Jacquard SDK is a way to connect Jacquard interactions within
your apps.  Create an app and bring it to life with swipes and taps
through the Jacquard SDK.

## What do I need to get started?
The Android Jacquard SDK supports Android versions 23 and greater.

You will need the Jacquard Tag with a supported Jacquard product (all come with one tag). Currently supported products are:

- Levi's Trucker Jacket
- Samsonite Konnect-i backpack
- Saint Laurent Cit-e Backpack \
You can find links to purchase these products on the [Google Jacquard website](https://atap.google.com/jacquard/products/).

## Sample App
To run the example project, 
- Clone the git repo
- Open sample app `build.gradle` file and update your keys as below. 
    ```
    buildConfigField "String", "API_KEY", "\"<Your-api-key>\""
    buildConfigField "String", "MAPS_API_KEY", "\"<Maps-api-key>\""
    resValue("string", "maps_api_key", "\"<Maps-api-key>\"")
    ```
  You can obtain `API_KEY` on [Google Jacquard website](wiki/cloud-api-terms) 
  whereas `MAPS_API_KEY` can be obtained from [Google Maps Platform](https://developers.google.com/maps/documentation/places/android-sdk/get-api-key).
- You are now all set to run the sample app.

## Next Steps
The best way to get started with the Jacquard SDK is to follow our tutorial.

Once you have completed the tutorial, the best place to go next is the API Overview which will explain the features of your Jacquard tag and gear, and how to use the API. After that, check out the API Documentation (available in the table of contents on the left of every page) and build your awesome app :)

## Copyright
Copyright 2021 Google LLC

## License
The code in this repository is licensed under the Apache License, Version 2.0. See the [LICENSE](wiki/license) section for more info.
