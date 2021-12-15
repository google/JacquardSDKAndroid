/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.jacquard.sample.scan

import androidx.annotation.StringRes
import com.google.android.jacquard.sample.KnownTag
import com.google.auto.value.AutoOneOf

/** Class for holding items to be displayed in the scanning list.  */
@AutoOneOf(AdapterItem.Type::class)
abstract class AdapterItem(var isItemSelected: Boolean = false) {
  abstract fun getType(): Type
  abstract fun tag(): KnownTag

  @StringRes
  abstract fun sectionHeader(): Int
  enum class Type {
    TAG, SECTION_HEADER
  }

  companion object {
    //TODO(b/207442538) Remove JVM annotation after all java classes are refactored to KT.
    @JvmStatic
    fun ofTag(tag: KnownTag): AdapterItem {
      return AutoOneOf_AdapterItem.tag(tag)
    }

    fun ofSectionHeader(@StringRes title: Int): AdapterItem {
      return AutoOneOf_AdapterItem.sectionHeader(title)
    }
  }
}