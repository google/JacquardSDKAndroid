/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.jacquard.sdk.dfu;

import android.text.TextUtils;
import android.util.Pair;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** A collection of methods to deal with generic dfu activities. */
final class DfuUtil {

  private static final String TAG = DfuUtil.class.getSimpleName();
  static final String SEPARATOR = "_";

  /** Returns the CRC16(Cyclic-Redundency-Check) int value of provided byte data and length. */
  static int crc16(byte[] data, int len) {
    int crc = 0;
    int offset = 0;
    for (int i = offset; i < len; i++) {
      crc = ((crc >> 8) & 0xFF) | (crc << 8);
      crc ^= data[i] & 0xFF;
      crc ^= (crc & 0xFF) >> 4;
      crc ^= ((crc << 8) << 4) & 0xFFFF;
      crc ^= ((crc & 0xFF) << 4) << 1;
      crc &= 0xFFFF;
    }
    return crc;
  }

  /** Returns the file size using the DfuInfo object. */
  static long getFileSize(String dir, DFUInfo info) {
    File file = new File(dir, getFileName(info));
    if (file.exists()) {
      return file.length();
    } else {
      PrintLogger.d(TAG, "File not found: " + info);
      return 0;
    }
  }

  /** Returns the absolute path of provided DfuInfo object. */
  static String getFirmwareFilePath(String dir, DFUInfo info) {
    File file = new File(dir, getFileName(info));
    return file.getAbsolutePath();
  }

  /** Returns file name using the DfuInfo object. */
  private static String getFileName(DFUInfo info) {
    ImmutableList.Builder<String> listBuilder = new ImmutableList.Builder<>();
    listBuilder.add(info.vendorId());
    listBuilder.add(info.productId());
    if (!TextUtils.isEmpty(info.moduleId())) {
      listBuilder.add(info.moduleId());
    }
    listBuilder.add(info.version().toString());
    Joiner joiner = Joiner.on(SEPARATOR);
    return joiner.join(listBuilder.build());
  }

  /**
   * Writes the InputStream to the File. This will overwrite the file if File already exists.
   * It closes the <code>inputStream</code> after consuming it.
   *
   * @param inputStream {@link InputStream} to read.
   * @param file {@link File} to write to.
   */
  static Signal<Boolean> inputStreamToFile(InputStream inputStream, File file) {

    return Signal.create(signal -> {

      boolean newFile;
      OutputStream outputStream = null;
      try {
        newFile = !file.exists() && file.createNewFile();

        outputStream = new BufferedOutputStream(new FileOutputStream(file));
        byte[] buffer = new byte[1024];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
          outputStream.write(buffer, 0, read);
        }
        outputStream.flush();
        PrintLogger
            .d(TAG, /* message= */ "File: " + file.getCanonicalFile() + " created: " + newFile);
        signal.next(true);
      } catch (IOException e) {
        PrintLogger.e(TAG, /* message= */ e.getMessage());
        signal.error(e);
      } finally {
        try {
          if (outputStream != null) {
            outputStream.close();
          }
          inputStream.close();
        } catch (IOException ioe) {
          PrintLogger.e(TAG, /* message= */ ioe.getMessage());
        }
      }

      return new Subscription();
    });
  }

  /**
   * Returns the {@link Pair} of InputStream and Underlying file length.
   *
   * @param path Complete Path of the File for InputStream.
   * @return {@link Pair}
   * @throws FileNotFoundException If File does not exists or App does not have permission to
   *     access the file.
   */
  static Pair<InputStream, Long> getFileInputStream(String path) throws FileNotFoundException {
    File file = new File(path);
    if (!file.exists()) {
      return Pair.create(null, 0L);
    }

    return Pair.create(new FileInputStream(file), file.length());
  }
}
