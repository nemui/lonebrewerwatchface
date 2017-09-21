package deep.dark.lonebrewer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.WindowInsets;
import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

import deep.dark.lonebrewercommon.Brewery;

public class LoneBrewerWatchFace extends CanvasWatchFaceService {

    @SuppressWarnings("unused")
    private static final String TAG = "LoneBrewerWatchFace";

    // updating only once per second - efficient!
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class TimeHandler extends Handler {
        private final WeakReference<Engine> engineReference;

        TimeHandler(Engine engine) {
            engineReference = new WeakReference<>(engine);
        }
        @Override
        public void handleMessage(Message msg)
        {
            Engine engine = engineReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case Engine.MSG_UPDATE_TIME:
                        engine.invalidate();
                        if (engine.shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = INTERACTIVE_UPDATE_RATE_MS
                                    - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                            sendEmptyMessageDelayed(Engine.MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        static final int MSG_UPDATE_TIME = 0;

        private final TimeHandler updateTimeHandler = new TimeHandler(this);

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                brewery.onTimeZoneChanged(intent);
                invalidate();
            }
        };

        boolean registeredTimeZoneReceiver = false;
        // an attempt to get a bit of data api out of the way
        DataApiHelper dataApiHelper;

        Brewery brewery;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(LoneBrewerWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setStatusBarGravity(Gravity.TOP | Gravity.START)
                    .setHotwordIndicatorGravity(Gravity.TOP | Gravity.END)
                    .build());

            brewery = new Brewery(LoneBrewerWatchFace.this);

            dataApiHelper = new DataApiHelper(LoneBrewerWatchFace.this, new DataApiHelper.DataHelperInterface() {

                @Override
                public boolean updateUiForKey(String configKey, int configValue) {
                    return brewery.updateElementForKey(configKey, configValue);
                }

                @Override
                public void onUiUpdated() {
                    brewery.setDirty();
                    invalidate();
                }
            });
        }

        @Override
        public void onDestroy() {
            updateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);

            brewery.setIsInAmbientMode(inAmbientMode);
            invalidate();
            // whether the timer should be running depends on whether we're in ambient mode (as well
            // as whether we're visible), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);
            brewery.setRoundChinHeight(insets.isRound(), insets.getSystemWindowInsetBottom());
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            brewery.setSize(width, height);
            super.onSurfaceChanged(holder, format, width, height);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            brewery.draw(canvas);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                dataApiHelper.connect();
                registerReceiver();

                // update time zone in case it changed while we weren't visible.
                brewery.updateTimeZone();
            } else {
                unregisterReceiver();
                dataApiHelper.disconnect();
            }

            // whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (registeredTimeZoneReceiver) {
                return;
            }
            registeredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            LoneBrewerWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!registeredTimeZoneReceiver) {
                return;
            }
            registeredTimeZoneReceiver = false;
            LoneBrewerWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }


        // starts the updateTimeHandler timer if it should be running and isn't currently
        // stops it if it shouldn't be running but currently is.
        private void updateTimer() {
            updateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                updateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        // returns whether the updateTimeHandler timer should be running. The timer should
        // only run when we're visible and in interactive mode.
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }
    }
}