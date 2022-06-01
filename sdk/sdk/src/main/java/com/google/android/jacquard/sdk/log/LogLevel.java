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

/**
 * A set of log levels to print a log message to the console.
 */
public enum LogLevel {
  VERBOSE(/* logLevel= */ 2),
  DEBUG(/* logLevel= */ 3),
  INFO(/* logLevel= */ 4),
  WARNING(/* logLevel= */ 5),
  ERROR(/* logLevel= */ 6),
  ASSERT(/* logLevel= */ 7);

  private final int logLevel;

  LogLevel(int logLevel) {
    this.logLevel = logLevel;
  }

  /**
   * Returns priority log level.
   */
  public int getLogLevel() {
    return logLevel;
  }
}
