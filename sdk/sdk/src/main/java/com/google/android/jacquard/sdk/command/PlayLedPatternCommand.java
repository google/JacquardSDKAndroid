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
package com.google.android.jacquard.sdk.command;

import com.google.android.jacquard.sdk.connection.Result;
import com.google.android.jacquard.sdk.model.Component;
import com.google.android.jacquard.sdk.util.StringUtils;
import com.google.atap.jacquard.protocol.JacquardProtocol;
import com.google.atap.jacquard.protocol.JacquardProtocol.Domain;
import com.google.atap.jacquard.protocol.JacquardProtocol.LedPatternFrames;
import com.google.atap.jacquard.protocol.JacquardProtocol.LedPatternRequest;
import com.google.atap.jacquard.protocol.JacquardProtocol.Opcode;
import com.google.atap.jacquard.protocol.JacquardProtocol.PatternPlayType;
import com.google.atap.jacquard.protocol.JacquardProtocol.PatternType;
import com.google.atap.jacquard.protocol.JacquardProtocol.Request;
import com.google.auto.value.AutoValue;

import java.util.List;

/**
 * Command for playing a list of {@link Frame} on a component.
 * <p>
 * A command can be send to a connected tag via
 * {@link com.google.android.jacquard.sdk.tag.ConnectedJacquardTag#enqueue(ProtoCommandRequest)}
 */
public class PlayLedPatternCommand extends ProtoCommandRequest<Boolean> {

  private final List<Frame> frames;
  private final int durationInMs;
  private final int intensityLevel;
  private final Component component;
  private final PatternType patternType;
  private final PatternPlayType patternPlayType;
  private final boolean resumable;
  private final boolean haltAll;
  private final StringUtils stringUtils;

  /**
   * Creates a new instance of PlayLedPatternCommand and when executed play the frames on the
   * component.
   */
  private PlayLedPatternCommand(PlayLedPatternCommandBuilder commandBuilder) {
    this.frames = commandBuilder.frames;
    this.durationInMs = commandBuilder.durationInMs;
    this.intensityLevel=commandBuilder.intensityLevel;
    this.component = commandBuilder.component;
    this.patternType = getLedPatternType(commandBuilder.ledPatternType);
    this.patternPlayType = getPlayType(commandBuilder.playType);
    this.resumable = commandBuilder.resumable;
    this.haltAll = commandBuilder.haltAll;
    this.stringUtils = commandBuilder.stringUtils;
  }

  @Override
  public Result<Boolean> parseResponse(byte[] respByte) {
    return Result.ofSuccess(true);
  }

  @Override
  public Request getRequest() {
    LedPatternRequest.Builder patternBuilder = LedPatternRequest.newBuilder();
    for (Frame frame : frames) {
      JacquardProtocol.Color jqColor = JacquardProtocol.Color.newBuilder()
          .setBlue(frame.color().blue()).setGreen(frame.color().green()).setRed(frame.color().red())
          .build();

      LedPatternFrames jqFrame = LedPatternFrames.newBuilder().setColor(jqColor)
          .setLengthMs(frame.durationInMs()).build();
      patternBuilder.addFrames(jqFrame);
    }

    LedPatternRequest ledPatternRequest = patternBuilder
        .setDurationMs(durationInMs)
        .setPatternType(patternType)
        .setPlayPauseToggle(patternPlayType)
        .setResumable(resumable)
        .setHaltAll(haltAll)
        .setIntensityLevel(intensityLevel)
        .build();

    return getBaseRequestBuilder()
        .setExtension(LedPatternRequest.ledPatternRequest, ledPatternRequest).build();
  }

  /**
   * Returns command request builder for this command.
   */
  public static PlayLedPatternCommandBuilder newBuilder(){
    return new PlayLedPatternCommandBuilder();
  }

  private Request.Builder getBaseRequestBuilder() {
    Request.Builder builder = Request
        .newBuilder()
        .setComponentId(0)
        .setId(getId());
    if (component.componentId() == Component.TAG_ID) {
      return builder.setDomain(Domain.BASE).setOpcode(Opcode.LED_PATTERN).setComponentId(
          Component.TAG_ID);
    } else {
      return builder.setDomain(Domain.GEAR).setOpcode(Opcode.GEAR_LED)
          .setComponentId(component.componentId());
    }
  }

  @AutoValue
  public abstract static class Color {

    public abstract int red();

    public abstract int green();

    public abstract int blue();

    public static Color of(int red, int green, int blue) {
      return new AutoValue_PlayLedPatternCommand_Color(red, green, blue);
    }
  }

  @AutoValue
  public static abstract class Frame {

    public abstract Color color();

    public abstract int durationInMs();

    public static Frame of(Color color, int durationInMs) {
      return new AutoValue_PlayLedPatternCommand_Frame(color, durationInMs);
    }
  }

  /**
   * Builder class for creating {@link PlayLedPatternCommand}.
   */
  public static class PlayLedPatternCommandBuilder {

    private List<Frame> frames;
    private int durationInMs;
    private Component component;
    private LedPatternType ledPatternType;
    private PlayType playType;
    private boolean resumable;
    private int intensityLevel;
    private boolean haltAll;
    private StringUtils stringUtils;

    /**
     * Sets frames for a given led pattern command.
     *
     * @param frames frames for given command.
     */
    public PlayLedPatternCommandBuilder setFrames(List<Frame> frames) {
      this.frames = frames;
      return this;
    }

    /**
     * Sets commands duration to play.
     *
     * @param durationInMs duration in milliseconds for playing command.
     */
    public PlayLedPatternCommandBuilder setDurationInMs(int durationInMs) {
      this.durationInMs = durationInMs;
      return this;
    }

    /**
     * Sets component on which command to be played.
     *
     * @param component component on which command to be played.
     */
    public PlayLedPatternCommandBuilder setComponent(Component component) {
      this.component = component;
      return this;
    }

    /**
     * Sets led pattern type for given led pattern command.
     *
     * @param ledPatternType led pattern type for a command.
     */
    public PlayLedPatternCommandBuilder setLedPatternType(LedPatternType ledPatternType) {
      this.ledPatternType = ledPatternType;
      return this;
    }

    /**
     * Sets play type for given led pattern command.
     *
     * @param playType play type for a command.
     */
    public PlayLedPatternCommandBuilder setPlayType(PlayType playType) {
      this.playType = playType;
      return this;
    }

    /**
     * Sets if pattern does not need to be resumable if halted.
     *
     * @param resumable true if pattern needs to be resumed upon halting else false.
     */
    public PlayLedPatternCommandBuilder setResumable(boolean resumable) {
      this.resumable = resumable;
      return this;
    }

    /**
     * Sets if pattern must resume upon interruption.
     *
     * @param haltAll true if pattern needs to be resumed upon interruption else false.
     */
    public PlayLedPatternCommandBuilder setHaltAll(boolean haltAll) {
      this.haltAll = haltAll;
      return this;
    }

    /**
     * Sets intensity level for given led pattern command.
     *
     * @param intensityLevel 0 (no LED) - 100 (Full intensity).
     */
    public PlayLedPatternCommandBuilder setIntensityLevel(int intensityLevel){
      this.intensityLevel=intensityLevel;
      return this;
    }

    public PlayLedPatternCommandBuilder setStringUtils(StringUtils stringUtilsInstance){
      this.stringUtils = stringUtilsInstance;
      return this;
    }

    /**
     * Returns builder for creating play led pattern command {@link PlayLedPatternCommand}.
     */
    public PlayLedPatternCommand build() {
      return new PlayLedPatternCommand(this);
    }
  }

  /**
   * An enum which contains LED pattern type.
   */
  public enum LedPatternType {
    /**
     * No led pattern is selected to play.
     */
    PATTERN_TYPE_NONE,
    /**
     * LED will play in solid pattern.
     */
    PATTERN_TYPE_SOLID,
    /**
     * LED will play in breathing pattern.
     */
    PATTERN_TYPE_BREATHING,
    /**
     * LED will play in pulse pattern.
     */
    PATTERN_TYPE_PULSING,
    /**
     * LED blinks single times.
     */
    PATTERN_TYPE_SINGLE_BLINK,
    /**
     * LED blinks two times.
     */
    PATTERN_TYPE_DOUBLE_BLINK,
    /**
     * LED blinks three times.
     */
    PATTERN_TYPE_TRIPPLE_BLINK,
    /**
     * LED will play in custom pattern.
     */
    PATTERN_TYPE_CUSTOM,
  }

  /**
   * An enum which contains LED pattern play type to play.
   */
  public enum PlayType {
    PLAY,
    TOGGLE,
  }

  private PatternType getLedPatternType(LedPatternType ledPatternType) {
    return PatternType.internalGetValueMap().findValueByNumber(ledPatternType.ordinal());
  }

  private PatternPlayType getPlayType(PlayType playType) {
    return PatternPlayType.internalGetValueMap().findValueByNumber(playType.ordinal());
  }
}
