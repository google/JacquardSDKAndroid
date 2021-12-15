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
 */

package com.google.android.jacquard.sample.imu;

import androidx.lifecycle.ViewModel;
import androidx.navigation.NavController;
import com.google.android.jacquard.sample.ConnectivityManager;
import com.google.android.jacquard.sample.Preferences;
import com.google.android.jacquard.sdk.imu.ImuModule;
import com.google.android.jacquard.sdk.imu.InitState;
import com.google.android.jacquard.sdk.imu.model.ImuStream;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.rx.Executors;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionMode;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionStatus;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Viewmodel for {@link ImuStreamingFragment}.
 */
public class ImuStreamingViewModel extends ViewModel {

  private static final String TAG = ImuViewModel.class.getSimpleName();
  private final ConnectivityManager connectivityManager;
  private final NavController navController;
  private final Preferences preferences;
  private Signal<ConnectedJacquardTag> connectedJacquardTag;
  private ImuModule imuModule = null;

  public ImuStreamingViewModel(
      ConnectivityManager connectivityManager, NavController navController,
      Preferences preferences) {
    this.connectivityManager = connectivityManager;
    this.navController = navController;
    this.preferences = preferences;
  }

  /**
   * Initialize {@link ImuModule}
   */
  public Signal<InitState> init() {
    this.connectedJacquardTag = connectivityManager
        .getConnectedJacquardTag(preferences.getCurrentTag().address());
    final AtomicReference<Subscription> inner = new AtomicReference<>();
    return Signal.<InitState>create(signal -> {
      Subscription outer = connectedJacquardTag.first().onNext(tag -> {
        imuModule = new ImuModule(tag);
        inner.set(imuModule.initialize().forward(signal));
      });
      return new Subscription() {
        @Override
        protected void onUnsubscribe() {
          PrintLogger.d(TAG, "init # onUnsubscribe");
          if (inner.get() != null) {
            inner.get().unsubscribe();
          }
          if (outer != null) {
            outer.unsubscribe();
          }
        }
      };
    }).observeOn(Executors.mainThreadExecutor());
  }

  /**
   * Get current data collection status.
   */
  public Signal<DataCollectionStatus> getDataCollectionStatus() {
    return imuModule.getDataCollectionStatus();
  }

  public Signal<Boolean> isImuStreamingInProgress() {
    return getDataCollectionStatus().flatMap(status -> {
      if (!DataCollectionStatus.DATA_COLLECTION_LOGGING.equals(status)) {
        return Signal.from(false);
      } else {
        return imuModule.getCurrentDataCollectionMode()
            .map(mode -> DataCollectionMode.DATA_COLLECTION_MODE_STREAMING.equals(mode));
      }
    });
  }

  public Signal<DataCollectionMode> getCurrentDCMode() {
    return imuModule.getCurrentDataCollectionMode();
  }

  /**
   * Starts imu streaming.
   */
  public Signal<ImuStream> startStreaming() {
    return imuModule.startImuStreaming();
  }

  /**
   * Stops imu session.
   */
  public Signal<Boolean> stopStreaming() {
    return imuModule.stopImuStreaming();
  }

  /**
   * Destroys view model.
   */
  public void destroy() {
    PrintLogger.d(TAG, "destroy # ");
    imuModule.getDataCollectionStatus().flatMap(status -> {
      PrintLogger.d(TAG, "DC Status #" + status);
      if (!status.equals(DataCollectionStatus.DATA_COLLECTION_LOGGING)) {
        return imuModule.unloadModule();
      }
      return Signal.from(true);
    }).onNext(ignore -> imuModule.destroy());
  }

  /**
   * Handles the back arrow click in the toolbar.
   */
  public void backKeyPressed() {
    navController.popBackStack();
  }
}