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

package com.google.android.jacquard.sdk.initialization;

/**
 * Fake implementation of {@link ProtocolInitializationStateMachine}.
 */
public class FakeProtocolInitializationStateMachine extends ProtocolInitializationStateMachine {

  private InitializationState initializationState;

  /**
   * Creates a new instance of ProtocolInitializationStateMachine.
   */
  public FakeProtocolInitializationStateMachine() {
    super(/* peripheral= */ null, /* requiredCharacteristics= */ null);
  }

  @Override
  public void startNegotiation() {
    getState().next(initializationState);
  }

  /**
   * Sets protocol initialization state.
   *
   * @param initializationState state of {@link ProtocolInitializationStateMachine}
   */
  public void setInitializationState(InitializationState initializationState) {
    this.initializationState = initializationState;
  }
}
