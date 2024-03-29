/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.jacquard.sample

import android.app.Application
import android.text.TextUtils
import com.google.android.jacquard.sdk.log.PrintLogger
import com.google.android.jacquard.sdk.model.SdkConfig

class SampleApplication : Application() {
    lateinit var resourceLocator: ResourceLocator

    override fun onCreate() {
        super.onCreate()
        resourceLocator = ResourceLocator(this)
        if (TextUtils.isEmpty(BuildConfig.API_KEY)) {
            PrintLogger.w(TAG, "******************************")
            PrintLogger.w(TAG, "* Invalid API Key.")
            PrintLogger.w(TAG, "* Cloud functions will not work (eg. firmware updating)")
            PrintLogger.w(TAG, "* Information on obtaining an API key is at")
            PrintLogger.w(
                TAG,
                "* https://google.github.io/JacquardSDKAndroid/wiki/cloud-api-terms/"
            )
            PrintLogger.w(TAG, "******************************")
        }
        resourceLocator
            .connectivityManager
            .init(SdkConfig.of(packageName, BuildConfig.API_KEY, BuildConfig.CLOUD_ENDPOINT))
        PrintLogger.d(
            TAG, "App created # "
                    + getString(
                R.string.app_version,
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE
            )
        )
        resourceLocator.preferences.knownTags.forEach {
            PrintLogger.d(TAG,  /* message= */"onBleEnabled # Connect to # " + it.address())
            resourceLocator.connectivityManager.connect(it.address())
        }
    }

    override fun onTerminate() {
        resourceLocator.connectivityManager.destroy()
        super.onTerminate()
    }

    companion object {
        private val TAG = SampleApplication::class.java.simpleName
    }
}