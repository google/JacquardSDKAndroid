/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.jacquard.sdk.util;

import android.os.Build.VERSION_CODES;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link StringUtils}
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = {VERSION_CODES.P}, manifest = Config.NONE)
public final class StringUtilsTests {

  @Test
  public void integerToHexString() {
    StringUtils u = StringUtils.getInstance();
    assertEquals(u.integerToHexString(154091947), "09-2f-41-ab");
    assertEquals(u.integerToHexString(-78143214), "fb-57-a1-12");
    assertEquals(u.integerToHexString(1), "00-00-00-01");
  }

  @Test
  public void hexStringToInteger() {
    StringUtils u = StringUtils.getInstance();
    assertEquals(u.hexStringToInteger("fb-57-a1-12"), -78143214);
    assertEquals(u.hexStringToInteger("09-2f-41-ab"), 154091947);
    assertEquals(u.hexStringToInteger("00-00-00-01"), 1);
  }

}
