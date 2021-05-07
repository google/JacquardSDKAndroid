/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.jacquard.sdk;

import androidx.annotation.NonNull;
import com.google.android.jacquard.sdk.log.PrintLogger;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Queue for executing BLE commands synchronously. */
public class BleQueue {

  private static final String TAG = BleQueue.class.getSimpleName();
  private final Queue<Command> commandQueue = new ConcurrentLinkedQueue<>();
  private boolean commandQueueBusy;

  /**
   * Enqueues a {@link Command} for execution.
   * @param command the command to execute
   * @return {@code true} if the command was appended to the queue
   */
  public synchronized boolean enqueue(Command command) {
    PrintLogger.d(TAG, "enqueue: " + command);
    boolean result = commandQueue.add(command);

    if (result) {
      nextCommand();
    } else {
      PrintLogger.e(TAG, "Could not enqueue command: " + command);
    }
    return result;
  }

  /** If no command is in flight the next command is dequeue and executed. */
  private void nextCommand() {
    PrintLogger.d(TAG, "nextCommand");
    if (commandQueueBusy) {
      PrintLogger.d(TAG, "nextCommand is busy");
      return;
    }

    if (commandQueue.isEmpty()) {
      PrintLogger.d(TAG, "command queue is empty");
      return;
    }
    Command command = commandQueue.element();
    commandQueueBusy = true;
    try {
      PrintLogger.d(TAG, "nextCommand run: " + command);
      command.run();
    } catch (Exception ex) {
      PrintLogger.e(TAG, "Command exception: " + command, ex);
    }
  }

  /**
   * Removes a pending {@link Command} from the queue and executes the next
   * @param type the {@link Command.Type} of command just completed. Used for logging purposes.
   */
  public synchronized void completedCommand(Command.Type type) {
    PrintLogger.d(TAG, "completedCommand");
    commandQueueBusy = false;
    Command command = commandQueue.poll();
    if (command != null && command.type != type) {
      PrintLogger.e(TAG, "Polled: " + command.type + " but completed: " + type);
    }
    nextCommand();
  }

  /** Command class wrapping a runnable BLE command. */
  public abstract static class Command implements Runnable {

    /** The type of command. */
    protected final Type type;

    /**
     * Creates new instance of Command with a type
     * @param type the type of command.
     */
    public Command(Type type) {
      this.type = type;
    }

    @NonNull
    @Override
    public String toString() {
      return "Command{" +
          "type=" + type +
          '}';
    }

    /** The type of BLE command requested. This is used to make sure the requests/responses are in
     * sync. */
    public enum Type {
      READ_CHARACTERISTIC, WRITE_CHARACTERISTIC, WRITE_DESCRIPTOR, DISCOVER_SERVICES
    }
  }

}
