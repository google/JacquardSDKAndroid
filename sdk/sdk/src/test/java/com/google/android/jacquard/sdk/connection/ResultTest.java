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

import static com.google.android.jacquard.sdk.connection.Result.Type.FAILURE;
import static com.google.android.jacquard.sdk.connection.Result.Type.SUCCESS;
import static com.google.android.jacquard.sdk.connection.Result.ofFailure;
import static com.google.android.jacquard.sdk.connection.Result.ofSuccess;
import static com.google.common.truth.Truth.assertThat;
import static java.lang.Boolean.TRUE;

import android.os.Build.VERSION_CODES;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.common.truth.Truth;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

/**
 * Unit tests for {@link Result}.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = {VERSION_CODES.P}, manifest = Config.NONE)
public class ResultTest {

  @Before
  public void setUp() {
    PrintLogger.initialize(ApplicationProvider.getApplicationContext());
  }

  @Test
  public void ofFailure_returnsTypeFailure() {
    // Act
    Result res = ofFailure(new Throwable());
    // Assert
    assertThat(res.getType()).isEqualTo(FAILURE);
  }

  @Test
  public void ofSuccess_returnsTypeSuccess() {
    // Act
    Result res = ofSuccess(TRUE);
    // Assert
    assertThat(res.getType()).isEqualTo(SUCCESS);
  }
}
