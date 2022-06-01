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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.NewInstanceFactory
import androidx.navigation.NavController
import com.google.android.jacquard.sample.firmwareupdate.FirmwareUpdateViewModel
import com.google.android.jacquard.sample.gesture.GestureViewModel
import com.google.android.jacquard.sample.haptics.HapticsViewModel
import com.google.android.jacquard.sample.home.HomeViewModel
import com.google.android.jacquard.sample.imu.ImuSamplesListViewModel
import com.google.android.jacquard.sample.imu.ImuStreamingViewModel
import com.google.android.jacquard.sample.imu.ImuViewModel
import com.google.android.jacquard.sample.ledpattern.LedPatternViewModel
import com.google.android.jacquard.sample.musicalthreads.MusicalThreadsViewModel
import com.google.android.jacquard.sample.places.PlacesConfigViewModel
import com.google.android.jacquard.sample.places.PlacesDetailsViewModel
import com.google.android.jacquard.sample.places.PlacesListViewModel
import com.google.android.jacquard.sample.renametag.RenameTagViewModel
import com.google.android.jacquard.sample.scan.ScanViewModel
import com.google.android.jacquard.sample.splash.SplashViewModel
import com.google.android.jacquard.sample.storage.ImuSessionsRepository
import com.google.android.jacquard.sample.storage.PlacesRepository
import com.google.android.jacquard.sample.tagmanager.TagDetailsViewModel
import com.google.android.jacquard.sample.tagmanager.TagManagerViewModel
import com.google.android.jacquard.sample.touchdata.TouchDataViewModel
import com.google.android.jacquard.sdk.util.StringUtils

/** Factory for creating view models and provide dependencies.  */
class ViewModelFactory(
    private val application: Application,
    private val navController: NavController
) : NewInstanceFactory() {

    private val resourceLocator: ResourceLocator =
        (application as SampleApplication).resourceLocator

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val connectivityManager = resourceLocator.connectivityManager
        val preferences = resourceLocator.preferences
        val resources = resourceLocator.resources
        when (modelClass) {
            ScanViewModel::class.java -> {
                return ScanViewModel(
                    connectivityManager,
                    preferences,
                    navController,
                    resources
                ) as T
            }
            TouchDataViewModel::class.java -> {
                return TouchDataViewModel(
                    connectivityManager, navController, preferences, resourceLocator.resources
                ) as T
            }
            MusicalThreadsViewModel::class.java -> {
                val threadsPlayer = resourceLocator.threadsPlayer
                return MusicalThreadsViewModel(
                    connectivityManager,
                    threadsPlayer,
                    navController,
                    preferences,
                    resourceLocator.resources
                ) as T
            }
            GestureViewModel::class.java -> {
                return GestureViewModel(connectivityManager, navController, preferences) as T
            }
            MainActivityViewModel::class.java -> {
                return MainActivityViewModel(
                    preferences,
                    resourceLocator.firmwareManager,
                    navController
                ) as T
            }
            HapticsViewModel::class.java -> {
                return HapticsViewModel(
                    connectivityManager, navController, resourceLocator.resources, preferences
                ) as T
            }
            LedPatternViewModel::class.java -> {
                return LedPatternViewModel(
                    connectivityManager, navController, StringUtils.getInstance(), preferences
                ) as T
            }
            HomeViewModel::class.java -> {
                return HomeViewModel(
                    preferences,
                    navController,
                    connectivityManager,
                    resourceLocator.recipeManager,
                    resourceLocator.firmwareManager,
                    resourceLocator.resources
                ) as T
            }
            SplashViewModel::class.java -> {
                return SplashViewModel(preferences, navController) as T
            }
            TagManagerViewModel::class.java -> {
                return TagManagerViewModel(connectivityManager, preferences, navController) as T
            }
            RenameTagViewModel::class.java -> {
                return RenameTagViewModel(preferences, navController, connectivityManager) as T
            }
            TagDetailsViewModel::class.java -> {
                return TagDetailsViewModel(connectivityManager, preferences, navController) as T
            }
            FirmwareUpdateViewModel::class.java -> {
                return FirmwareUpdateViewModel(
                    resourceLocator.firmwareManager,
                    navController,
                    connectivityManager,
                    preferences
                ) as T
            }
            PlacesConfigViewModel::class.java -> {
                return PlacesConfigViewModel(navController, resourceLocator.recipeManager) as T
            }
            PlacesListViewModel::class.java -> {
                return PlacesListViewModel(
                    connectivityManager,
                    navController,
                    resourceLocator.geocoder,
                    PlacesRepository(application),
                    preferences
                ) as T
            }
            ImuViewModel::class.java -> {
                return ImuViewModel(
                    connectivityManager,
                    navController,
                    ImuSessionsRepository(application),
                    preferences,
                    resourceLocator.imuSessionDownloadDirectory
                ) as T
            }
            ImuStreamingViewModel::class.java -> {
                return ImuStreamingViewModel(connectivityManager, navController, preferences) as T
            }
            ImuSamplesListViewModel::class.java -> {
                return ImuSamplesListViewModel(navController) as T
            }
            else -> return if (modelClass == PlacesDetailsViewModel::class.java) {
                PlacesDetailsViewModel(navController, PlacesRepository(application)) as T
            } else super.create(modelClass)
        }
    }
}