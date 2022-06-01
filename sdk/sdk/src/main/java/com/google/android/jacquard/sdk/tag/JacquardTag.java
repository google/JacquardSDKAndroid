/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.jacquard.sdk.tag;

import androidx.annotation.Nullable;
import com.google.android.jacquard.sdk.rx.Signal;
import java.io.Serializable;

/** Based interface for Jacquard tags. */
public interface JacquardTag extends Serializable {

  /** The bluetooth address of the tag.  */
  String address();

  /** The name of the tag to display in the UI.  */
  String displayName();

  /**
   * Returns the rssi signal strength for the tag.<br/>During ble scanning as and when available
   * will be notified. When tag get connected, rssi value will be notified every second.
   */
  @Nullable
  Signal<Integer> rssiSignal();
}
