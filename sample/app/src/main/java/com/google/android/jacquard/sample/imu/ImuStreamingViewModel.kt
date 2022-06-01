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
import com.google.android.jacquard.sample.ConnectivityManager
import com.google.android.jacquard.sample.Preferences
import com.google.android.jacquard.sdk.imu.ImuModule
import com.google.android.jacquard.sdk.lm.InitState
import com.google.android.jacquard.sdk.imu.model.ImuStream
import com.google.android.jacquard.sdk.log.PrintLogger
import com.google.android.jacquard.sdk.rx.Executors
import com.google.android.jacquard.sdk.rx.Signal
import com.google.android.jacquard.sdk.rx.Signal.Subscription
import com.google.atap.jacquard.protocol.JacquardProtocol
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionMode
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionStatus
import java.util.concurrent.atomic.AtomicReference

/** Viewmodel for [ImuStreamingFragment]. */
class ImuStreamingViewModel(
  private val connectivityManager: ConnectivityManager,
  private val navController: NavController,
  private val preferences: Preferences
) : ViewModel() {

  companion object {
    private val TAG = ImuViewModel::class.java.simpleName
  }

  private lateinit var imuModule: ImuModule

  /** Initialize [ImuModule] */
  fun init(): Signal<InitState> {

    val tagAddress = preferences.currentTag!!.address()
    val inner = AtomicReference<Subscription>()

    return Signal.create<InitState> { signal ->
      val outer: Subscription =
        connectivityManager.getConnectedJacquardTag(tagAddress).first().onNext { tag ->
          imuModule = ImuModule(tag)
          inner.set(imuModule.initialize().forward(signal))
        }

      object : Subscription() {
        override fun onUnsubscribe() {
          PrintLogger.d(TAG, "init # onUnsubscribe")
          inner.get().unsubscribe()
          outer.unsubscribe()
        }
      }
    }.observeOn(Executors.mainThreadExecutor())
  }

  /** Get current data collection status. */
  fun getDataCollectionStatus(): Signal<JacquardProtocol.DataCollectionStatus> {
    return imuModule.dataCollectionStatus
  }

  fun isImuStreamingInProgress(): Signal<Boolean> {
    return getDataCollectionStatus().flatMap { status ->
      if (DataCollectionStatus.DATA_COLLECTION_LOGGING != status) {
        Signal.from(false)
      } else {
        imuModule.currentDataCollectionMode.map { mode -> DataCollectionMode.DATA_COLLECTION_MODE_STREAMING == mode }
      }
    }
  }

  fun getCurrentDCMode(): Signal<DataCollectionMode> {
    return imuModule.currentDataCollectionMode
  }

  /** Starts imu streaming. */
  fun startStreaming(): Signal<ImuStream> {
    return imuModule.startImuStreaming()
  }

  /** Stops imu session. */
  fun stopStreaming(): Signal<Boolean> {
    return imuModule.stopImuStreaming()
  }

  /** Destroys view model. */
  fun destroy() {
    PrintLogger.d(TAG, "destroy # ")
    imuModule.dataCollectionStatus
      .flatMap { status ->
        PrintLogger.d(TAG, "DC Status #" + status)
        if (DataCollectionStatus.DATA_COLLECTION_LOGGING != status) {
          imuModule.unloadModule()
        }
        Signal.from(true)
      }
      .onNext { _ -> imuModule.destroy() }
  }

  /** Handles the back arrow click in the toolbar. */
  fun backKeyPressed() {
    navController.popBackStack()
  }
}