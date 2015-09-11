package team.jcandfriends.cookstogo.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import team.jcandfriends.cookstogo.Api;
import team.jcandfriends.cookstogo.JSONGrabber;

public class RecipeManager {

    /**
     * The Log tag
     */
    private static final String TAG = "RecipeManager";

    /**
     * The name of the SharedPreferences that contains the caches of this class
     */
    private static final String RECIPE_CACHE = "recipe_cache";

    /**
     * The key of the cached recipe types
     */
    private static final String PERSISTENT_RECIPE_TYPES = "persistent_recipe_types";

    /**
     * The prefix of each recipe that was cached
     */
    private static final String RECIPE_CACHE_PREFIX = "recipe";

    /**
     * Singleton instance
     */
    private static RecipeManager ourInstance;

    /**
     * The SharedPreferences that contains the caches of this class
     */
    private SharedPreferences preferences;

    private RecipeManager(Context context) {
        preferences = context.getSharedPreferences(RECIPE_CACHE, Context.MODE_PRIVATE);
    }

    /**
     * Returns the singleton instance
     *
     * @param context the context
     * @return the singleton instance
     */
    public static RecipeManager get(Context context) {
        if (ourInstance == null) {
            ourInstance = new RecipeManager(context);
        }
        return ourInstance;
    }

    /**
     * Checks the cache if the previously fetched recipe types were cached
     *
     * @return true if recipe types cache is not empty, false otherwise
     */
    public boolean hasCachedRecipeTypes() {
        return preferences.getAll().containsKey(PERSISTENT_RECIPE_TYPES);
    }

    /**
     * Caches the given JSONArray
     *
     * @param recipeTypes the JSONArray to cache
     */
    public void cacheRecipeTypes(JSONArray recipeTypes) {
        preferences.edit().putString(PERSISTENT_RECIPE_TYPES, recipeTypes.toString()).apply();
    }

    /**
     * Returns the cached recipe types
     *
     * @return the cached recipe types as JSONArray
     */
    public JSONArray getCachedRecipeTypes() {
        String recipeTypesAsString = preferences.getString(PERSISTENT_RECIPE_TYPES, "");
        try {
            return new JSONArray(recipeTypesAsString);
        } catch (JSONException e) {
            Log.e(TAG, "JSONException : The recipeTypesAsString can't be parsed as a valid JSONArray");
            e.printStackTrace();
            throw new RuntimeException("Cannot proceed anymore");
        }
    }

    /**
     * Removes the cached recipe types
     */
    public void clearCachedRecipeTypes() {
        preferences.edit().remove(PERSISTENT_RECIPE_TYPES).apply();
    }

    /**
     * Determines if the the recipe with the given id exists in the cache
     *
     * @param recipeId the id of the recipe to search
     * @return true if the recipe is found, false otherwise
     */
    public boolean hasCachedRecipe(int recipeId) {
        return preferences.getAll().containsKey(RECIPE_CACHE_PREFIX + recipeId);
    }

    /**
     * Caches a recipe
     *
     * @param recipe the recipe to cache
     */
    public void cacheRecipe(JSONObject recipe) {
        try {
            preferences.edit().putString(RECIPE_CACHE_PREFIX + recipe.getInt(Api.RECIPE_PK), recipe.toString()).apply();
        } catch (JSONException e) {
            Log.e(TAG, "JSONException : The passed JSONObject recipe doesn't contain a '" + Api.RECIPE_PK + "'");
            e.printStackTrace();
            throw new RuntimeException("Cannot proceed anymore");
        }
    }

    /**
     * Returns a JSONObject that contains the cached recipe
     *
     * @param recipeId the id of the recipe to get
     * @return the recipe
     */
    public JSONObject getCachedRecipe(int recipeId) {
        String recipeAsString = preferences.getString(RECIPE_CACHE_PREFIX + recipeId, "");
        try {
            return new JSONObject(recipeAsString);
        } catch (JSONException e) {
            Log.e(TAG, "JSONException : The recipeAsString can't be parsed as a valid JSONObject");
            e.printStackTrace();
            throw new RuntimeException("Cannot proceed anymore");
        }
    }

    /**
     * Removes a recipe from the cache
     *
     * @param recipeId the id of the recipe to remove
     */
    public void clearCachedRecipe(int recipeId) {
        preferences.edit().remove(RECIPE_CACHE_PREFIX + recipeId).apply();
    }

    public void fetch(final int recipeId, final Callbacks callbacks) {
        new AsyncTask<String, Void, JSONObject>() {
            @Override
            protected JSONObject doInBackground(String... params) {
                try {
                    JSONGrabber grabber = new JSONGrabber(Api.RECIPES + recipeId);
                    return grabber.grab();
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(JSONObject result) {
                super.onPostExecute(result);
                if (null == result) {
                    callbacks.onFailure();
                } else {
                    callbacks.onSuccess(result);
                }
            }
        }.execute();
    }

    /**
     * Starts an asynchronous request to the backend server that will return a resulting JSONObject
     * containing the count, next, previous, and the actual results which are the recipes that
     * matches the query.
     *
     * @param query     the query
     * @param callbacks the callbacks that will be invoked in success or failure event
     */
    public void search(String query, final Callbacks callbacks) {
        if (query == null || query.isEmpty()) {
            throw new IllegalArgumentException("query must not be null and empty");
        }

        final String url = Api.RECIPES + "?search=" + query;

        new AsyncTask<String, Void, JSONObject>() {
            @Override
            protected JSONObject doInBackground(String... params) {
                try {
                    JSONGrabber grabber = new JSONGrabber(url);
                    return grabber.grab();
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(JSONObject result) {
                super.onPostExecute(result);
                if (null == result) {
                    callbacks.onFailure();
                } else {
                    callbacks.onSuccess(result);
                }
            }
        }.execute();
    }

    /**
     * Starts an asynchronous request to the backend server that will return a resulting JSONObject
     * containing the count, next, previous, and the actual results which are the recipes that contain
     * ingredients from the given ingredients parameter.
     *
     * @param ingredients the list of ingredients
     * @param callbacks   the callbacks that will be invoked in success or failure event
     */
    public void recommendRecipes(ArrayList<JSONObject> ingredients, final Callbacks callbacks) {
        if (ingredients.size() < 1)
            throw new IllegalArgumentException("ingredients length < 1 : " + ingredients.size());

        StringBuilder sb = new StringBuilder();
        boolean first = true;

        sb.append(Api.RECIPES).append("?ingredients=");
        for (JSONObject ingredient : ingredients) {
            if (first) {
                sb.append(ingredient.optInt(Api.INGREDIENT_PK));
                first = false;
            } else {
                sb.append(",").append(ingredient.optInt(Api.INGREDIENT_PK));
            }
        }

        final String url = sb.toString();

        new AsyncTask<String, Void, JSONObject>() {
            @Override
            protected JSONObject doInBackground(String... params) {
                try {
                    JSONGrabber grabber = new JSONGrabber(url);
                    return grabber.grab();
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(JSONObject result) {
                super.onPostExecute(result);
                if (null == result) {
                    callbacks.onFailure();
                } else {
                    callbacks.onSuccess(result);
                }
            }
        }.execute();
    }

    public interface Callbacks {
        void onSuccess(JSONObject result);

        void onFailure();
    }
}
