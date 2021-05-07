/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.jacquard.sdk.model;

/** Holds supported MTU size Jacquard protocol versions. */
public enum ProtocolSpec {
  UNKNOWN(0, 23), VERSION_1(1, 23), VERSION_2(2, 64);

  private final int protocolId;
  private final int mtuSize;

  ProtocolSpec(int protocolId, int mtuSize) {
    this.protocolId = protocolId;
    this.mtuSize = mtuSize;
  }

  /** Returns the protocol Id */
  public int getProtocolId() {
    return protocolId;
  }

  /** Returns the supports MTU size. */
  public int getMtuSize() {
    return mtuSize;
  }
}
