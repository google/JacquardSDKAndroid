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

package com.google.android.jacquard.sample.splash

import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import com.google.android.jacquard.sample.Preferences

/** View model for the [SplashFragment]. */
class SplashViewModel(
    private val preferences: Preferences,
    private val navController: NavController
) : ViewModel() {

    fun navigateToNextScreen() {
        val navDirections: NavDirections = if (preferences.knownTags.isEmpty()) {
            SplashFragmentDirections.actionToScanFragment()
        } else {
            SplashFragmentDirections.actionToHomeFragment()
        }
        navController.navigate(navDirections)
    }

}