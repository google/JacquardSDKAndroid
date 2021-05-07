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
package com.google.android.jacquard.sdk.initialization;

import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag;
import com.google.auto.value.AutoOneOf;

/** The states of {@link ProtocolInitializationStateMachine}. */
@AutoOneOf(InitializationState.Type.class)
public abstract class InitializationState {

  public enum Type {
    PAIRED,
    HELLO_SENT,
    BEGIN_SENT,
    COMPONENT_INFO_SENT,
    CREATING_TAG_INSTANCE,
    TAG_INITIALIZED,
    ERROR
  }

  public boolean isTerminal() {
    return getType() == Type.TAG_INITIALIZED || getType() == Type.ERROR;
  }

  public boolean isType(Type type) {
    return getType() == type;
  }

  public abstract Type getType();

  public abstract void paired();

  public abstract void helloSent();

  public abstract void beginSent();

  public abstract void componentInfoSent();

  public abstract void creatingTagInstance();

  public abstract ConnectedJacquardTag tagInitialized();

  public abstract Throwable error();

  public static InitializationState ofPaired() {
    return AutoOneOf_InitializationState.paired();
  }

  public static InitializationState ofError(Throwable throwable) {
    return AutoOneOf_InitializationState.error(throwable);
  }

  public static InitializationState ofHelloSent() {
    return AutoOneOf_InitializationState.helloSent();
  }

  public static InitializationState ofBeginSent() {
    return AutoOneOf_InitializationState.beginSent();
  }

  public static InitializationState ofComponentInfoSent() {
    return AutoOneOf_InitializationState.componentInfoSent();
  }

  public static InitializationState ofTagInitialized(ConnectedJacquardTag tag) {
    return AutoOneOf_InitializationState.tagInitialized(tag);
  }

  public static InitializationState ofCreatingTagInstance() {
    return AutoOneOf_InitializationState.creatingTagInstance();
  }
}
