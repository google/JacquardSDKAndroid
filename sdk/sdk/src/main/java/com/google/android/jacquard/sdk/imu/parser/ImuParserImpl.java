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

package com.google.android.jacquard.sdk.imu.parser;

import androidx.annotation.NonNull;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionActionHeader;
import com.google.atap.jacquard.protocol.JacquardProtocol.ImuSample;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;

/**
 * Default implementation class for {@link ImuParser}
 */
public class ImuParserImpl implements ImuParser {

  //byte size for fields composing
  public static final int IMU_SAMPLE_LENGTH = 16;

  //byte sizes for fields composing ActionHeader data
  private static final int ACTION_ID_LENGTH = 4;
  private static final int ACTION_RESULT_LENGTH = 2;

  static final int ACTION_HEADER_LENGTH = ACTION_ID_LENGTH + ACTION_RESULT_LENGTH;
  public static final int ACTION_HEADER_LENGTH_PARTIAL = ACTION_ID_LENGTH;

  @Override
  public ImuSample parseImuSample(@NonNull byte[] bytes) {
    validateBytes(bytes, "ImuSample");
    ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    short ax = byteBuffer.getShort();
    short ay = byteBuffer.getShort();
    short az = byteBuffer.getShort();
    short gx = byteBuffer.getShort();
    short gy = byteBuffer.getShort();
    short gz = byteBuffer.getShort();
    int timestamp = byteBuffer.getInt();

    return ImuSample.newBuilder()
        .setAccX(ax)
        .setAccY(ay)
        .setAccZ(az)
        .setGyroRoll(gx)
        .setGyroPitch(gy)
        .setGyroYaw(gz)
        .setUjtSysTick(timestamp)
        .build();
  }

  @Override
  public DataCollectionActionHeader parseActionHeader(@NonNull byte[] bytes) {
    validateBytes(bytes, "actionHeader");
    ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
    validateLength(buffer, ACTION_HEADER_LENGTH_PARTIAL);
    int actionId = buffer.getInt();
    boolean isError = false;
    if (bytes.length == ACTION_HEADER_LENGTH) {
      isError = buffer.getShort() > 0;
    }
    DataCollectionActionHeader actionHeader = DataCollectionActionHeader.newBuilder()
        .setActionId(actionId)
        .setIsError(isError)
        .setNumSamples(0) //always 0
        .build();
    return actionHeader;
  }

  void validateLength(ByteBuffer buffer, int minLength) {
    if (buffer.limit() - buffer.position() < minLength) {
      throw new ImuParserException(
          "Buffer does not meet the required minimum length to parse.");
    }
  }

  void validateBytes(@NonNull byte[] bytes, String type) {
    if (bytes == null || bytes.length == 0) {
      throw new ImuParserException(String.format(Locale.US,
          "Cannot parse %s. bytes  cannot be null or empty.", type));
    }
  }
}
