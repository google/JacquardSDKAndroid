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
package com.google.android.jacquard.sample.imu.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.jacquard.sdk.imu.Sensors
import com.google.android.jacquard.sdk.imu.model.ImuSessionInfo

@Entity(tableName = "imu_sessions")
data class JqSessionInfo(
    @PrimaryKey val imuSessionId: String,
    val dcSessionId: String,
    val campaignId: String,
    val subjectId: String,
    val sensor: Int = 0,
    val productId: String,
    val imuSize: Int = 0,
    val tagSerialNumber: String,
    val path: String?
) {

    companion object {
        @JvmStatic
        fun map(info: ImuSessionInfo, tagSerialNumber: String = ""): JqSessionInfo {
            return JqSessionInfo(
                info.imuSessionId(),
                info.dcSessionId(),
                info.campaignId(),
                info.subjectId(),
                info.sensor().id(),
                info.productId(),
                info.imuSize(),
                tagSerialNumber, path = null
            )
        }

        @JvmStatic
        fun map(info: JqSessionInfo): ImuSessionInfo {
            return ImuSessionInfo.builder()
                .productId(info.productId)
                .subjectId(info.subjectId)
                .campaignId(info.campaignId)
                .imuSessionId(info.imuSessionId)
                .dcSessionId(info.dcSessionId)
                .imuSize(info.imuSize)
                .sensor(Sensors.forId(info.sensor))
                .build()
        }
    }
}
