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

import com.google.android.jacquard.sdk.rx.Signal;

/**
 * Common interface for all state machine implementations.
 * @param <State> the type of state emitted from the state machine.
 */
public interface StateMachine<State, Event> {

  /**
   * Returns a signal for observing the state of the state mashing.
   * @return signal of with the current state.
   */
  Signal<State> getState();

  /**
   * Call to react to events emitted.
   * @param state the state emitted.
   */
  void onStateEvent(Event state);

  /** Releases all allocated resources. */
  void destroy();

}
