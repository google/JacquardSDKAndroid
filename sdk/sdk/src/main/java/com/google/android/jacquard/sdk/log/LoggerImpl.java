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
 *
 */

package com.google.android.jacquard.sdk.log;


import static android.util.Log.println;
import static com.google.android.jacquard.sdk.log.LogLevel.DEBUG;
import static com.google.android.jacquard.sdk.log.LogLevel.ERROR;
import static com.google.android.jacquard.sdk.log.LogLevel.INFO;
import static com.google.android.jacquard.sdk.log.LogLevel.VERBOSE;
import static com.google.android.jacquard.sdk.log.LogLevel.WARNING;

import android.content.Context;
import android.text.format.DateFormat;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * A default {@link Logger} implementation that prints message to the console.
 */
class LoggerImpl implements Logger {
  private static final String TAG = LoggerImpl.class.getSimpleName();

  /**
   * Which log levels should be displayed.
   */
  private final ImmutableList<LogLevel> logLevels;

  /**
   * Log file name.
   */
  private static final String LOG_FILE_NAME = "sdk_log";

  /**
   * Max size of log file.
   */
  private static final long LOG_FILE_MAX_SIZE = 10485760; // 10 * 2^20 (10MiB)
  private static final Object m_lock = new Object();
  private BufferedWriter logWriter;

  private List<String> ignoreStringList = new ArrayList<>();

  /**
   * Creates a LoggerImpl instance.
   *
   * @param logLevels which log levels should be displayed
   */
  LoggerImpl(@NonNull ImmutableList<LogLevel> logLevels, Context context) {
    this.logLevels = logLevels;
    try {
      rotateLog(context, 0);
      File logFile = new File(context.getFilesDir(), LOG_FILE_NAME);
      logWriter = new BufferedWriter(new FileWriter(logFile, false));
    } catch (IOException e) {
      Log.e(TAG, "failed to init log writer", e);
    }
    enableCrashReporting();
  }

  public void addToIgnoreList(String ignoreText) {
    ignoreStringList.add(ignoreText);
  }

  @Override
  public void v(String tag, String message) {
    log(VERBOSE, tag, message);
  }

  @Override
  public void v(String tag, String message, Throwable t) {
    log(VERBOSE, tag, message + Log.getStackTraceString(t));
  }

  @Override
  public void d(String tag, String message) {
    log(DEBUG, tag, message);
  }

  @Override
  public void d(String tag, String message, Throwable t) {
    log(DEBUG, tag, message + Log.getStackTraceString(t));
  }

  @Override
  public void i(String tag, String message) {
    log(INFO, tag, message);
  }

  @Override
  public void i(String tag, String message, Throwable t) {
    log(INFO, tag, message + Log.getStackTraceString(t));
  }

  @Override
  public void w(String tag, String message) {
    log(WARNING, tag, message);
  }

  @Override
  public void w(String tag, String message, Throwable t) {
    log(WARNING, tag, message + Log.getStackTraceString(t));
  }

  @Override
  public void e(String tag, String message) {
    log(ERROR, tag, message);
  }

  @Override
  public void e(String tag, String message, Throwable t) {
    log(ERROR, tag, message + Log.getStackTraceString(t));
  }

  @Override
  public void log(LogLevel logLevel, String tag, String message) {
    synchronized (m_lock) {
      if (!logLevels.contains(logLevel)) {
        return;
      }
      if (shouldIgnore(message)) {
        // do not print the logs containing ignore texts.
        return;
      }
      println(logLevel.getLogLevel(), tag, message);
      String date = DateFormat.format("yyyy-MM-dd hh:mm:ss", System.currentTimeMillis()).toString();
      try {
        logWriter.write(String.format("%s %s: %s\n", date, tag, message));
      } catch (IOException e) {
        println(Level.INFO.intValue(), TAG, e.getMessage());
      }
    }
  }

  private boolean shouldIgnore(String message) {
    for (String ignore : ignoreStringList) {
      if (message.contains(ignore)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public File getLogFile(Context context) {
    instanceFlush();
    return assembleLogFile(context, 0, 0);
  }

  @VisibleForTesting
  BufferedWriter getLogWriter(){
    return logWriter;
  }

  /**
   * Flush the current instance log file out to storage.
   */
  private void instanceFlush() {
    synchronized (m_lock) {
      if (logWriter == null) {
        return;
      }
      try {
        logWriter.flush();
      } catch (IOException e) {
        println(Level.INFO.intValue(), TAG, e.getMessage());
      }
    }
  }

  /**
   * Rotate logs, keep at most 10 files, rotate every time Logger is initialized.
   */
  private void rotateLog(final Context c, final int count) {
    final File current =
        new File(
            c.getFilesDir(),
            count == 0 ? LOG_FILE_NAME : (LOG_FILE_NAME + "_" + count));
    if (count > 9 || !current.isFile()) {
      return;
    }
    rotateLog(c, count + 1); // rotate count+1 first
    final File next = new File(c.getFilesDir(), LOG_FILE_NAME + "_" + (count + 1));
    next.delete();
    current.renameTo(next);
  }

  /**
   * Retrieve at most LOG_FILE_MAX_SIZE bytes of log.
   */
  private File assembleLogFile(final Context c, final long size, final int count) {
    final File current =
        new File(
            c.getFilesDir(),
            count == 0 ? LOG_FILE_NAME : (LOG_FILE_NAME + "_" + count));
    if (!current.isFile() || (current.length() + size) > LOG_FILE_MAX_SIZE) {
      final File out = new File(c.getFilesDir(), LOG_FILE_NAME + "_combined");
      if (current.isFile() && current.length() > 0) {
        try (FileChannel fin = new FileInputStream(current).getChannel();
            FileChannel fout = new FileOutputStream(out, false).getChannel()) {
          fin.transferTo(
              Math.max(0, current.length() - (LOG_FILE_MAX_SIZE - size)),
              Math.min(current.length(), LOG_FILE_MAX_SIZE - size),
              fout);
        } catch (IOException e) {
          Log.e(TAG, "Failed to assemble log", e);
        }
      }
      return out;
    }
    final File output = assembleLogFile(c, size + current.length(), count + 1);
    try (FileChannel fin = new FileInputStream(current).getChannel();
        FileChannel fout = new FileOutputStream(output, true).getChannel()) {
      fin.transferTo(0, current.length(), fout);
    } catch (IOException e) {
      Log.e(TAG, "Failed to assemble log", e);
    }
    return output;
  }

  private void enableCrashReporting() {
    final Thread.UncaughtExceptionHandler originalHandler =
        Thread.getDefaultUncaughtExceptionHandler();
    Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
      PrintLogger.e("UncaughtException", ex.toString(), ex);
      instanceFlush();
      if (originalHandler != null) {
        originalHandler.uncaughtException(thread, ex);
      }
    });
  }
}
