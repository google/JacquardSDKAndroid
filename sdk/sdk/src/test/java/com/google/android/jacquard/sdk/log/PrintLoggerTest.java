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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.os.Build.VERSION_CODES;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

/**
 * Unit tests for {@link PrintLogger}.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = {VERSION_CODES.P}, manifest = Config.NONE)
public class PrintLoggerTest {

  private static final String TAG = PrintLoggerTest.class.getSimpleName();
  private static final String MESSAGE = "message for testing";
  private static final Throwable THROWABLE = new Throwable();
  private LoggerImpl logger;

  @Before
  public void setUp() {
    logger = mock(LoggerImpl.class);
  }

  @Test
  public void defaultInitialization() {
    // Arrange
    PrintLogger.initialize(ApplicationProvider.getApplicationContext());
    PrintLogger.setGlobalJacquardSDKLogger(logger);
    // Act
    PrintLogger.i(TAG, MESSAGE);
    // Assert
    verify(logger).i(TAG, MESSAGE);
  }

  @Test
  public void initializationWithLogLevels() {
    // Arrange
    ImmutableList<LogLevel> logLevels = ImmutableList.of(LogLevel.VERBOSE, LogLevel.DEBUG);
    PrintLogger.initialize(logLevels, ApplicationProvider.getApplicationContext());
    PrintLogger.setGlobalJacquardSDKLogger(logger);
    // Act
    PrintLogger.i(TAG, MESSAGE);
    // Assert
    verify(logger).i(TAG, MESSAGE);
  }

  @Test
  public void logVerbose() {
    // Arrange
    PrintLogger.initialize(ApplicationProvider.getApplicationContext());
    PrintLogger.setGlobalJacquardSDKLogger(logger);
    // Act
    PrintLogger.v(TAG, MESSAGE);
    // Assert
    verify(logger).v(TAG, MESSAGE);
  }

  @Test
  public void logVerboseWithException() {
    // Arrange
    PrintLogger.initialize(ApplicationProvider.getApplicationContext());
    PrintLogger.setGlobalJacquardSDKLogger(logger);
    // Act
    PrintLogger.v(TAG, MESSAGE, THROWABLE);
    // Assert
    verify(logger).v(TAG, MESSAGE, THROWABLE);
  }

  @Test
  public void logDebug() {
    // Arrange
    PrintLogger.initialize(ApplicationProvider.getApplicationContext());
    PrintLogger.setGlobalJacquardSDKLogger(logger);
    // Act
    PrintLogger.d(TAG, MESSAGE);
    // Assert
    verify(logger).d(TAG, MESSAGE);
  }

  @Test
  public void logDebugWithException() {
    // Arrange
    PrintLogger.initialize(ApplicationProvider.getApplicationContext());
    PrintLogger.setGlobalJacquardSDKLogger(logger);
    // Act
    PrintLogger.d(TAG, MESSAGE, THROWABLE);
    // Assert
    verify(logger).d(TAG, MESSAGE, THROWABLE);
  }

  @Test
  public void logInfo() {
    // Arrange
    PrintLogger.initialize(ApplicationProvider.getApplicationContext());
    PrintLogger.setGlobalJacquardSDKLogger(logger);
    // Act
    PrintLogger.i(TAG, MESSAGE);
    // Assert
    verify(logger).i(TAG, MESSAGE);
  }

  @Test
  public void logInfoWithException() {
    // Arrange
    PrintLogger.initialize(ApplicationProvider.getApplicationContext());
    PrintLogger.setGlobalJacquardSDKLogger(logger);
    // Act
    PrintLogger.i(TAG, MESSAGE, THROWABLE);
    // Assert
    verify(logger).i(TAG, MESSAGE, THROWABLE);
  }

  @Test
  public void logWarning() {
    // Arrange
    PrintLogger.initialize(ApplicationProvider.getApplicationContext());
    PrintLogger.setGlobalJacquardSDKLogger(logger);
    // Act
    PrintLogger.w(TAG, MESSAGE);
    // Assert
    verify(logger).w(TAG, MESSAGE);
  }

  @Test
  public void logWarningWithException() {
    // Arrange
    PrintLogger.initialize(ApplicationProvider.getApplicationContext());
    PrintLogger.setGlobalJacquardSDKLogger(logger);
    // Act
    PrintLogger.w(TAG, MESSAGE, THROWABLE);
    // Assert
    verify(logger).w(TAG, MESSAGE, THROWABLE);
  }

  @Test
  public void logError() {
    // Arrange
    PrintLogger.initialize(ApplicationProvider.getApplicationContext());
    PrintLogger.setGlobalJacquardSDKLogger(logger);
    // Act
    PrintLogger.e(TAG, MESSAGE);
    // Assert
    verify(logger).e(TAG, MESSAGE);
  }

  @Test
  public void logErrorWithException() {
    // Arrange
    PrintLogger.initialize(ApplicationProvider.getApplicationContext());
    PrintLogger.setGlobalJacquardSDKLogger(logger);
    // Act
    PrintLogger.e(TAG, MESSAGE, THROWABLE);
    // Assert
    verify(logger).e(TAG, MESSAGE, THROWABLE);
  }

  @Test
  public void log() {
    // Arrange
    PrintLogger.initialize(ApplicationProvider.getApplicationContext());
    PrintLogger.setGlobalJacquardSDKLogger(logger);
    // Act
    PrintLogger.log(LogLevel.DEBUG, TAG, MESSAGE);
    // Assert
    verify(logger).log(LogLevel.DEBUG, TAG, MESSAGE);
  }

  @Test
  public void getLogFile() {
    // Arrange
    PrintLogger.initialize(ApplicationProvider.getApplicationContext());
    PrintLogger.setGlobalJacquardSDKLogger(logger);
    // Act
    PrintLogger.getLogFile(ApplicationProvider.getApplicationContext());
    // Assert
    verify(logger).getLogFile(ApplicationProvider.getApplicationContext());
  }
}
