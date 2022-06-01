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

import androidx.core.util.Pair
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.google.android.jacquard.sample.ConnectivityManager
import com.google.android.jacquard.sample.Preferences
import com.google.android.jacquard.sample.imu.db.JqSessionInfo
import com.google.android.jacquard.sample.storage.ImuSessionsRepository
import com.google.android.jacquard.sdk.imu.ImuModule
import com.google.android.jacquard.sdk.imu.ImuModule.SESSION_FILE_EXTENSION
import com.google.android.jacquard.sdk.imu.model.ImuSessionInfo
import com.google.android.jacquard.sdk.lm.InitState
import com.google.android.jacquard.sdk.log.PrintLogger
import com.google.android.jacquard.sdk.rx.Signal
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag
import com.google.atap.jacquard.protocol.JacquardProtocol
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionStatus
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/** ViewModel class for [IMUFragment] */
class ImuViewModel(
  private val connectivityManager: ConnectivityManager,
  private val navController: NavController,
  private val repository: ImuSessionsRepository,
  private val preferences: Preferences,
  downloadDirectoryPath: String
) : ViewModel() {

  companion object {
    private val TAG = ImuViewModel::class.java.simpleName
  }

  private lateinit var connectedJacquardTag: Signal<ConnectedJacquardTag>
  private lateinit var imuModule: ImuModule
  private val downloadDirectory: File = File(downloadDirectoryPath)
  private lateinit var tagSerialNumber: String

  /** Initialize [ImuModule] */
  fun init(): Signal<InitState> {

    preferences.currentTag?.let { tag ->
      connectedJacquardTag =
        connectivityManager.getConnectedJacquardTag(tag.address())
      val inner = AtomicReference<Signal.Subscription>()
      return Signal.create { signal ->

        val outer: Signal.Subscription = connectedJacquardTag.first().onNext { tag ->
          tagSerialNumber = tag.serialNumber()
          imuModule = ImuModule(tag)
          inner.set(imuModule.initialize().forward(signal))
        }

        return@create object : Signal.Subscription() {
          override fun onUnsubscribe() {
            PrintLogger.d(TAG, "init # onUnsubscribe")
            inner.get()?.unsubscribe()
            outer.unsubscribe()
          }
        }
      }
    } ?: throw Exception("No current tag exists")
  }

  fun getCurrentSessionId(): Signal<String> = imuModule.currentSessionId

  /** Get current data collection status. */
  fun getDataCollectionStatus(): Signal<DataCollectionStatus> =
    imuModule.dataCollectionStatus

  fun getDataCollectionMode(): Signal<JacquardProtocol.DataCollectionMode> =
    imuModule.currentDataCollectionMode

  /** Starts imu session. */
  fun startSession(): Signal<String> = imuModule.startImuSession()

  /** Stops imu session. */
  fun stopSession(): Signal<Boolean> {
    return imuModule.stopImuSession().flatMap { isStopped ->
      if (isStopped) getUjtSessionList().map { isStopped } else Signal.from(isStopped)
    }
  }

  /** Stops imu data collection. */
  fun stopDataCollectionSession(): Signal<Boolean> {
    return imuModule.stopImuSession()
  }

  /** Fetch imu session list from ujt. */
  fun getUjtSessionList(): Signal<Int> {
    PrintLogger.d(TAG, "Fetch sessions list from ujt # ")
    return imuModule.imuSessionsList.map { sessions ->
      PrintLogger.d(TAG, "Sessions found on ujt # ${sessions.size}")

      for (info in sessions) {
        PrintLogger.d(TAG, "Ujt session # $info")
        repository.insert(JqSessionInfo.map(info, tagSerialNumber))
      }
      return@map sessions.size
    }
  }

  /** Returns download directory for imu sessions. */
  fun getDownloadDirectory() =
    File("${downloadDirectory.absolutePath}/${tagSerialNumber}/")

  /** Gives observables to see updates in sessions db. */
  fun getSessionsList(): LiveData<List<JqSessionInfo>> {
    return repository.getImuSessions()
  }

  /** Download imu session data from ujt. */
  fun downloadSession(sessionInfo: ImuSessionInfo): Signal<Pair<Int, File>> {
    return imuModule.downloadImuData(sessionInfo)
      .tap { progress ->
        if (progress.first == 100) {
          // Give some extra time to dc lm to finish current task.
          Signal.from(1)
            .delay(1000)
            .onNext {
              imuModule.erase(sessionInfo)
                .onNext { erased ->
                  PrintLogger.d(
                    TAG,
                    String.format(
                      "Erased Session ${sessionInfo.imuSessionId()} after download ? $erased"
                    )
                  )
                }
            }
        }
      }
  }

  /** Erase session either from local or ujt. */
  fun erase(session: JqSessionInfo): Signal<Boolean> {
    val imuSession = File(getDownloadDirectory(), session.imuSessionId + SESSION_FILE_EXTENSION)
    val isDownloaded = imuSession.exists() && imuSession.length() == session.imuSize.toLong()
    PrintLogger.d(
      TAG,
      "Erasing session # ${session.imuSessionId}. isDownloaded ? $isDownloaded"
    )

    if (isDownloaded) {
      return Signal.from(imuSession.delete())
        .map { deleted ->
          repository.delete(session) // Delete from db
          return@map deleted
        }
    }
    return imuModule.erase(JqSessionInfo.map(session)).map { erased ->
      if (erased) {
        repository.delete(session)
        imuSession.delete()
      }
      return@map erased
    }
  }

  /** Erase all imu sessions downloaded as well as all sessions from ujt. */
  fun eraseAll(): Signal<Boolean> {
    PrintLogger.d(TAG, "Erasing all ## $tagSerialNumber")
    return imuModule.eraseAll().map { erased ->
      if (erased) {
        repository.deleteAll(tagSerialNumber)
        val downloaded: Array<File> = getDownloadDirectory().listFiles() ?: return@map erased
        for (session in downloaded) {
          session.delete()
        }
      }
      return@map erased
    }
  }

  fun destroy() {
    PrintLogger.d(TAG, "destroy # ")
    imuModule.dataCollectionStatus.flatMap { status ->
      PrintLogger.d(TAG, "DC Status # $status")
      if (status != DataCollectionStatus.DATA_COLLECTION_LOGGING) {
        return@flatMap imuModule.unloadModule()
      }
      return@flatMap Signal.from(true)
    }.onNext { imuModule.destroy() }
  }

  fun getTagSerialNumber(): String = tagSerialNumber

  /** Handles the back arrow click in the toolbar. */
  fun backKeyPressed() = navController.popBackStack()

  /** Trigger action to navigate to [ImuSamplesListFragment] */
  fun viewImuSamples(path: String) {
    navController.navigate(IMUFragmentDirections.actionImuFragmentToImusamplesListFragment(path))
  }
}