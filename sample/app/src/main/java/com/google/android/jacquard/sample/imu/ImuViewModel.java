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

import static com.google.android.jacquard.sdk.imu.ImuModule.SESSION_FILE_EXTENSION;

import android.util.Pair;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.navigation.NavController;
import com.google.android.jacquard.sample.ConnectivityManager;
import com.google.android.jacquard.sample.Preferences;
import com.google.android.jacquard.sample.imu.db.JqSessionInfo;
import com.google.android.jacquard.sample.storage.ImuSessionsRepository;
import com.google.android.jacquard.sdk.imu.ImuModule;
import com.google.android.jacquard.sdk.imu.InitState;
import com.google.android.jacquard.sdk.imu.model.ImuSessionInfo;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.rx.Executors;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionStatus;
import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ViewModel class for {@link IMUFragment}
 */
public class ImuViewModel extends ViewModel {

  private static final String TAG = ImuViewModel.class.getSimpleName();
  private final Signal<ConnectedJacquardTag> connectedJacquardTag;
  private final NavController navController;
  private final ImuSessionsRepository repository;
  private final Preferences preferences;
  private File downloadDirectory;
  private ImuModule imuModule = null;

  public ImuViewModel(
      ConnectivityManager connectivityManager, NavController navController,
      String downloadDirectory,
      ImuSessionsRepository repository, Preferences preferences) {
    this.connectedJacquardTag = connectivityManager.getConnectedJacquardTag();
    this.navController = navController;
    this.downloadDirectory = new File(downloadDirectory);
    this.repository = repository;
    this.preferences = preferences;
  }

  /**
   * Initialize {@link ImuModule}
   */
  public Signal<InitState> init() {
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

  public String getCurrentSessionId() {
    return preferences.getCurrentImuSessionId(imuModule.tagSerialNumber());
  }

  /**
   * Get current data collection status.
   */
  public Signal<DataCollectionStatus> getDataCollectionStatus() {
    return imuModule.getDataCollectionStatus();
  }

  /**
   * Starts imu session.
   */
  public Signal<String> startSession() {
    return imuModule.startImuSession()
        .tap(id -> preferences.setCurrentImuSession(id, imuModule.tagSerialNumber()));
  }

  /**
   * Stops imu session.
   */
  public Signal<Boolean> stopSession() {
    return imuModule.stopImuSession().flatMap(stopped -> {
      if (stopped) {
        // reset current imu session id
        preferences.setCurrentImuSession("0", imuModule.tagSerialNumber());
        return getUjtSessionList().map(ignore -> stopped);
      }
      return Signal.from(stopped);
    });
  }

  /**
   * Returns download directory for imu sessions.
   */
  public File getDownloadDirectory() {
    return new File(downloadDirectory.getAbsolutePath() + "/" + imuModule.tagSerialNumber() + "/");
  }

  /**
   * Gives observables to see updates in sessions db.
   */
  public LiveData<List<JqSessionInfo>> getSessionsList() {
    return repository.getImuSessions();
  }

  /**
   * Fetch imu session list from ujt.
   */
  public Signal<Integer> getUjtSessionList() {
    PrintLogger.d(TAG, "Fetch sessions list from ujt # ");
    return imuModule.getImuSessionsList().map(sessions -> {
      PrintLogger.d(TAG, "Sessions found on ujt # " + sessions.size());
      for (ImuSessionInfo info : sessions) {
        PrintLogger.d(TAG, "Ujt session # " + info);
        repository.insert(JqSessionInfo.map(info, imuModule.tagSerialNumber()));
      }
      return sessions.size();
    });
  }

  /**
   * Download imu session data from ujt.
   */
  public Signal<Pair<Integer, File>> downloadSession(ImuSessionInfo sessionInfo) {
    return imuModule.downloadImuData(sessionInfo)
        .tap(progress -> {
          if (progress.first == 100) {
            // Give some extra time to dc lm to finish current task.
            Signal.from(1).delay(1000)
                .onNext(ignore -> imuModule.erase(sessionInfo).onNext(erased -> PrintLogger
                    .d(TAG, String
                        .format("Erased Session %s after download ? %s", sessionInfo.imuSessionId(),
                            erased))));
          }
        });
  }

  /**
   * Erase session either from local or ujt.
   */
  public Signal<Boolean> erase(JqSessionInfo session) {
    File imuSession = new File(getDownloadDirectory(),
        session.imuSessionId + SESSION_FILE_EXTENSION);
    boolean isDownloaded = imuSession.exists() && imuSession.length() == session.imuSize;
    PrintLogger.d(TAG, String
        .format("Erasing session # %s. isDownloaded ? %s", session.imuSessionId, isDownloaded));
    if (isDownloaded) {
      return Signal.from(imuSession.delete()).map(deleted -> {
        repository.delete(session); // Delete from db
        return deleted;
      });
    }
    return imuModule.erase(JqSessionInfo.map(session)).map(erased -> {
      if (erased) {
        repository.delete(session);
        imuSession.delete();
      }
      return erased;
    });
  }

  /**
   * Erase all imu sessions downloaded as well as all sessions from ujt.
   */
  public Signal<Boolean> eraseAll() {
    PrintLogger.d(TAG, "Erasing all ## " + imuModule.tagSerialNumber());
    return imuModule.eraseAll().map(erased -> {
      if (erased) {
        repository.deleteAll(imuModule.tagSerialNumber());
        File[] downloaded = getDownloadDirectory().listFiles();
        if (downloaded == null) {
          return erased;
        }
        for (File session : downloaded) {
          session.delete();
        }
      }
      return erased;
    });
  }

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

  public String getTagSerialNumber() {
    return imuModule.tagSerialNumber();
  }

  /**
   * Handles the back arrow click in the toolbar.
   */
  public void backKeyPressed() {
    navController.popBackStack();
  }
}
