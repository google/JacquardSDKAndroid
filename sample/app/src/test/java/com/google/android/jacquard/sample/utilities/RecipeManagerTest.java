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

package com.google.android.jacquard.sample.utilities;

import android.content.Context;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/**
 * Unit tests for {@link RecipeManager}.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = {Build.VERSION_CODES.P}, manifest = Config.NONE)
public class RecipeManagerTest {

    private RecipeManager recipeManager;
    private Gson gson;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        PrintLogger.initialize(context);
        recipeManager = new RecipeManager(context, getGson());
    }

    private Gson getGson() {
        if (gson == null) {
            gson = new GsonBuilder().create();
        }
        return gson;
    }


    @Test
    public void testAddRecipe() {
        List<Recipe> allRecipes = recipeManager.getAllRecipes();
        assertThat(allRecipes).isEmpty();

        recipeManager.addRecipe("ability_id", 1);

        assertThat(recipeManager.getAllRecipes().size()).isEqualTo(1);
    }

    @Test
    public void testRemoveRecipe() {
        recipeManager.addRecipe("ability_id", 1);

        recipeManager.removeRecipe(1);

        assertThat(recipeManager.getAllRecipes().size()).isEqualTo(0);
    }


    @Test
    public void testAbilityAssinged() {
        recipeManager.addRecipe("ability_id", 1);
        assertThat(recipeManager.isAbilityAssigned("ability_id")).isTrue();
        assertThat(recipeManager.isAbilityAssigned("ability_id_1")).isFalse();
    }


    @Test
    public void testGestureAssinged() {
        recipeManager.addRecipe("ability_id", 1);
        assertThat(recipeManager.isGestureAssigned(1)).isTrue();
        assertThat(recipeManager.isGestureAssigned(0)).isFalse();
    }
}