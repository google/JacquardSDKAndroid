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

import static com.google.android.jacquard.sdk.log.LogLevel.ASSERT;
import static com.google.android.jacquard.sdk.log.LogLevel.ERROR;
import static com.google.android.jacquard.sdk.log.LogLevel.INFO;
import static com.google.android.jacquard.sdk.log.LogLevel.WARNING;

import android.content.Context;
import android.util.Log;
import com.google.common.collect.ImmutableList;
import java.io.File;

/**
 * A class which configures logger to print a log message to the console.
 */
public class PrintLogger {

  private static final String TAG = PrintLogger.class.getSimpleName();
  private static Logger logger;

  /**
   * Sets the global {@link Logger} instance used by all code in Jacquard SDK.
   * <p>
   * You do not need to set this - the default logger prints log messages to the console using
   * {@link Log#println(int, String, String)}, ignoring {@link LogLevel#VERBOSE} {@link
   * LogLevel#DEBUG}. You can also set a default logger with log level which you desire, or you may
   * implement your own custom {@link Logger} type.
   *
   * @param logger new {@link Logger} instance
   */
  public static void setGlobalJacquardSDKLogger(Logger logger) {
    PrintLogger.logger = logger;
  }

  /**
   * Initializes a default logger {@link Logger} with a log level which you desire.
   * <p>
   * You can also use {@link #initialize(Context)} with a default log levels.
   *
   * @param logLevels which log levels {@link LogLevel} should be displayed
   */
  public static void initialize(ImmutableList<LogLevel> logLevels, Context context) {
    if (logger == null) {
      synchronized (PrintLogger.class) {
        if (logger == null) {
          logger = new LoggerImpl(logLevels, context);
        }
      }
    }
  }

  /**
   * Initializes a default logger with a log level {@link LogLevel#INFO}, {@link LogLevel#WARNING},
   * {@link LogLevel#ERROR}, {@link LogLevel#ASSERT}.
   * <p>
   * You can also set a log level which you desire {@link #initialize(ImmutableList, Context)}.
   */
  public static void initialize(Context context) {
    initialize(ImmutableList.of(INFO, WARNING, ERROR, ASSERT), context);
  }

  /**
   * Ignores the logs containing the provided text.
   *
   * @param text     the string to filter the logs.
   */
  public static void ignore(String text) {
    logger.addToIgnoreList(text);
  }

  /**
   * Sends a {@link LogLevel#VERBOSE} log message.
   *
   * @param tag     the source of a log message, the class or activity where the log call occurs
   * @param message the message you would like logged
   */
  public static void v(String tag, String message) {
    logger.v(tag, message);
  }

  /**
   * Sends a {@link LogLevel#VERBOSE} log message.
   *
   * @param tag     the source of a log message, the class or activity where the log call occurs
   * @param message the message you would like logged
   * @param t       an exception to log
   */
  public static void v(String tag, String message, Throwable t) {
    logger.v(tag, message, t);
  }

  /**
   * Sends a {@link LogLevel#DEBUG} log message.
   *
   * @param tag     the source of a log message, the class or activity where the log call occurs
   * @param message the message you would like logged
   */
  public static void d(String tag, String message) {
    logger.d(tag, message);
  }

  /**
   * Sends a {@link LogLevel#DEBUG} log message.
   *
   * @param tag     the source of a log message, the class or activity where the log call occurs
   * @param message the message you would like logged
   * @param t       an exception to log
   */
  public static void d(String tag, String message, Throwable t) {
    logger.d(tag, message, t);
  }

  /**
   * Sends a {@link LogLevel#INFO} log message.
   *
   * @param tag     the source of a log message, the class or activity where the log call occurs
   * @param message the message you would like logged
   */
  public static void i(String tag, String message) {
    logger.i(tag, message);
  }

  /**
   * Sends a {@link LogLevel#INFO} log message.
   *
   * @param tag     the source of a log message, the class or activity where the log call occurs
   * @param message the message you would like logged
   * @param t       an exception to log
   */
  public static void i(String tag, String message, Throwable t) {
    logger.i(tag, message, t);
  }

  /**
   * Sends a {@link LogLevel#WARNING} log message.
   *
   * @param tag     the source of a log message, the class or activity where the log call occurs
   * @param message the message you would like logged
   */
  public static void w(String tag, String message) {
    logger.w(tag, message);
  }

  /**
   * Sends a {@link LogLevel#WARNING} log message.
   *
   * @param tag     the source of a log message, the class or activity where the log call occurs
   * @param message the message you would like logged
   * @param t       an exception to log
   */
  public static void w(String tag, String message, Throwable t) {
    logger.w(tag, message, t);
  }

  /**
   * Sends a {@link LogLevel#ERROR} log message.
   *
   * @param tag     the source of a log message, the class or activity where the log call occurs
   * @param message the message you would like logged
   */
  public static void e(String tag, String message) {
    logger.e(tag, message);
  }

  /**
   * Sends a {@link LogLevel#ERROR} log message.
   *
   * @param tag     the source of a log message, the class or activity where the log call occurs
   * @param message the message you would like logged
   * @param t       an exception to log
   */
  public static void e(String tag, String message, Throwable t) {
    logger.e(tag, message, t);
  }

  /**
   * Sends a log message.
   *
   * @param logLevel priority level of the message
   * @param tag      the source of a log message, the class or activity where the log call occurs
   * @param message  the message you would like logged
   */
  public static void log(LogLevel logLevel, String tag, String message) {
    logger.log(logLevel, tag, message);
  }

  /**
   * Returns a log file.
   *
   * @param context application context from a caller app.
   */
  public static File getLogFile(Context context) {
    return logger.getLogFile(context);
  }
}
