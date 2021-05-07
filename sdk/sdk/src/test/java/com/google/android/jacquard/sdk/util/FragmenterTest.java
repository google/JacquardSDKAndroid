/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.jacquard.sdk.util;

import static com.google.android.jacquard.sdk.util.Fragmenter.FIRST_FRAGMENT_FLAG;
import static com.google.common.truth.Truth.assertThat;

import android.os.Build.VERSION_CODES;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.ProtocolSpec;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

/**
 * Unit tests for {@link Fragmenter}
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = {VERSION_CODES.P}, manifest = Config.NONE)
public final class FragmenterTest {

  private Fragmenter fragmenter;

  private final static byte[] getByteArray(Fragmenter fragmenter, List<byte[]> list) {
    for (byte[] bytes : list) {
      byte[] arr = fragmenter.decodeFragment(bytes);
      if (arr != null) {
        return arr;
      }
    }
    return null;
  }

  @Before
  public void setup() {
    PrintLogger.initialize(ApplicationProvider.getApplicationContext());
    fragmenter = new Fragmenter("commandFragmenter",
        ProtocolSpec.VERSION_2.getMtuSize());
  }

  @Test
  public void fragmentData_returnsNull() {
    // Act
    List<byte[]> bytes = fragmenter.fragmentData(new byte[1025]);
    // Assert
    assertThat(bytes).isNull();
  }

  @Test
  public void fragmentData_returnsEmptyList() {
    // Assign
    byte[] message = new byte[0];
    // Act
    List<byte[]> bytes = fragmenter.fragmentData(message);
    // Assert
    assertThat(bytes).isEmpty();
  }

  @Test
  public void fragmentData_validatedReturnValue() {
    // Assign
    String inputMsg =
        "This classic fable tells the story of a very slow tortoise and a speedy hare. The"
            + " tortoise challenges the hare to a race. The hare laughs at the idea that a"
            + " tortoise could run faster than him, but when the two actually race, the results"
            + " are surprising.";
    byte[] message = inputMsg.getBytes();
    // Act
    List<byte[]> bytes = fragmenter.fragmentData(message);
    // Assert
    assertThat(
        new String(getByteArray(fragmenter, bytes), StandardCharsets.UTF_8).contains(inputMsg))
        .isTrue();
  }

  @Test
  public void decodeFragment_validatedStartHeader() {
    // Assign
    byte[] message = new byte['A'];
    // Act
    byte[] bytes = fragmenter.decodeFragment(message);
    // Assert
    assertThat(bytes).isNull();
  }

  @Test
  public void decodeFragment_validatedEndHeader() {
    // Assign
    byte[] message = new byte[]{(byte) (FIRST_FRAGMENT_FLAG), 'A'};
    // Act
    byte[] bytes = fragmenter.decodeFragment(message);
    // Assert
    assertThat(bytes).isNull();
  }

  @Test
  public void decodeFragment_validatedReturnValue() {
    // Assign
    String inputMsg = "Hello World!";
    byte[] message = inputMsg.getBytes();
    List<byte[]> bytes = fragmenter.fragmentData(message);
    // Act
    byte[] list = fragmenter.decodeFragment(bytes.get(0));
    // Assert
    assertThat(new String(list, StandardCharsets.UTF_8).contains(inputMsg)).isTrue();
  }
}
