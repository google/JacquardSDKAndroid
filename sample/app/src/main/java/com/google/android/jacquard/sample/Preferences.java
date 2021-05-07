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

import static com.google.android.jacquard.sample.Preferences.KEY.CURRENT_TAG;
import static com.google.android.jacquard.sample.Preferences.KEY.FIRST_RUN;
import static com.google.android.jacquard.sample.Preferences.KEY.IS_GESTURE_LOADED;
import static com.google.android.jacquard.sample.Preferences.KEY.IS_HOME_LOADED;
import static com.google.android.jacquard.sample.Preferences.KEY.KNOWN_TAGS;

import android.content.Context;
import android.content.SharedPreferences;
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
    KNOWN_TAGS,
    FIRST_RUN,
    IS_HOME_LOADED,
    CURRENT_TAG,
    IS_GESTURE_LOADED
  }

  private static final String EMPTY_JSON_LIST = "[]";
  private final SharedPreferences sharedPref;
  private final Gson gson;
  public Preferences(Context context, Gson gson) {
    sharedPref =
        context.getSharedPreferences(
            context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
    this.gson = gson;
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
}
