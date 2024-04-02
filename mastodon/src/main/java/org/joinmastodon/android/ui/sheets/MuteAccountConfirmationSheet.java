package org.joinmastodon.android.ui.sheets;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.ui.views.M3Switch;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.NonNull;

public class MuteAccountConfirmationSheet extends AccountRestrictionConfirmationSheet{
	public MuteAccountConfirmationSheet(@NonNull Context context, Account user, AtomicReference<Duration> muteDuration, AtomicBoolean muteNotifications, ConfirmCallback confirmCallback){
		super(context, user, confirmCallback);
		titleView.setText(R.string.mute_user_confirm_title);
		confirmBtn.setText(R.string.do_mute);
		secondaryBtn.setVisibility(View.GONE);
		icon.setImageResource(R.drawable.ic_fluent_speaker_off_24_regular);
		subtitleView.setText(user.getDisplayUsername());
		addRow(R.drawable.ic_campaign_24px, R.string.user_wont_know_muted);
		addRow(R.drawable.ic_fluent_eye_off_24_regular, R.string.user_can_still_see_your_posts);
		addRow(R.drawable.ic_fluent_mention_24_regular, R.string.you_wont_see_user_mentions);
		addRow(R.drawable.ic_fluent_arrow_reply_24_regular, R.string.user_can_mention_and_follow_you);

		// add mute notifications toggle (Moshidon)
		M3Switch m3Switch=new M3Switch(getContext());
		m3Switch.setClickable(true);
		m3Switch.setChecked(muteNotifications.get());
		m3Switch.setOnCheckedChangeListener((compoundButton, b) -> muteNotifications.set(b));
		m3Switch.setOnClickListener(view -> muteNotifications.set(m3Switch.isSelected()));
		addRow(R.drawable.ic_fluent_alert_off_24_regular, R.string.mo_mute_notifications, m3Switch);

		// add mute duration (Moshidon)
		Button button=new Button(getContext());
		PopupMenu popupMenu=getMuteDurationPopupMenu(context, muteDuration, button);
		button.setOnClickListener(v->popupMenu.show());
		button.setOnTouchListener(popupMenu.getDragToOpenListener());
		button.setText(popupMenu.getMenu().getItem(0).getTitle());

		addRow(R.drawable.ic_fluent_clock_20_regular, R.string.sk_mute_label, button);
	}

	@NonNull
	private PopupMenu getMuteDurationPopupMenu(@NonNull Context context, AtomicReference<Duration> muteDuration, Button button){
		PopupMenu popupMenu=new PopupMenu(context, button, Gravity.CENTER_HORIZONTAL);
		popupMenu.inflate(R.menu.mute_duration);
		popupMenu.setOnMenuItemClickListener(item->{
			int id=item.getItemId();
			if(id==R.id.duration_indefinite)
				muteDuration.set(Duration.ZERO);
			else if(id==R.id.duration_minutes_5){
				muteDuration.set(Duration.ofMinutes(5));
			}else if(id==R.id.duration_minutes_30){
				muteDuration.set(Duration.ofMinutes(30));
			}else if(id==R.id.duration_hours_1){
				muteDuration.set(Duration.ofHours(1));
			}else if(id==R.id.duration_hours_6){
				muteDuration.set(Duration.ofHours(6));
			}else if(id==R.id.duration_days_1){
				muteDuration.set(Duration.ofDays(1));
			}else if(id==R.id.duration_days_3){
				muteDuration.set(Duration.ofDays(3));
			}else if(id==R.id.duration_days_7){
				muteDuration.set(Duration.ofDays(7));
			}
			button.setText(item.getTitle());
			return true;
		});
		return popupMenu;
	}
}
