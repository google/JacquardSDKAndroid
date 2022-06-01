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
package com.google.android.jacquard.sample.imu

import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.google.android.jacquard.sdk.imu.ImuModule
import com.google.android.jacquard.sdk.imu.parser.ImuSessionData
import com.google.android.jacquard.sdk.rx.Signal

/** ViewModel class for [ImuSamplesListFragment]. */
class ImuSamplesListViewModel(private val navController: NavController) : ViewModel() {

  /** Parses raw imu samples file. */
  fun parse(path: String): Signal<ImuSessionData> {
    return ImuModule.parseImuData(path)
  }

  /** Handles the back arrow click. */
  fun backKeyPressed() {
    navController.popBackStack()
  }
}