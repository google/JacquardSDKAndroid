/*
 * Copyright 2021 Google LLC. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.jacquard.sample.gesture

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.google.android.jacquard.sample.R

/** Set of supported gestures used by [GestureInfoAdapter].  */
enum class GestureInfo(
    @get:DrawableRes
    @param:DrawableRes val imgResId: Int, @get:StringRes
    @param:StringRes val titleResId: Int, @get:StringRes
    @param:StringRes val subtitleResId: Int
) {
    BRUSH_IN(
        R.drawable.ic_brush_in,
        R.string.gesture_info_brush_in_title,
        R.string.gesture_info_brush_in_subtitle
    ),
    BRUSH_OUT(
        R.drawable.ic_brush_out,
        R.string.gesture_info_brush_out_title,
        R.string.gesture_info_brush_out_subtitle
    ),
    DOUBLE_TAP(
        R.drawable.ic_double_tap,
        R.string.gesture_info_double_tap_title,
        R.string.gesture_info_double_tap_subtitle
    ),
    COVER(
        R.drawable.ic_cover, R.string.gesture_info_cover_title, R.string.gesture_info_cover_subtitle
    );

}