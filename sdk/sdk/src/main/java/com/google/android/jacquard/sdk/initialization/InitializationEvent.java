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
package com.google.android.jacquard.sdk.initialization;

import com.google.android.jacquard.sdk.model.CharacteristicUpdate;
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag;
import com.google.atap.jacquard.protocol.JacquardProtocol.Response;
import com.google.auto.value.AutoOneOf;

/** The types of events that {@link ProtocolInitializationStateMachine} reacts to. */
@AutoOneOf(InitializationEvent.Type.class)
public abstract class InitializationEvent {

  public enum Type {
    START_NEGOTIATION,
    VALUE_WRITTEN,
    RECEIVED_RESPONSE,
    RECEIVED_RESPONSE_WITH_ERROR,
    CREATED_CONNECTED_TAG_INSTANCE,
  }

  public abstract Type getType();

  public abstract void startNegotiation();

  public abstract CharacteristicUpdate valueWritten();

  public abstract Response receivedResponse();

  public abstract Throwable receivedResponseWithError();

  public abstract ConnectedJacquardTag createdConnectedTagInstance();

  public static InitializationEvent ofCreatedConnectedTagInstance(ConnectedJacquardTag tag) {
    return AutoOneOf_InitializationEvent.createdConnectedTagInstance(tag);
  }

  public static InitializationEvent ofStartNegotiation() {
    return AutoOneOf_InitializationEvent.startNegotiation();
  }

  public static InitializationEvent ofReceivedResponse(Response response) {
    return AutoOneOf_InitializationEvent.receivedResponse(response);
  }

  public static InitializationEvent ofReceivedResponseWithError(Throwable throwable) {
    return AutoOneOf_InitializationEvent.receivedResponseWithError(throwable);
  }

  public static InitializationEvent ofValueWritten(CharacteristicUpdate characteristicUpdate) {
    return AutoOneOf_InitializationEvent.valueWritten(characteristicUpdate);
  }
}
