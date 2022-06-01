/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.google.android.jacquard.sample

import android.content.Context
import android.content.res.Resources
import android.location.Geocoder
import android.media.AudioAttributes
import android.media.SoundPool
import com.google.android.jacquard.sample.firmwareupdate.FirmwareManager
import com.google.android.jacquard.sample.musicalthreads.audio.Fader
import com.google.android.jacquard.sample.musicalthreads.audio.SoundPlayer
import com.google.android.jacquard.sample.musicalthreads.audio.SoundPoolPlayer
import com.google.android.jacquard.sample.musicalthreads.player.PluckThreadsPlayerImpl
import com.google.android.jacquard.sample.utilities.RecipeManager.Companion.getInstance
import com.google.android.jacquard.sdk.JacquardManager
import com.google.android.jacquard.sdk.log.LogLevel
import com.google.android.jacquard.sdk.log.PrintLogger
import com.google.android.jacquard.sdk.util.JQUtils
import com.google.common.collect.ImmutableList
import com.google.gson.GsonBuilder
import java.io.File
import java.util.*

/**
 * Very simple implementation of the Service Locator IoC pattern - central registry for obtaining
 * various "services" used by the app.
 */
class ResourceLocator(private val context: Context) {
    init {
        configurePrintLogger()
    }

    companion object {
        private val TAG = ResourceLocator::class.java.simpleName
    }

    val preferences by lazy {
        Preferences(
            context,
            GsonBuilder().create()
        )
    }
    val connectivityManager by lazy { ConnectivityManager(JacquardManager.getInstance()) }
    val firmwareManager by lazy { FirmwareManager(connectivityManager) }
    val geocoder by lazy { Geocoder(context, Locale.getDefault()) }
    val resources: Resources by lazy { context.resources }
    val threadsPlayer by lazy {
        PluckThreadsPlayerImpl(context)
    }
    val recipeManager by lazy { getInstance(context) }
    val imuSessionDownloadDirectory: String by lazy {
        val directory = File(context.cacheDir, "Sessions/")
        directory.mkdirs()
        directory.absolutePath
    }

    /** Configures [PrintLogger] with log levels [LogLevel].  */
    private fun configurePrintLogger() {
        val logLevels: ImmutableList<LogLevel> =
            if (JQUtils.isValidUrl(BuildConfig.CLOUD_ENDPOINT)) {
                // Non-Production
                ImmutableList.of(
                    LogLevel.DEBUG,
                    LogLevel.INFO,
                    LogLevel.WARNING,
                    LogLevel.ERROR,
                    LogLevel.ASSERT
                )
            } else {
                // Production. Default
                ImmutableList.of(LogLevel.INFO, LogLevel.WARNING, LogLevel.ERROR, LogLevel.ASSERT)
            }
        PrintLogger.initialize(logLevels, context)
        // Ignoring spam logs for rssi.
        PrintLogger.ignore("rssi")
        PrintLogger.i(TAG, "PrintLogger is configured with $logLevels")
    }
}