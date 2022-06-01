/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.jacquard.sdk.dfu.model;

import static com.google.common.truth.Truth.assertThat;

import android.os.Build.VERSION_CODES;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.io.IOException;
import java.io.InputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

@RunWith(AndroidJUnit4.class)
@Config(sdk = {VERSION_CODES.P}, manifest = Config.NONE)
public class FileDescriptorTest {

  @Test
  public void giveValueTypeWithAutoValue_whenFiledCorrectSet_thenCorrect() {

    // Assign
    InputStream in = new InputStream() {
      @Override
      public int read() throws IOException {
        return 0;
      }
    };

    // Act
    FileDescriptor fileDescriptor = FileDescriptor.create(in, 100);

    // Assert
    assertThat(fileDescriptor.inputStream()).isEqualTo(in);
    assertThat(fileDescriptor.totalSize()).isEqualTo(100);
  }

  @Test
  public void giveTwoValueTypeWithAutoValue_whenEqual_thenCorrect() {

    // Assign
    InputStream in = new InputStream() {
      @Override
      public int read() throws IOException {
        return 0;
      }
    };

    // Act
    FileDescriptor fileDescriptor1 = FileDescriptor.create(in, 100);
    FileDescriptor fileDescriptor2 = FileDescriptor.create(in, 100);

    // Assert
    assertThat(fileDescriptor1.equals(fileDescriptor2)).isTrue();
  }

  @Test
  public void giveTwoValueTypeWithAutoValue_whenNotEqual_thenCorrect() {

    // Assign
    InputStream in = new InputStream() {
      @Override
      public int read() throws IOException {
        return 0;
      }
    };

    // Act
    FileDescriptor fileDescriptor1 = FileDescriptor.create(in, 100);
    FileDescriptor fileDescriptor2 = FileDescriptor.create(in, 200);

    // Assert
    assertThat(fileDescriptor1.equals(fileDescriptor2)).isFalse();
  }
}
