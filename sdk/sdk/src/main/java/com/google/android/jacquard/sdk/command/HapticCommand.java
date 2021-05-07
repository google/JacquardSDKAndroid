package com.google.android.jacquard.sdk.command;

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

import com.google.android.jacquard.sdk.connection.CommandResponseStatus;
import com.google.android.jacquard.sdk.connection.Result;
import com.google.android.jacquard.sdk.model.Component;
import com.google.atap.jacquard.protocol.JacquardProtocol.Domain;
import com.google.atap.jacquard.protocol.JacquardProtocol.HapticRequest;
import com.google.atap.jacquard.protocol.JacquardProtocol.HapticSymbol;
import com.google.atap.jacquard.protocol.JacquardProtocol.HapticSymbolType;
import com.google.atap.jacquard.protocol.JacquardProtocol.Opcode;
import com.google.atap.jacquard.protocol.JacquardProtocol.Request;
import com.google.atap.jacquard.protocol.JacquardProtocol.Response;
import com.google.atap.jacquard.protocol.JacquardProtocol.Status;
import com.google.auto.value.AutoValue;

/**
 * A class which forms haptic command request and plays on gear component.
 */
public class HapticCommand implements CommandRequest<Boolean> {

  /**
   * A frame which plays the haptic pattern on gear component.
   */
  @AutoValue
  public static abstract class Frame {

    public abstract int onMs();

    public abstract int offMs();

    public abstract Pattern pattern();

    public abstract int maxAmplitudePercent();

    public abstract int repeatNminusOne();

    public static Builder builder() {
      return new AutoValue_HapticCommand_Frame.Builder();
    }

    /**
     * Builder class for {@link Frame}.
     */
    @AutoValue.Builder
    public abstract static class Builder {

      /**
       * Sets {@code onMs} duration to play haptics on gear component.
       *
       * @param onMs play time duration for haptic
       */
      public abstract Builder setOnMs(int onMs);

      /**
       * Sets {@code offMs} duration for haptics on gear component.
       *
       * @param offMs off time duration for haptic
       */
      public abstract Builder setOffMs(int offMs);

      /**
       * Sets haptic pattern {@link Pattern} which plays on gear component.
       *
       * @param pattern haptic pattern type plays on gear component
       */
      public abstract Builder setPattern(Pattern pattern);

      /**
       * Sets {@code maxAmplitudePercent} of haptic pattern to play on gear component.
       *
       * @param maxAmplitudePercent maximum amplitude in percentage for haptic pattern
       */
      public abstract Builder setMaxAmplitudePercent(int maxAmplitudePercent);

      /**
       * Sets repetition of haptic pattern to play on gear component.
       *
       * @param repeatNminusOne repetition number for haptic pattern
       */
      public abstract Builder setRepeatNminusOne(int repeatNminusOne);

      /**
       * Returns haptic frame {@link Frame} which plays on gear component.
       */
      public abstract Frame build();
    }
  }

  /**
   * A enum which contains HapticSymbolType pattern.
   */
  public enum Pattern {
    HAPTIC_SYMBOL_HALTED,
    HAPTIC_SYMBOL_SINE_INCREASE,
    HAPTIC_SYMBOL_SINE_DECREASE,
    HAPTIC_SYMBOL_LINEAR_INCREASE,
    HAPTIC_SYMBOL_LINEAR_DECREASE,
    HAPTIC_SYMBOL_PARABOLIC_INCREASE,
    HAPTIC_SYMBOL_PARABOLIC_DECREASE,
    HAPTIC_SYMBOL_CONST_ON,
  }

  private final Frame frame;
  private final Component component;

  /**
   * Creates a new instance of HapticCommand and when executed play the frames on the component.
   *
   * @param frame     the haptic pattern to play.
   * @param component the gear component plays the command.
   */
  public HapticCommand(Frame frame, Component component) {
    this.frame = frame;
    this.component = component;
  }

  @Override
  public Result<Boolean> parseResponse(Response response) {
    if (response.getStatus() != Status.STATUS_OK) {
      Throwable error = CommandResponseStatus.from(response.getStatus().getNumber());
      return Result.ofFailure(error);
    }
    return Result.ofSuccess(true);
  }

  @Override
  public Request getRequest() {
    HapticRequest.Builder patternBuilder = HapticRequest.newBuilder();
    HapticSymbol jqFrame = HapticSymbol.newBuilder().setOnMs(frame.onMs()).setOffMs(frame.offMs())
        .setPattern(getHapticSymbolType(frame.pattern()))
        .setMaxAmplitudePercent(frame.maxAmplitudePercent())
        .setRepeatNMinusOne(frame.repeatNminusOne()).build();
    patternBuilder.setFrames(jqFrame);

    HapticRequest hapticRequest = patternBuilder.build();

    return getBaseRequestBuilder()
        .setExtension(HapticRequest.haptic, hapticRequest).build();
  }

  private HapticSymbolType getHapticSymbolType(Pattern type) {
    return HapticSymbolType.internalGetValueMap().findValueByNumber(type.ordinal());
  }

  private Request.Builder getBaseRequestBuilder() {
    return Request
        .newBuilder()
        .setId(0)
        .setDomain(Domain.GEAR)
        .setOpcode(Opcode.GEAR_HAPTIC)
        .setComponentId(component.componentId());
  }

}
