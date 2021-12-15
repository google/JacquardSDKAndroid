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

package com.google.android.jacquard.sdk.imu.model;

import com.google.android.jacquard.sdk.imu.Sensors;
import com.google.atap.jacquard.protocol.JacquardProtocol.ImuConfiguration;
import com.google.atap.jacquard.protocol.JacquardProtocol.ImuSample;
import com.google.auto.value.AutoValue;

/**
 * Model class which holds {@link ImuSample} and metadata info.
 */
@AutoValue
public abstract class ImuStream {

  public abstract ImuSample imuSample();

  public abstract ImuConfiguration imuConfig();

  public abstract Sensors sensors();

  public static ImuStream of(ImuSample sample, ImuConfiguration config, Sensors sensor) {
    return new AutoValue_ImuStream(sample, config, sensor);
  }
}
