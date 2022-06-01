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
package com.google.android.jacquard.sdk.util;

import java.util.List;

/**
 * Fake implementation of {@link Fragmenter}.
 */
public final class FakeFragmenter extends Fragmenter {

  private boolean isDecodeFragmentNull;
  private boolean throwsException;

  public FakeFragmenter(String name, int mtuSize) {
    super(name, mtuSize);
  }

  @Override
  public List<byte[]> fragmentData(byte[] message) {
    if (throwsException) {
      throw new NullPointerException();
    }
    return super.fragmentData(message);
  }

  @Override
  public byte[] decodeFragment(byte[] fragment) {
    if (isDecodeFragmentNull) {
      return null;
    }
    return super.decodeFragment(fragment);
  }

  /**
   * Sets {@code isDataNull} true to fail decode fragment operation.
   *
   * @param isDataNull expects decode fragment returns null if true
   */
  public void setDecodeFragmentNull(boolean isDataNull) {
    this.isDecodeFragmentNull = isDataNull;
  }

  /**
   * Sets {@code throwsException} true to fail fragment data operation.
   *
   * @param throwsException expects decode fragment returns null if true
   */
  public void setThrowsException(boolean throwsException) {
    this.throwsException = throwsException;
  }

  private byte[] getNotificationDecodeFragmentPacket() {
    return new byte[]{8, 0, 16, 29, 24, 0};
  }
}
