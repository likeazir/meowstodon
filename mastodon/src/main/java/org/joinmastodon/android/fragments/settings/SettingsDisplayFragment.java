package org.joinmastodon.android.fragments.settings;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import org.joinmastodon.android.E;
import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.MastodonApp;
import org.joinmastodon.android.R;
import org.joinmastodon.android.events.StatusDisplaySettingsChangedEvent;
import org.joinmastodon.android.model.viewmodel.CheckableListItem;
import org.joinmastodon.android.model.viewmodel.ListItem;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import me.grishka.appkit.FragmentStackActivity;

public class SettingsDisplayFragment extends BaseSettingsFragment<Void>{
	private ImageView themeTransitionWindowView;
	private ListItem<Void> themeItem;
	private CheckableListItem<Void> showCWsItem, hideSensitiveMediaItem, interactionCountsItem, emojiInNamesItem, dynamicColorsItem;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setTitle(R.string.settings_display);
		List<ListItem<Void>> items=new ArrayList<>();
		items.add(themeItem=new ListItem<>(R.string.settings_theme, getAppearanceValue(), R.drawable.ic_dark_mode_24px, this::onAppearanceClick));
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S){
			items.add(dynamicColorsItem=new CheckableListItem<>(R.string.settings_use_dynamic_colors, 0, CheckableListItem.Style.SWITCH, GlobalUserPreferences.useDynamicColors, R.drawable.ic_palette_24px, item->{
				toggleCheckableItem(item);
				setUseDynamicColors(item.checked);
			}));
			dynamicColorsItem.checkedChangeListener=this::setUseDynamicColors;
		}
		items.add(showCWsItem=new CheckableListItem<>(R.string.settings_show_cws, 0, CheckableListItem.Style.SWITCH, GlobalUserPreferences.showCWs, R.drawable.ic_warning_24px, this::toggleCheckableItem));
		items.add(hideSensitiveMediaItem=new CheckableListItem<>(R.string.settings_hide_sensitive_media, 0, CheckableListItem.Style.SWITCH, GlobalUserPreferences.hideSensitiveMedia, R.drawable.ic_no_adult_content_24px, this::toggleCheckableItem));
		items.add(interactionCountsItem=new CheckableListItem<>(R.string.settings_show_interaction_counts, 0, CheckableListItem.Style.SWITCH, GlobalUserPreferences.showInteractionCounts, R.drawable.ic_social_leaderboard_24px, this::toggleCheckableItem));
		items.add(emojiInNamesItem=new CheckableListItem<>(R.string.settings_show_emoji_in_names, 0, CheckableListItem.Style.SWITCH, GlobalUserPreferences.customEmojiInNames, R.drawable.ic_emoticon_24px, this::toggleCheckableItem));
		onDataLoaded(items);
	}

	@Override
	protected void doLoadData(int offset, int count){}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		if(themeTransitionWindowView!=null){
			// Activity has finished recreating. Remove the overlay.
			activity.getSystemService(WindowManager.class).removeView(themeTransitionWindowView);
			themeTransitionWindowView=null;
		}
	}

	@Override
	protected void onHidden(){
		super.onHidden();
		GlobalUserPreferences.showCWs=showCWsItem.checked;
		GlobalUserPreferences.hideSensitiveMedia=hideSensitiveMediaItem.checked;
		GlobalUserPreferences.showInteractionCounts=interactionCountsItem.checked;
		GlobalUserPreferences.customEmojiInNames=emojiInNamesItem.checked;
		GlobalUserPreferences.save();
		E.post(new StatusDisplaySettingsChangedEvent(accountID));
	}

	private int getAppearanceValue(){
		return switch(GlobalUserPreferences.theme){
			case AUTO -> R.string.theme_auto;
			case LIGHT -> R.string.theme_light;
			case DARK -> R.string.theme_dark;
			case NEONPINK -> R.string.theme_neon;
		};
	}

	private void onAppearanceClick(ListItem<?> item_){
		int selected=switch(GlobalUserPreferences.theme){
			case LIGHT -> 0;
			case DARK -> 1;
			case AUTO -> 2;
			case NEONPINK -> 3;
		};
		new M3AlertDialogBuilder(getActivity())
				.setTitle(R.string.settings_theme)
				.setSingleChoiceItems((String[])IntStream.of(R.string.theme_light, R.string.theme_dark, R.string.theme_auto, R.string.theme_neon).mapToObj(this::getString).toArray(String[]::new),
						selected, (dlg, item)->{
							GlobalUserPreferences.ThemePreference pref=switch(item){
								case 0 -> GlobalUserPreferences.ThemePreference.LIGHT;
								case 1 -> GlobalUserPreferences.ThemePreference.DARK;
								case 2 -> GlobalUserPreferences.ThemePreference.AUTO;
								case 3 -> GlobalUserPreferences.ThemePreference.NEONPINK;
								default -> throw new IllegalStateException("Unexpected value: "+item);
							};
							if(pref!=GlobalUserPreferences.theme){
								GlobalUserPreferences.ThemePreference prev=GlobalUserPreferences.theme;
								GlobalUserPreferences.theme=pref;
								GlobalUserPreferences.save();
								themeItem.subtitleRes=getAppearanceValue();
								rebindItem(themeItem);
								maybeApplyNewThemeRightNow(prev);
							}
							dlg.dismiss();
						})
				.show();
	}

	private void setUseDynamicColors(boolean useDynamicColors){
		dynamicColorsItem.checked=useDynamicColors;
		GlobalUserPreferences.useDynamicColors=useDynamicColors;
		GlobalUserPreferences.save();
		restartActivityToApplyNewTheme();
	}

	private void maybeApplyNewThemeRightNow(GlobalUserPreferences.ThemePreference prev){
		boolean isCurrentDark=prev==GlobalUserPreferences.ThemePreference.DARK ||
				(prev==GlobalUserPreferences.ThemePreference.AUTO && Build.VERSION.SDK_INT>=30 && getResources().getConfiguration().isNightModeActive());
		boolean isNewDark=GlobalUserPreferences.theme==GlobalUserPreferences.ThemePreference.DARK ||
				(GlobalUserPreferences.theme==GlobalUserPreferences.ThemePreference.AUTO && Build.VERSION.SDK_INT>=30 && getResources().getConfiguration().isNightModeActive());
		if(isCurrentDark!=isNewDark){
			restartActivityToApplyNewTheme();
		}
	}

	private void restartActivityToApplyNewTheme(){
		// Calling activity.recreate() causes a black screen for like half a second.
		// So, let's take a screenshot and overlay it on top to create the illusion of a smoother transition.
		// As a bonus, we can fade it out to make it even smoother.
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N && Build.VERSION.SDK_INT<Build.VERSION_CODES.S){
			View activityDecorView=getActivity().getWindow().getDecorView();
			Bitmap bitmap=Bitmap.createBitmap(activityDecorView.getWidth(), activityDecorView.getHeight(), Bitmap.Config.ARGB_8888);
			activityDecorView.draw(new Canvas(bitmap));
			themeTransitionWindowView=new ImageView(MastodonApp.context);
			themeTransitionWindowView.setImageBitmap(bitmap);
			WindowManager.LayoutParams lp=new WindowManager.LayoutParams(WindowManager.LayoutParams.TYPE_APPLICATION);
			lp.flags=WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
					WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;
			lp.systemUiVisibility=View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
			lp.systemUiVisibility|=(activityDecorView.getWindowSystemUiVisibility() & (View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR));
			lp.width=lp.height=WindowManager.LayoutParams.MATCH_PARENT;
			lp.token=getActivity().getWindow().getAttributes().token;
			lp.windowAnimations=R.style.window_fade_out;
			MastodonApp.context.getSystemService(WindowManager.class).addView(themeTransitionWindowView, lp);
		}
		getActivity().recreate();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		((FragmentStackActivity)getActivity()).invalidateSystemBarColors(this);
	}
}
