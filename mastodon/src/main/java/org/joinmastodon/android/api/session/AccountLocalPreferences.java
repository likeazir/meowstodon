package org.joinmastodon.android.api.session;


import static org.joinmastodon.android.api.MastodonAPIController.gson;

import android.content.SharedPreferences;

import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.model.Emoji;


import java.lang.reflect.Type;
import java.util.ArrayList;



public class AccountLocalPreferences{
	private final SharedPreferences prefs;

	public boolean serverSideFiltersSupported;
	public ArrayList<Emoji> recentCustomEmoji;
	private final static Type recentCustomEmojiType=new TypeToken<ArrayList<Emoji>>() {}.getType();

	public AccountLocalPreferences(SharedPreferences prefs){
		this.prefs=prefs;
		serverSideFiltersSupported=prefs.getBoolean("serverSideFilters", false);
		recentCustomEmoji=fromJson(prefs.getString("recentCustomEmoji", null), recentCustomEmojiType, new ArrayList<>());
	}

	public long getNotificationsPauseEndTime(){
		return prefs.getLong("notificationsPauseTime", 0L);
	}

	public void setNotificationsPauseEndTime(long time){
		prefs.edit().putLong("notificationsPauseTime", time).apply();
	}

	public void save(){
		prefs.edit()
				.putBoolean("serverSideFilters", serverSideFiltersSupported)
				.putString("recentCustomEmoji", gson.toJson(recentCustomEmoji))
				.apply();
	}

	private static <T> T fromJson(String json, Type type, T orElse){
		if(json==null) return orElse;
		try{
			T value=gson.fromJson(json, type);
			return value==null ? orElse : value;
		}catch(JsonSyntaxException ignored){
			return orElse;
		}
	}
}
