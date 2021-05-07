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

package com.google.android.jacquard.sample.utilities;

import android.content.Context;
import androidx.annotation.VisibleForTesting;
import com.google.android.jacquard.sample.Preferences;
import com.google.android.jacquard.sample.ResourceLocator;
import com.google.android.jacquard.sample.SampleApplication;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.gson.Gson;
import java.util.Iterator;
import java.util.List;

/**
 * Manager to add/remove/fetch ability-gesture (i.e. recipe).
 */
public class RecipeManager {

    private static final String TAG = RecipeManager.class.getSimpleName();
    private final Preferences preferences;
    private static RecipeManager instance;

    private RecipeManager(Context context) {
        ResourceLocator resourceLocator = ((SampleApplication) context.getApplicationContext())
                .getResourceLocator();
        preferences = resourceLocator.getPreferences();
    }

    @VisibleForTesting
    RecipeManager(Context context, Gson gson) {
        preferences = new Preferences(context, gson);
    }

    public static RecipeManager getInstance(Context context) {
        if (instance == null) {
            instance = new RecipeManager(context);
        }
        return instance;
    }

    public List<Recipe> getAllRecipes() {
        return preferences.getRecipes();
    }

    public void addRecipe(String abilityId, int gestureId) {
        List<Recipe> recipes = preferences.getRecipes();
        recipes.add(Recipe.of(abilityId, gestureId));
        preferences.setRecipes(recipes);
    }

    public void removeRecipe(int gesturedId) {
        List<Recipe> recipes = preferences.getRecipes();
        Iterator<Recipe> iterator = recipes.iterator();
        while (iterator.hasNext()) {
            Recipe recipe = iterator.next();
            if (recipe.gestureId() == gesturedId) {
                iterator.remove();
                PrintLogger.i(TAG, "recipe removed for gesture: " + gesturedId);
                preferences.setRecipes(recipes);
                break;
            }
        }

    }

    public boolean isAbilityAssigned(String abilityId) {
        List<Recipe> recipes = preferences.getRecipes();
        for (Recipe recipe : recipes) {
            if (recipe.abilityId().equals(abilityId)) {
                return true;
            }
        }
        return false;
    }

    public boolean isGestureAssigned(int gestureId) {
        List<Recipe> recipes = preferences.getRecipes();
        for (Recipe recipe : recipes) {
            if (recipe.gestureId() == gestureId) {
                return true;
            }
        }
        return false;
    }

    public Recipe getRecipeForAbility(String abilityId) {
        List<Recipe> recipes = preferences.getRecipes();
        for (Recipe recipe : recipes) {
            if (recipe.abilityId().equals(abilityId)) {
                return recipe;
            }
        }
        return null;
    }
}
