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

import android.content.Context;
import java.io.File;

/**
 * An interface describes the type you must implement if you wish to provide your own logging
 * implementation.
 */
public interface Logger {

  /**
   * Logs a {@link LogLevel#VERBOSE} message.
   *
   * @param tag     the source of a log message, the class or activity where the log call occurs
   * @param message the message you would like logged
   */
  void v(String tag, String message);

  /**
   * Logs a {@link LogLevel#VERBOSE} message.
   *
   * @param tag     the source of a log message, the class or activity where the log call occurs
   * @param message the message you would like logged
   * @param t       an exception to log
   */
  void v(String tag, String message, Throwable t);

  /**
   * Logs a {@link LogLevel#DEBUG} message.
   *
   * @param tag     the source of a log message, the class or activity where the log call occurs
   * @param message the message you would like logged
   */
  void d(String tag, String message);

  /**
   * Logs a {@link LogLevel#DEBUG} message.
   *
   * @param tag     the source of a log message, the class or activity where the log call occurs
   * @param message the message you would like logged
   * @param t       an exception to log
   */
  void d(String tag, String message, Throwable t);

  /**
   * Logs a {@link LogLevel#INFO} message.
   *
   * @param tag     the source of a log message, the class or activity where the log call occurs
   * @param message the message you would like logged
   */
  void i(String tag, String message);

  /**
   * Logs a {@link LogLevel#INFO} log message.
   *
   * @param tag     the source of a log message, the class or activity where the log call occurs
   * @param message the message you would like logged
   * @param t       an exception to log
   */
  void i(String tag, String message, Throwable t);

  /**
   * Logs a {@link LogLevel#WARNING} message.
   *
   * @param tag     the source of a log message, the class or activity where the log call occurs
   * @param message the message you would like logged
   */
  void w(String tag, String message);

  /**
   * Logs a {@link LogLevel#WARNING} message.
   *
   * @param tag     the source of a log message, the class or activity where the log call occurs
   * @param message the message you would like logged
   * @param t       an exception to log
   */
  void w(String tag, String message, Throwable t);

  /**
   * Logs a {@link LogLevel#ERROR} message.
   *
   * @param tag     the source of a log message, the class or activity where the log call occurs
   * @param message the message you would like logged
   */
  void e(String tag, String message);

  /**
   * Logs a {@link LogLevel#ERROR} message.
   *
   * @param tag     the source of a log message, the class or activity where the log call occurs
   * @param message the message you would like logged
   * @param t       an exception to log
   */
  void e(String tag, String message, Throwable t);

  /**
   * Logs a message with log level.
   *
   * @param logLevel log level of the message to be displayed on console
   * @param tag      the source of a log message, the class or activity where the log call occurs
   * @param message  the message you would like logged
   */
  void log(LogLevel logLevel, String tag, String message);

  /**
   * Returns a log file with log details.
   *
   * @param context application context from a caller app.
   */
  File getLogFile(Context context);

}
