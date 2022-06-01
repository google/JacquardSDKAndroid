/*
 * Copyright 2021 Google LLC
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
package com.google.android.jacquard.sample

import android.content.Context
import android.content.SharedPreferences
import com.google.android.jacquard.sample.utilities.Recipe
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

/** Key Value based storage based on [SharedPreferences].  */
class Preferences(context: Context, private val gson: Gson) {

    companion object {
        private const val EMPTY_JSON_OBJECT = ""
        private const val EMPTY_JSON_LIST = "[]"
    }

    internal enum class KEY {
        RECIPES, KNOWN_TAGS, FIRST_RUN, IS_HOME_LOADED, CURRENT_TAG, IS_GESTURE_LOADED,
        TAG_LED_STATE, GEAR_LED_STATE, DFU_STATE, ALMOST_READY_SHOWN, AUTO_UPDATE, DFU_IN_PROGRESS
    }

    private val sharedPref: SharedPreferences = context.getSharedPreferences(
        "com.google.android.jacquard.sample.preferences", Context.MODE_PRIVATE
    )

    /** Returns all gesture recipe list.  */
    /** Add new recipe to the existing recipe list.  */
    var recipes: MutableList<Recipe>
        get() {
            val listType = object : TypeToken<ArrayList<Recipe>>() {}.type
            val json = sharedPref.getString(KEY.RECIPES.toString(), EMPTY_JSON_LIST)
            return gson.fromJson(json, listType)
        }
        set(recipes) {
            val json = gson.toJson(recipes)
            sharedPref.edit().putString(KEY.RECIPES.toString(), json).apply()
        }

    /** Returns known tags.  */
    val knownTags: MutableList<KnownTag>
        get() {
            val listType = object : TypeToken<ArrayList<KnownTag>>() {}.type
            val json = sharedPref.getString(KEY.KNOWN_TAGS.toString(), EMPTY_JSON_LIST)
            return gson.fromJson(json, listType)
        }

    /** Persists the provided list of known tags.  */
    fun putKnownDevices(knownTags: Collection<KnownTag>) {
        val set = HashSet(knownTags)
        val json = gson.toJson(set)
        sharedPref.edit().putString(KEY.KNOWN_TAGS.toString(), json).apply()
    }

    var isFirstRun: Boolean
        get() = sharedPref.getBoolean(KEY.FIRST_RUN.toString(), true)
        set(firstRun) {
            sharedPref.edit().putBoolean(KEY.FIRST_RUN.toString(), firstRun).apply()
        }
    /** Returns true if home was loaded before.  */
    /** Returns boolean for home load.  */
    var isHomeLoaded: Boolean
        get() = sharedPref.getBoolean(KEY.IS_HOME_LOADED.toString(), false)
        set(homeLoaded) {
            sharedPref.edit().putBoolean(KEY.IS_HOME_LOADED.toString(), homeLoaded).apply()
        }
    /** Returns true if Gesture screen was loaded before.  */
    /** Returns boolean for gesture screen load.  */
    var isGestureLoaded: Boolean
        get() = sharedPref.getBoolean(KEY.IS_GESTURE_LOADED.toString(), false)
        set(gestureLoaded) {
            sharedPref.edit().putBoolean(KEY.IS_GESTURE_LOADED.toString(), gestureLoaded).apply()
        }

    /** Returns a current tag.  */
    val currentTag: KnownTag?
        get() {
            val type = object : TypeToken<KnownTag>() {}.type
            val json = sharedPref.getString(KEY.CURRENT_TAG.toString(), EMPTY_JSON_OBJECT)
            return gson.fromJson(json, type)
        }

    /**
     * Persists the provided current tag.
     *
     * @param knownTag current tag
     */
    fun putCurrentDevice(knownTag: KnownTag) {
        val json = gson.toJson(knownTag)
        sharedPref.edit().putString(KEY.CURRENT_TAG.toString(), json).apply()
    }

    /** Removes the current tag from preference.  */
    fun removeCurrentDevice() {
        sharedPref.edit().remove(KEY.CURRENT_TAG.toString()).apply()
    }

    /**
     * Persists tag led state whether active or not.
     *
     * @param isActive true if tag led control is active
     */
    fun persistTagLedState(isActive: Boolean) {
        sharedPref.edit().putBoolean(KEY.TAG_LED_STATE.toString(), isActive).apply()
    }

    /** Returns true if tag led is active.  */
    val isTagLedActive: Boolean
        get() = sharedPref.getBoolean(KEY.TAG_LED_STATE.toString(), true)

    /**
     * Persists gear led state.
     *
     * @param isActive true if gear led control is active
     */
    fun persistGearLedState(isActive: Boolean) {
        sharedPref.edit().putBoolean(KEY.GEAR_LED_STATE.toString(), isActive).apply()
    }

    /** Returns true if gear led is active.  */
    val isGearLedActive: Boolean
        get() = sharedPref.getBoolean(KEY.GEAR_LED_STATE.toString(), false)

    fun autoUpdateFlag(autoUpdate: Boolean) {
        sharedPref.edit().putBoolean(KEY.AUTO_UPDATE.toString(), autoUpdate).apply()
    }

    val isAutoUpdate: Boolean
        get() = sharedPref.getBoolean(KEY.AUTO_UPDATE.toString(), false)

    fun removeAutoUpdateFlag() {
        sharedPref.edit().remove(KEY.AUTO_UPDATE.toString()).apply()
    }

    fun flagDfuInProgress() {
        sharedPref.edit().putBoolean(KEY.DFU_IN_PROGRESS.toString(), true).apply()
    }

    val isDfuInProgress: Boolean
        get() = sharedPref.getBoolean(KEY.DFU_IN_PROGRESS.toString(), false)

    fun removeFlagDfuInProgress() {
        sharedPref.edit().remove(KEY.DFU_IN_PROGRESS.toString()).apply()
    }
}