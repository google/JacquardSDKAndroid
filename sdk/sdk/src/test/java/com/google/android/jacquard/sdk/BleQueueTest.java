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

package com.google.android.jacquard.sdk;

import static com.google.android.jacquard.sdk.BleQueue.Command.Type.READ_CHARACTERISTIC;
import static com.google.android.jacquard.sdk.BleQueue.Command.Type.WRITE_CHARACTERISTIC;
import static com.google.common.truth.Truth.assertThat;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.jacquard.sdk.log.PrintLogger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unit tests for {@link BleQueue}.
 */
@RunWith(AndroidJUnit4.class)
public final class BleQueueTest {

  private BleQueue bleQueue;

  @Before
  public void setUp() {
    PrintLogger.initialize(ApplicationProvider.getApplicationContext());
    bleQueue = new BleQueue();
  }

  @Test
  public void enqueue_queueEmpty_enqueuesCommand() {
    // Arrange
    FakeCommand command = new FakeCommand(READ_CHARACTERISTIC);
    // Act
    boolean result = bleQueue.enqueue(command);
    // Assert
    assertThat(result).isTrue();
  }

  @Test
  public void enqueue_enqueuesCommand_interruptsCommandExcution() {
    // Arrange
    FakeCommand command = new FakeCommand(READ_CHARACTERISTIC);
    command.setInterruption(true);
    // Act
    boolean result = bleQueue.enqueue(command);
    // Assert
    assertThat(result).isTrue();
  }

  @Test
  public void completedCommand_emptyCommandQueue() {
    // Arrange
    FakeCommand command = new FakeCommand(READ_CHARACTERISTIC);
    // Act
    boolean result = bleQueue.enqueue(command);
    bleQueue.completedCommand(WRITE_CHARACTERISTIC);
    // Assert
    assertThat(result).isTrue();
  }
}
