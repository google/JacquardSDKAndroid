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
 */

package com.google.android.jacquard.sample.haptics

import com.google.android.jacquard.sdk.command.HapticCommand
import com.google.android.jacquard.sdk.command.HapticCommand.Frame

/** A set of predefined haptic patterns with plays on the gear component. */
enum class HapticPatternType(val description: String,
                             val onMs: Int,
                             val offMs: Int,
                             val pattern: HapticCommand.Pattern,
                             val maxAmplitudePercent: Int,
                             val repeatNminusOne: Int) {
    INSERT_PATTERN(/* description= */"Tag Insertion Pattern",
            /* onMs= */200,
            /* offMs= */0,
            HapticCommand.Pattern.HAPTIC_SYMBOL_SINE_INCREASE,
            /* maxAmplitudePercent= */60,
            /* repeatNminusOne= */0),
    GESTURE_PATTERN(
            /* description= */ "Gesture Pattern",
            /* onMs= */ 170,
            /* offMs= */ 0,
            HapticCommand.Pattern.HAPTIC_SYMBOL_SINE_INCREASE,
            /* maxAmplitudePercent= */ 60,
            /* repeatNminusOne= */ 0),
    NOTIFICATION_PATTERN(
            /* description= */ "Notification Pattern",
            /* onMs= */ 170,
            /* offMs= */ 30,
            HapticCommand.Pattern.HAPTIC_SYMBOL_SINE_INCREASE,
            /* maxAmplitudePercent= */ 60,
            /* repeatNminusOne= */ 1),
    ERROR_PATTERN(
            /* description= */ "Error Pattern",
            /* onMs= */ 170,
            /* offMs= */ 50,
            HapticCommand.Pattern.HAPTIC_SYMBOL_SINE_INCREASE,
            /* maxAmplitudePercent= */ 60,
            /* repeatNminusOne= */ 3),
    ALERT_PATTERN(
            /* description= */ "Alert Pattern",
            /* onMs= */ 170,
            /* offMs= */ 700,
            HapticCommand.Pattern.HAPTIC_SYMBOL_SINE_INCREASE,
            /* maxAmplitudePercent= */ 60,
            /* repeatNminusOne= */ 14),
    STOP_PATTERN(
            /* description= */ "Stop Pattern",
            /* onMs= */ 0,
            /* offMs= */ 0,
            HapticCommand.Pattern.HAPTIC_SYMBOL_HALTED,
            /* maxAmplitudePercent= */ 0,
            /* repeatNminusOne= */ 0);

    /** Returns frame [Frame] which play on gear component.  */
    open fun getFrame(): Frame {
        return Frame.builder()
                .setOnMs(onMs)
                .setOffMs(offMs)
                .setPattern(pattern)
                .setMaxAmplitudePercent(maxAmplitudePercent)
                .setRepeatNminusOne(repeatNminusOne)
                .build()
    }

}