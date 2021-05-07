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

package com.google.android.jacquard.sdk.connection;

import static com.google.android.jacquard.sdk.connection.CommandResponseStatus.ERROR_APP_TIMEOUT;
import static com.google.android.jacquard.sdk.connection.CommandResponseStatus.ERROR_APP_UNKNOWN;
import static com.google.android.jacquard.sdk.connection.CommandResponseStatus.ERROR_AUTH;
import static com.google.android.jacquard.sdk.connection.CommandResponseStatus.ERROR_BAD_PARAM;
import static com.google.android.jacquard.sdk.connection.CommandResponseStatus.ERROR_BATTERY;
import static com.google.android.jacquard.sdk.connection.CommandResponseStatus.ERROR_BUSY;
import static com.google.android.jacquard.sdk.connection.CommandResponseStatus.ERROR_CHECKSUM;
import static com.google.android.jacquard.sdk.connection.CommandResponseStatus.ERROR_DEVICE_TYPE_INFO;
import static com.google.android.jacquard.sdk.connection.CommandResponseStatus.ERROR_FLASH_ACCESS;
import static com.google.android.jacquard.sdk.connection.CommandResponseStatus.ERROR_HARDWARE;
import static com.google.android.jacquard.sdk.connection.CommandResponseStatus.ERROR_INVALID_STATE;
import static com.google.android.jacquard.sdk.connection.CommandResponseStatus.ERROR_UNSUPPORTED;
import static com.google.android.jacquard.sdk.connection.CommandResponseStatus.OK;
import static com.google.common.truth.Truth.assertThat;

import android.os.Build.VERSION_CODES;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.jacquard.sdk.log.PrintLogger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

/**
 * Unit tests for {@link CommandResponseStatus}.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = {VERSION_CODES.P}, manifest = Config.NONE)
public final class CommandResponseStatusTest {

  @Before
  public void setUp() {
    PrintLogger.initialize(ApplicationProvider.getApplicationContext());
  }

  @Test
  public void getStatusCode_returnsCode() {
    // Assert
    assertThat(OK.getStatusCode()).isEqualTo(0);
    assertThat(ERROR_UNSUPPORTED.getStatusCode()).isEqualTo(1);
    assertThat(ERROR_BAD_PARAM.getStatusCode()).isEqualTo(2);
    assertThat(ERROR_BATTERY.getStatusCode()).isEqualTo(3);
    assertThat(ERROR_HARDWARE.getStatusCode()).isEqualTo(4);
    assertThat(ERROR_AUTH.getStatusCode()).isEqualTo(5);
    assertThat(ERROR_DEVICE_TYPE_INFO.getStatusCode()).isEqualTo(6);
    assertThat(ERROR_INVALID_STATE.getStatusCode()).isEqualTo(7);
    assertThat(ERROR_FLASH_ACCESS.getStatusCode()).isEqualTo(8);
    assertThat(ERROR_CHECKSUM.getStatusCode()).isEqualTo(9);
    assertThat(ERROR_BUSY.getStatusCode()).isEqualTo(10);
    assertThat(ERROR_APP_TIMEOUT.getStatusCode()).isEqualTo(253);
    assertThat(ERROR_APP_UNKNOWN.getStatusCode()).isEqualTo(254);
    assertThat(ERROR_INVALID_STATE.getStatusCode()).isEqualTo(7);
  }

  @Test
  public void getStatusCode_returnsThrowable() {
    //Act
    Throwable t = CommandResponseStatus.from(ERROR_BUSY.getStatusCode());
    // Assert
    assertThat(t.getMessage()).contains(ERROR_BUSY.name());
  }
}

