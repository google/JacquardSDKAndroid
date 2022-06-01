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

package com.google.android.jacquard.sample.firmwareupdate

import com.google.android.jacquard.sample.ConnectivityManager
import com.google.android.jacquard.sdk.connection.ConnectionState
import com.google.android.jacquard.sdk.dfu.DFUInfo
import com.google.android.jacquard.sdk.dfu.FirmwareUpdateState
import com.google.android.jacquard.sdk.log.PrintLogger
import com.google.android.jacquard.sdk.rx.Signal
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag

/** Manager for providing access to the DFU updates. */
class FirmwareManager(private val connectivityManager: ConnectivityManager) {
    companion object {
        private val TAG = FirmwareManager::class.java.simpleName
    }

    private var mapLastState: MutableMap<String, FirmwareUpdateState> = mutableMapOf()
    private lateinit var dfuInfoList: MutableList<DFUInfo>
    private val moduleInfoList: MutableList<DFUInfo> = mutableListOf()

    fun checkFirmware(address: String, forceUpdate: Boolean): Signal<List<DFUInfo>> {
        dfuInfoList = mutableListOf();
        return getConnectedJacquardTag(address)
            .flatMap { tag ->
                tag.dfuManager().checkFirmware(tag.components, forceUpdate)
            }
            .tap { dfuInfos ->
                PrintLogger.d(TAG, "checkFirmware result : $dfuInfos")
                dfuInfoList.addAll(dfuInfos)
            }.tapError { error ->
                PrintLogger.d(TAG, "checkFirmware error : ${error.message}")
            }
    }

    fun checkModuleUpdate(address: String, forceUpdate: Boolean): Signal<List<DFUInfo>> {
        moduleInfoList.clear()
        return getConnectedJacquardTag(address)
            .flatMap { tag ->
                tag.dfuManager().checkModuleUpdate(forceUpdate)
                    .tap { dfuInfoList ->
                        PrintLogger.d(TAG, "checkModule result : $dfuInfoList")
                        moduleInfoList.addAll(dfuInfoList)
                    }
                    .tapError { error ->
                        PrintLogger.d(TAG, "module update error : ${error.message}")
                    }
            }
    }

    /** Apply the firmware updated of provided DfuManager.  */
    fun applyFirmware(
        address: String,
        autoExecute: Boolean
    ): Signal<FirmwareUpdateState> {
        return getConnectedJacquardTag(address)
            .flatMap { tag ->
                dfuInfoList.addAll(moduleInfoList)
                tag.dfuManager().applyUpdates(dfuInfoList, autoExecute)
            }
            .distinctUntilChanged()
            .tap { status ->
                PrintLogger.d(TAG, "applyFirmware response = $status")
            }
            .tapError { error ->
                PrintLogger.d(TAG, "applyFirmware error = " + error.message)
            }
    }

    /** Execute the firmware updated of provided DfuManager. */
    fun executeFirmware(address: String): Signal<FirmwareUpdateState> {
        return getConnectedJacquardTag(address)
            .flatMap { tag ->
                tag.dfuManager().executeUpdates()
                tag.dfuManager().currentState
            }
    }

    /** Returns current state of tag for provided address. */
    fun getState(address: String): Signal<FirmwareUpdateState> {
        return getConnectedJacquardTag(address).flatMap { tag ->
            tag.dfuManager().currentState.tap { state ->
                if (state.type != FirmwareUpdateState.Type.IDLE) {
                    mapLastState[address] = state
                }
            }
        }
    }

    fun popLastKnownState(address: String): FirmwareUpdateState? {
        val state = mapLastState.remove(address)
        return state
    }

    private fun getConnectedJacquardTag(address: String): Signal<ConnectedJacquardTag> {
        return connectivityManager.getConnectedTag(address)
    }
}