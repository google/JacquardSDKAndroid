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

import androidx.core.util.Pair;
import com.google.android.jacquard.sdk.model.JacquardError;
import com.google.android.jacquard.sdk.model.Peripheral;
import com.google.android.jacquard.sdk.pairing.RequiredCharacteristics;
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag;
import com.google.auto.value.AutoOneOf;

/** The events used internally in {@link TagConnectionStateMachine}. */
@AutoOneOf(ConnectionEvent.Type.class)
public abstract class ConnectionEvent {

  public static ConnectionEvent ofConnectionError(JacquardError error) {
    return AutoOneOf_ConnectionEvent.connectionError(error);
  }

  public static ConnectionEvent ofConnectionProgress() {
    return AutoOneOf_ConnectionEvent.connectionProgress();
  }

  public static ConnectionEvent ofInitializationProgress() {
    return AutoOneOf_ConnectionEvent.initializationProgress();
  }

  public static ConnectionEvent ofTagPaired(
      Pair<Peripheral, RequiredCharacteristics> pair) {
    return AutoOneOf_ConnectionEvent.tagPaired(pair);
  }

  public static ConnectionEvent ofTagInitialized(ConnectedJacquardTag tag) {
    return AutoOneOf_ConnectionEvent.tagInitialized(tag);
  }

  public static ConnectionEvent ofTagConfigured(ConnectedJacquardTag tag) {
    return AutoOneOf_ConnectionEvent.tagConfigured(tag);
  }

  public static ConnectionEvent ofTagDisconnected(JacquardError error) {
    return AutoOneOf_ConnectionEvent.tagDisconnected(error);
  }

  public abstract Type getType();

  public abstract JacquardError connectionError();

  public abstract void connectionProgress();

  public abstract void initializationProgress();

  public abstract Pair<Peripheral, RequiredCharacteristics> tagPaired();

  public abstract ConnectedJacquardTag tagInitialized();

  public abstract ConnectedJacquardTag tagConfigured();

  public abstract JacquardError tagDisconnected();

  public enum Type {
    CONNECTION_ERROR,
    CONNECTION_PROGRESS,
    INITIALIZATION_PROGRESS,
    TAG_PAIRED,
    TAG_INITIALIZED,
    TAG_CONFIGURED,
    TAG_DISCONNECTED
  }
}
