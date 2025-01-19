package org.joinmastodon.android.ui;

import android.app.Activity;

import org.joinmastodon.android.model.EmojiCategory;

import java.util.List;

public class CustomEmojiReactionPopupKeyboard extends CustomEmojiPopupKeyboard{
	public CustomEmojiReactionPopupKeyboard(Activity activity, String account, List<EmojiCategory> customEmojis, String session){
		super(activity, account, customEmojis, session, true);
	}
}
