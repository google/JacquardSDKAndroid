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

package com.google.android.jacquard.sample;

import static com.google.android.jacquard.sample.Preferences.KEY.CURRENT_IMU_SESSION;
import static com.google.android.jacquard.sample.Preferences.KEY.CURRENT_TAG;
import static com.google.android.jacquard.sample.Preferences.KEY.FIRST_RUN;
import static com.google.android.jacquard.sample.Preferences.KEY.GEAR_LED_STATE;
import static com.google.android.jacquard.sample.Preferences.KEY.IS_GESTURE_LOADED;
import static com.google.android.jacquard.sample.Preferences.KEY.IS_HOME_LOADED;
import static com.google.android.jacquard.sample.Preferences.KEY.KNOWN_TAGS;
import static com.google.android.jacquard.sample.Preferences.KEY.RECIPES;
import static com.google.android.jacquard.sample.Preferences.KEY.TAG_LED_STATE;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.android.jacquard.sample.utilities.Recipe;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Key Value based storage based on {@link SharedPreferences}. */
public class Preferences {

  private static final String EMPTY_JSON_OBJECT = "";

  enum KEY {
    RECIPES,
    KNOWN_TAGS,
    FIRST_RUN,
    IS_HOME_LOADED,
    CURRENT_TAG,
    IS_GESTURE_LOADED,
    TAG_LED_STATE,
    GEAR_LED_STATE,
    CURRENT_IMU_SESSION
  }

  private static final String EMPTY_JSON_LIST = "[]";
  private final SharedPreferences sharedPref;
  private final Gson gson;
  public Preferences(Context context, Gson gson) {
    sharedPref =
        context.getSharedPreferences(
            "com.google.android.jacquard.sample.preferences", Context.MODE_PRIVATE);
    this.gson = gson;
  }

  /** Returns all gesture recipe list. */
  public List<Recipe> getRecipes() {
    Type listType = new TypeToken<ArrayList<Recipe>>() {}.getType();
    String json = sharedPref.getString(RECIPES.toString(), EMPTY_JSON_LIST);
    return gson.fromJson(json, listType);
  }

  /** Add new recipe to the existing recipe list. */
  public void setRecipes(List<Recipe>  recipes) {
    String json = gson.toJson(recipes);
    sharedPref.edit().putString(RECIPES.toString(), json).apply();
  }

  /** Returns known tags. */
  public List<KnownTag> getKnownTags() {
    Type listType = new TypeToken<ArrayList<KnownTag>>() {}.getType();
    String json = sharedPref.getString(KNOWN_TAGS.toString(), EMPTY_JSON_LIST);
    return gson.fromJson(json, listType);
  }

  /** Persists the provided list of known tags. */
  public void putKnownDevices(Collection<KnownTag> knownTags) {
    Set<KnownTag> set = new HashSet<>(knownTags);
    String json = gson.toJson(set);
    sharedPref.edit().putString(KNOWN_TAGS.toString(), json).apply();
  }

  public boolean isFirstRun() {
    return sharedPref.getBoolean(FIRST_RUN.toString(), true);
  }

  public void setFirstRun(boolean firstRun) {
    sharedPref.edit().putBoolean(FIRST_RUN.toString(), firstRun).apply();
  }

  /** Returns true if home was loaded before. */
  public boolean isHomeLoaded() {
    return sharedPref.getBoolean(IS_HOME_LOADED.toString(), false);
  }

  /** Returns boolean for home load. */
  public void setHomeLoaded(boolean homeLoaded) {
    sharedPref.edit().putBoolean(IS_HOME_LOADED.toString(), homeLoaded).apply();
  }

  /** Returns boolean for gesture screen load. */
  public void setGestureLoaded(boolean gestureLoaded) {
    sharedPref.edit().putBoolean(IS_GESTURE_LOADED.toString(), gestureLoaded).apply();
  }

  /** Returns true if Gesture screen was loaded before. */
  public boolean isGestureLoaded() {
    return sharedPref.getBoolean(IS_GESTURE_LOADED.toString(), false);
  }

  /** Returns a current tag. */
  public KnownTag getCurrentTag() {
    Type type = new TypeToken<KnownTag>() {
    }.getType();
    String json = sharedPref.getString(CURRENT_TAG.toString(), EMPTY_JSON_OBJECT);
    return gson.fromJson(json, type);
  }

  /**
   * Persists the provided current tag.
   *
   * @param knownTag current tag
   */
  public void putCurrentDevice(KnownTag knownTag) {
    String json = gson.toJson(knownTag);
    sharedPref.edit().putString(CURRENT_TAG.toString(), json).apply();
  }

  /** Removes the current tag from preference. */
  public void removeCurrentDevice() {
    sharedPref.edit().remove(CURRENT_TAG.toString()).apply();
  }

  /**
   * Persists tag led state whether active or not.
   *
   * @param isActive true if tag led control is active
   */
  public void persistTagLedState(boolean isActive) {
    sharedPref.edit().putBoolean(TAG_LED_STATE.toString(), isActive).apply();
  }

  /**
   * Returns true if tag led is active.
   */
  public boolean isTagLedActive() {
    return sharedPref.getBoolean(TAG_LED_STATE.toString(), true);
  }

  /**
   * Persists gear led state.
   *
   * @param isActive true if gear led control is active
   */
  public void persistGearLedState(boolean isActive) {
    sharedPref.edit().putBoolean(GEAR_LED_STATE.toString(), isActive).apply();
  }

  /**
   * Returns true if gear led is active.
   */
  public boolean isGearLedActive() {
    return sharedPref.getBoolean(GEAR_LED_STATE.toString(), false);
  }

  /**
   * Saves current Imu session id.
   */
  public void setCurrentImuSession(String id, String serial) {
    sharedPref.edit().putString(CURRENT_IMU_SESSION.name() + serial, id).commit();
  }

  /**
   * Returns current imu session id.
   */
  public String getCurrentImuSessionId(String serial) {
    return sharedPref.getString(CURRENT_IMU_SESSION.name() + serial, "0");
  }
}
