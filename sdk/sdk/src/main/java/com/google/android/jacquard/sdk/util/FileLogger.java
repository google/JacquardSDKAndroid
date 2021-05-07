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

package com.google.android.jacquard.sdk.util;

import androidx.annotation.Nullable;
import com.google.android.jacquard.sdk.log.PrintLogger;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Wrapper to save data to file.
 */
public final class FileLogger {

  private static final String TAG = FileLogger.class.getSimpleName();
  private File destination;
  private FileChannel channel;

  /**
   * Public constructor. Make sure to have write permission to target directory.
   */
  public FileLogger(String directory, String fileName) {
    File folder = new File(directory);
    folder.mkdirs();

    destination = new File(directory, fileName);
    try {
      PrintLogger.d(TAG,
          "File exists ? " + destination.exists() + ", append at # " + destination.length()
              + ", Path #"
              + destination.getAbsolutePath());
      if (!destination.exists()) {
        destination.createNewFile();
      }
      channel = new FileOutputStream(destination, true).getChannel();
    } catch (IOException e) {
      PrintLogger.e(TAG, e.getMessage(), e);
    }
    PrintLogger.d(TAG, "Logging to # " + destination.getAbsolutePath());
  }

  /**
   * Appends data to end of the target file.
   */
  public void log(byte[] data) {
    try {
      ByteBuffer buffer = ByteBuffer.allocate(data.length).put(data);
      buffer.flip();
      channel.write(buffer);
    } catch (IOException e) {
      PrintLogger.e(TAG, e.getMessage(), e);
      error();
    }
  }

  /**
   * Flushes the buffer and releases the resources.
   */
  public void done() {
    try {
      channel.force(true);
      channel.close();
      if (destination.length() == 0) {
        PrintLogger.d(TAG, "Deleting Empty file # ");
        destination.delete(); // delete empty file.
      } else {
        PrintLogger.d(
            TAG,
            String.format(
                "Data has been logged to # %s. Length = %d",
                destination.getAbsolutePath(), destination.length()));
      }
    } catch (IOException e) {
      PrintLogger.e(TAG, e.getMessage(), e);
    }
  }

  /**
   * Returns destination file.
   */
  @Nullable
  public File getFile() {
    return destination.exists() ? destination : null;
  }

  private void error() {
    PrintLogger.d(TAG, "onError ## ");
    done();
    destination.delete();
  }
}
