package com.android.systemui.qs.tiles;

import static com.android.internal.logging.MetricsLogger.VIEW_UNKNOWN;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.service.quicksettings.Tile;
import androidx.annotation.Nullable;

import com.android.internal.logging.MetricsLogger;
import com.android.systemui.animation.Expandable;
import com.android.systemui.dagger.qualifiers.Background;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.plugins.qs.QSTile.Icon;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.QsEventLogger;
import com.android.systemui.qs.logging.QSLogger;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.res.R;

import javax.inject.Inject;

public class SoundTile extends QSTileImpl<BooleanState> {
    public static final String TILE_SPEC = "sound";

    private final Icon mIconNormal  = ResourceIcon.get(R.drawable.ic_qs_sound);
    private final Icon mIconVibrate = ResourceIcon.get(R.drawable.ic_qs_sound_vibrate);
    private final Icon mIconSilent  = ResourceIcon.get(R.drawable.ic_qs_sound_silent);

    private final AudioManager mAudioManager;
    private final BroadcastReceiver mReceiver;
    private boolean mListening = false;

    @Inject
    public SoundTile(
            QSHost host,
            QsEventLogger uiEventLogger,
            @Background Looper backgroundLooper,
            @Main Handler mainHandler,
            FalsingManager falsingManager,
            MetricsLogger metricsLogger,
            StatusBarStateController statusBarStateController,
            ActivityStarter activityStarter,
            QSLogger qsLogger
    ) {
        super(host, uiEventLogger, backgroundLooper, mainHandler, falsingManager, metricsLogger,
                statusBarStateController, activityStarter, qsLogger);
        mAudioManager = mContext.getSystemService(AudioManager.class);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                refreshState();
            }
        };
    }

    @Override public BooleanState newTileState() { return new BooleanState(); }

    @Override
    public void handleSetListening(boolean listening) {
        if (mAudioManager == null) return;
        if (mListening == listening) return;
        mListening = listening;
        if (listening) {
            final IntentFilter filter = new IntentFilter();
            filter.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);
            filter.addAction(AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION);
            mContext.registerReceiver(mReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            mContext.unregisterReceiver(mReceiver);
        }
    }

    @Override
    protected void handleDestroy() {
        super.handleDestroy();
        if (mListening) { mContext.unregisterReceiver(mReceiver); mListening = false; }
    }

    @Override
    protected void handleClick(@Nullable Expandable expandable) {
        if (mAudioManager != null)
            mAudioManager.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI);
    }

    @Override
    public Intent getLongClickIntent() {
        return new Intent(android.provider.Settings.ACTION_SOUND_SETTINGS);
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_sound_tile_label);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        state.label = mContext.getString(R.string.quick_settings_sound_tile_label);
        state.hasLongClickEffect = true;
        if (mAudioManager != null) {
            int ringerMode = mAudioManager.getRingerModeInternal();
            boolean isSoundOn = (ringerMode == AudioManager.RINGER_MODE_NORMAL);
            state.value = isSoundOn;
            state.state = isSoundOn ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE;
            if (isSoundOn) {
                state.icon = mIconNormal;
                state.secondaryLabel = mContext.getString(R.string.switch_bar_on);
            } else if (ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
                state.icon = mIconVibrate;
                state.secondaryLabel = mContext.getString(R.string.quick_settings_sound_tile_vibrate);
            } else {
                state.icon = mIconSilent;
                state.secondaryLabel = mContext.getString(R.string.quick_settings_sound_tile_silent);
            }
        } else {
            state.value = false;
            state.state = Tile.STATE_INACTIVE;
            state.icon = mIconSilent;
            state.secondaryLabel = mContext.getString(R.string.switch_bar_off);
        }
    }

    @Override public int getMetricsCategory() { return VIEW_UNKNOWN; }
}
