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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import android.os.Build.VERSION_CODES;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.common.collect.ImmutableList;
import java.io.BufferedWriter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.annotation.Config;

/**
 * Unit tests for {@link PrintLogger}.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = {VERSION_CODES.P}, manifest = Config.NONE)
public class LoggerImplTest {

  private static final String TAG = PrintLoggerTest.class.getSimpleName();
  private static final String MESSAGE = "message for testing";
  private static final Throwable THROWABLE = new Throwable();
  private LoggerImpl logger;

  @Before
  public void setUp() {
    logger = new LoggerImpl(ImmutableList.of(INFO, WARNING, ERROR, ASSERT),
        ApplicationProvider.getApplicationContext());
  }

  @Test
  public void initialization() {
    // Act
    BufferedWriter writer = Mockito.spy(logger.getLogWriter());
    // Assert
    verifyNoInteractions(writer);
  }

  @Test
  public void logVerbose() {
    // Arrange
    LoggerImpl loggerSpy = Mockito.spy(logger);
    // Act
    loggerSpy.v(TAG, MESSAGE);
    // Assert
    verify(loggerSpy).v(TAG, MESSAGE);
  }

  @Test
  public void logVerboseWithException() {
    // Arrange
    LoggerImpl loggerSpy = Mockito.spy(logger);
    // Act
    loggerSpy.v(TAG, MESSAGE, THROWABLE);
    // Assert
    verify(loggerSpy).v(TAG, MESSAGE, THROWABLE);
  }

  @Test
  public void logDebug() {
    // Arrange
    LoggerImpl loggerSpy = Mockito.spy(logger);
    // Act
    loggerSpy.d(TAG, MESSAGE);
    // Assert
    verify(loggerSpy).d(TAG, MESSAGE);
  }

  @Test
  public void logDebugWithException() {
    // Arrange
    LoggerImpl loggerSpy = Mockito.spy(logger);
    // Act
    loggerSpy.d(TAG, MESSAGE, THROWABLE);
    // Assert
    verify(loggerSpy).d(TAG, MESSAGE, THROWABLE);
  }

  @Test
  public void logInfo() {
    // Arrange
    LoggerImpl loggerSpy = Mockito.spy(logger);
    // Act
    loggerSpy.i(TAG, MESSAGE);
    // Assert
    verify(loggerSpy).i(TAG, MESSAGE);
  }

  @Test
  public void logInfoWithException() {
    // Arrange
    LoggerImpl loggerSpy = Mockito.spy(logger);
    // Act
    loggerSpy.i(TAG, MESSAGE, THROWABLE);
    // Assert
    verify(loggerSpy).i(TAG, MESSAGE, THROWABLE);
  }

  @Test
  public void logWarning() {
    // Arrange
    LoggerImpl loggerSpy = Mockito.spy(logger);
    // Act
    loggerSpy.w(TAG, MESSAGE);
    // Assert
    verify(loggerSpy).w(TAG, MESSAGE);
  }

  @Test
  public void logWarningWithException() {
    // Arrange
    LoggerImpl loggerSpy = Mockito.spy(logger);
    // Act
    loggerSpy.w(TAG, MESSAGE, THROWABLE);
    // Assert
    verify(loggerSpy).w(TAG, MESSAGE, THROWABLE);
  }

  @Test
  public void logError() {
    // Arrange
    LoggerImpl loggerSpy = Mockito.spy(logger);
    // Act
    loggerSpy.e(TAG, MESSAGE);
    // Assert
    verify(loggerSpy).e(TAG, MESSAGE);
  }

  @Test
  public void logErrorWithException() {
    // Arrange
    LoggerImpl loggerSpy = Mockito.spy(logger);
    // Act
    loggerSpy.e(TAG, MESSAGE, THROWABLE);
    // Assert
    verify(loggerSpy).e(TAG, MESSAGE, THROWABLE);
  }

  @Test
  public void getFile() {
    // Arrange
    LoggerImpl loggerSpy = Mockito.spy(logger);
    // Act
    loggerSpy.getLogFile(ApplicationProvider.getApplicationContext());
    // Assert
    verify(loggerSpy).getLogFile(any());
  }
}
