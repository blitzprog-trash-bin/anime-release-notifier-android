package com.freezingwind.animereleasenotifier.controller;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class AnimeUpdater {
	protected ArrayList<Anime> animeList;

	public AnimeUpdater() {
		animeList = new ArrayList<Anime>();
	}

	// GetAnimeList
	public ArrayList<Anime> getAnimeList() {
		return animeList;
	}

	// Update
	public void update(String userName, final Context context, final AnimeListUpdateCallBack callBack) {
		String apiUrl = "https://animereleasenotifier.com/api/animelist/" + userName;

		//Toast.makeText(activity, "Loading anime list of " + userName, Toast.LENGTH_SHORT).show();

		final JsonObjectRequest jsObjRequest = new JsonObjectRequest
			(Request.Method.GET, apiUrl, null, new Response.Listener<JSONObject>() {
				@Override
				public void onResponse(JSONObject response) {
					try {
						animeList.clear();

						SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
						SharedPreferences.Editor editor = sharedPrefs.edit();

						JSONArray watchingList = response.getJSONArray("watching");
						for (int i = 0; i < watchingList.length(); i++) {
							JSONObject animeJSON = watchingList.getJSONObject(i);
							JSONObject episodes = animeJSON.getJSONObject("episodes");
							JSONObject animeProvider = animeJSON.getJSONObject("animeProvider");

							Anime anime = new Anime(
								animeJSON.getString("title"),
								animeJSON.getString("image"),
								animeJSON.getString("url"),
								animeProvider.getString("url"),
								animeProvider.getString("nextEpisodeUrl"),
								animeProvider.getString("videoUrl"),
								episodes.getInt("watched"),
								episodes.getInt("available"),
								episodes.getInt("max"),
								episodes.getInt("offset")
							);

							// Load cached episode count
							String key = anime.title + ":episodes-available";
							int availableCached = sharedPrefs.getInt(key, -1);

							anime.notify = anime.available > availableCached && availableCached != -1;

							// Save data in preferences
							editor.putInt(anime.title + ":episodes-available", anime.available);

							// Add to list
							animeList.add(anime);
						}

						// Write preferences
						editor.apply();
					} catch (JSONException e) {
						System.out.println("Error parsing JSON: " + e.toString());
					} finally {
						callBack.execute();
					}
				}
			}, new Response.ErrorListener() {

				@Override
				public void onErrorResponse(VolleyError error) {
					System.out.println("Error: " + error.toString());
				}
			}
			);

		MyVolley.getRequestQueue().add(jsObjRequest);
	}
}