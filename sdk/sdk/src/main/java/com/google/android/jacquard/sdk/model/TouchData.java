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

import com.google.android.jacquard.sdk.util.Lists.UnmodifiableListBuilder;
import com.google.atap.jacquard.protocol.JacquardProtocol;
import com.google.auto.value.AutoValue;
import com.google.protobuf.ByteString;
import java.util.List;

/** Data class holding raw data emitted when the tag is set on {@link TouchMode#CONTINUOUS}. */
@AutoValue
public abstract class TouchData {

  /**
   * Creates a new instance of TouchData from a {@link JacquardProtocol.TouchData}.
   * @param touchData the raw touch data received from the gear.
   * @return the decoded touch data
   */
  public static TouchData of(JacquardProtocol.TouchData touchData) {
    ByteString diffDataScaled = touchData.getDiffDataScaled();
    if (diffDataScaled.size() != 13) {
      // DiffDataScaled has 13 (8 bit)values. 1st represents the proximity and remaining 12
      // represent touch data for the threads.
      throw new IllegalStateException("Invalid TouchData");
    }

    byte[] diffData = diffDataScaled.toByteArray();
    byte[] lines = new byte[diffData.length - 1];
    System.arraycopy(diffData, 1, lines, 0, diffData.length - 1);

    int proximity = toUnsignedInt(diffData[0]);
    List<Integer> unsignedLines = toUnsignedInts(lines);
    return new AutoValue_TouchData(unsignedLines, touchData.getSequence(), proximity);
  }

  /**
   * The raw touch data encoded as a list of 12 unsigned integers.
   * One integer per lines but not all lines may be active on each product.
   * The values are in the range 0-127 depicting the intensity.
   */
  public abstract List<Integer> lines();

  /** Sequence byte. This can be used by to verify that every sample has been processed. */
  public abstract int sequence();

  /** Proximity Diff Data */
  public abstract int proximity();

  private static List<Integer> toUnsignedInts(byte[] lines) {
    UnmodifiableListBuilder<Integer> builder = new UnmodifiableListBuilder<>();
    for (byte line : lines) {
      builder.add(toUnsignedInt(line));
    }
    return builder.build();
  }

  private static int toUnsignedInt(byte x) {
    return ((int) x) & 0xff;
  }
}
