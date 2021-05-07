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
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionActionHeader;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionMetadata;
import com.google.atap.jacquard.protocol.JacquardProtocol.ImuConfiguration;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Factory class to parse Raw IMU Samples.
 */
public class JQImuParser {

  private static final short START_DELIMITER = (short) 0xfeca;
  private static final short END_DELIMITER = (short) 0xadde;
  private static final int START_DELIMITER_REPETITIONS = 6;
  private static final int END_DELIMITER_REPETITIONS = 7;
  private static final String TAG = JQImuParser.class.getSimpleName();
  private ImuParser imuParser;

  public JQImuParser(@NonNull ImuParser imuParser) {
    this.imuParser = imuParser;
  }

  /**
   * Starts parsing raw imu sample binary file.
   *
   * @param path Absolute path to the imu sample file. Make sure that your app has all necessary
   *             permissions to access this file.
   * @return {@link ImuSessionData}
   * @throws IOException                  If there is issue while reading raw imu sample file.
   * @throws ImuParserException If raw imu sample file is not properly formatted.
   */
  public ImuSessionData parseImuData(@NonNull String path)
      throws IOException, ImuParserException {
    PrintLogger.d(TAG, "Parsing raw IMU Sample file # " + path);
    return parseImuData(new BufferedInputStream(new FileInputStream(new File(path))));
  }

  private ImuSessionData parseImuData(BufferedInputStream inputStream) throws IOException {
    ImuSessionData imuSessionData = new ImuSessionData();

    // Parse DataCollectionMetadata
    DataCollectionMetadata dataCollectionMetadata = DataCollectionMetadata
        .parseDelimitedFrom(inputStream);
    imuSessionData.setMetadata(dataCollectionMetadata);
    // Parse ImuConfiguration
    ImuConfiguration imuConfiguration = ImuConfiguration.parseDelimitedFrom(inputStream);
    imuSessionData.setImuConfig(imuConfiguration);
    // Parse Imu Samples
    parseImuSamples(imuSessionData, inputStream);

    return imuSessionData;
  }

  private DataCollectionActionHeader parseDataCollectionActionHeader(
      BufferedInputStream inputStream) throws IOException {
    byte[] partialHeader = new byte[ImuParserImpl.ACTION_HEADER_LENGTH_PARTIAL];
    inputStream.read(partialHeader);
    return imuParser.parseActionHeader(partialHeader);
  }

  private boolean findDelimiterRepetitions(short delimiter, int repetitions,
      BufferedInputStream inputStream) throws IOException {
    DataInputStream dataInputStream = new DataInputStream(inputStream);
    dataInputStream.mark(0);
    for (int i = 0; i < repetitions; i++) {
      if (dataInputStream.available() > 1) {
        short delim = dataInputStream.readShort();
        if (delim != delimiter) {
          dataInputStream.reset();
          return false;
        }
      } else {
        dataInputStream.reset();
        return false;
      }
    }
    return true;
  }

  private void parseImuSamples(ImuSessionData imuSessionData,
      BufferedInputStream inputStream) throws IOException {
    while (inputStream.available() > 0) {
      if (findDelimiterRepetitions(START_DELIMITER, START_DELIMITER_REPETITIONS,
          inputStream)) {
        imuSessionData.setActionHeader(parseDataCollectionActionHeader(inputStream));
      } else if (findDelimiterRepetitions(END_DELIMITER, END_DELIMITER_REPETITIONS, inputStream)) {
        byte[] actionResult = new byte[2];
        inputStream.read(actionResult);
        boolean isError = actionResult[0] + actionResult[1] > 0;
        imuSessionData.updateActionHeaderResult(isError);
      } else {
        if (inputStream.available() >= ImuParserImpl.IMU_SAMPLE_LENGTH) {
          byte[] sampleBytes = new byte[ImuParserImpl.IMU_SAMPLE_LENGTH];
          inputStream.read(sampleBytes);
          imuSessionData.addSample(imuParser.parseImuSample(sampleBytes));
        } else {
          return;
        }
      }
    }
  }
}
