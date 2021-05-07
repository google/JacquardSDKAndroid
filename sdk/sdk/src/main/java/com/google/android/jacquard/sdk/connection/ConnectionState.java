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
package com.google.android.jacquard.sdk.connection;

import com.google.android.jacquard.sdk.model.JacquardError;
import com.google.android.jacquard.sdk.model.Progress;
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag;
import com.google.auto.value.AutoOneOf;
import java.io.Serializable;

/** The different states on the connection process. */
@AutoOneOf(ConnectionState.Type.class)
public abstract class ConnectionState implements Serializable {

  public enum Type {
    PREPARING_TO_CONNECT,
    CONNECTING,
    INITIALIZING,
    CONFIGURING,
    CONNECTED,
    DISCONNECTED
  }

  public boolean isTerminal() {
    return getType() == Type.DISCONNECTED;
  }

  public boolean isType(Type type) {
    return getType() == type;
  }

  public abstract Type getType();

  /** This is initial state, and also the state while waiting for reconnection. */
  public abstract void preparingToConnect();

  /**
   * Connecting with approximate progress.
   * @return progress object with current step and total steps (including initializing).
   */
  public abstract Progress connecting();

  // Initializing with approximate progress.
  // First Int is the current step, second Int is total number of steps. This continues on from the progress reported by the
  // `connecting` state.

  /**
   * Initializing with approximate progress.
   * The progress continues on from the progress reported by the `connecting` state.
   * @return progress object with current step and total steps.
   */
  public abstract Progress initializing();

  /**
   * Configuring with approximate progress.
   * The progress continues on from the progress reported by the `initializing` state.
   * @return progress object with current step and total steps.
   */
  public abstract Progress configuring();

  // Note this is not a terminal state - the stream may bo back to disconnected, and then subsequently reconnect again.

  /**
   * returns the connected tag ready to use.
   * Note this is not a terminal state - the stream may ho back to disconnected, and then
   * subsequently reconnect again.
   * @return a connected tag ready to use.
   */
  public abstract ConnectedJacquardTag connected();

  /** This terminal state will only be reached if reconnecting or retrying is not possible. */
  public abstract JacquardError disconnected();

  public static ConnectionState ofPreparingToConnect() {
    return AutoOneOf_ConnectionState.preparingToConnect();
  }

  public static ConnectionState ofConnecting(Progress progress) {
    return AutoOneOf_ConnectionState.connecting(progress);
  }

  public static ConnectionState ofInitializing(Progress progress) {
    return AutoOneOf_ConnectionState.initializing(progress);
  }

  public static ConnectionState ofConfiguring(Progress progress) {
    return AutoOneOf_ConnectionState.configuring(progress);
  }

  public static ConnectionState ofConnected(ConnectedJacquardTag tag) {
    return AutoOneOf_ConnectionState.connected(tag);
  }

  public static ConnectionState ofDisconnected(JacquardError error) {
    return AutoOneOf_ConnectionState.disconnected(error);
  }

}
