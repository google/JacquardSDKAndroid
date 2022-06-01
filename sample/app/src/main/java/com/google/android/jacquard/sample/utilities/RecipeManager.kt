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
package com.google.android.jacquard.sample.utilities

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.google.android.jacquard.sample.Preferences
import com.google.android.jacquard.sample.SampleApplication
import com.google.android.jacquard.sdk.log.PrintLogger
import com.google.gson.Gson

/**
 * Manager to add/remove/fetch ability-gesture (i.e. recipe).
 */
class RecipeManager {

    val preferences: Preferences

    private constructor(context: Context) {
        preferences = (context.applicationContext as SampleApplication)
            .resourceLocator.preferences
    }

    @VisibleForTesting
    constructor(context: Context, gson: Gson) {
        preferences = Preferences(context, gson)
    }

    companion object {
        private val TAG: String = RecipeManager::class.java.simpleName

        @Volatile
        private var instance: RecipeManager? = null

        @JvmStatic
        fun getInstance(context: Context): RecipeManager {
            return instance ?: synchronized(this) {
                instance ?: RecipeManager(context).also { instance = it }
            }
        }
    }

    fun getAllRecipes(): List<Recipe> {
        return preferences.recipes
    }

    fun addRecipe(abilityId: String, gestureId: Int) {
        val recipes = preferences.recipes
        recipes.add(Recipe(abilityId, gestureId))
        preferences.recipes = recipes
    }

    fun removeRecipe(gesturedId: Int) {
        val recipes = preferences.recipes
        val iterator = recipes.iterator()
        while (iterator.hasNext()) {
            val (_, gestureId) = iterator.next()
            if (gestureId == gesturedId) {
                iterator.remove()
                PrintLogger.i(TAG, "recipe removed for gesture: $gesturedId")
                preferences.recipes = recipes
                break
            }
        }
    }

    fun isAbilityAssigned(abilityId: String): Boolean {
        val recipes = preferences.recipes
        for ((abilityId1) in recipes) {
            if (abilityId1 == abilityId) {
                return true
            }
        }
        return false
    }

    fun isGestureAssigned(gestureId: Int): Boolean {
        val recipes = preferences.recipes
        for ((_, gestureId1) in recipes) {
            if (gestureId1 == gestureId) {
                return true
            }
        }
        return false
    }

    fun getRecipeForAbility(abilityId: String): Recipe? {
        val recipes = preferences.recipes
        for (recipe in recipes) {
            if (recipe.abilityId == abilityId) {
                return recipe
            }
        }
        return null
    }

}