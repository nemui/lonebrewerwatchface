package deep.dark.lonebrewer;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.IBinder;
import android.widget.RemoteViews;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import deep.dark.lonebrewercommon.Brewery;


public class LoneBrewerWidgetService extends Service {

    @SuppressWarnings("unused")
    private static final String TAG = LoneBrewerWidgetService.class.getSimpleName();

    private static final int SIZE = 320;

    private Timer timer;
    private Bitmap bitmap;
    private Canvas canvas;
    private Brewery brewery;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_SCREEN_ON)) {
                startTimer();
            }
            else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                stopTimer();
            }
        }
    };

    private final BroadcastReceiver timeZoneReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            brewery.onTimeZoneChanged(intent);
        }
    };

    @Override
    public int onStartCommand (Intent intent, int flags, int startId) {

        bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);

        brewery = new Brewery(this);
        brewery.setSize(SIZE, SIZE);

        registerReceivers();

        startTimer();

        return super.onStartCommand(intent,flags,startId);
    }

    private void startTimer () {
        if (timer != null) {
            stopTimer();
        }
        timer = new Timer();
        timer.scheduleAtFixedRate(new UpdateWidget(this), 0, TimeUnit.SECONDS.toMillis(1));
    }

    private void stopTimer () {
        timer.cancel();
        timer = null;
    }

    private class UpdateWidget extends TimerTask {

        private Context context;
        private ComponentName thisWidget;
        private AppWidgetManager manager;

        UpdateWidget (Context context) {
            this.context = context;
            thisWidget = new ComponentName(context, LoneBrewerWidgetProvider.class);
            manager = AppWidgetManager.getInstance(context);
        }

        public void run() {
            manager.updateAppWidget(thisWidget, buildUpdate(context));
        }
    }

    public RemoteViews buildUpdate (Context context) {
        brewery.draw(canvas);

        RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.widget);
        updateViews.setImageViewBitmap(R.id.imageView, bitmap);
        return updateViews;
    }

    @Override
    public IBinder onBind (Intent intent) {
        // We don't need to bind to this service
        return null;
    }

    @Override
    public void onDestroy () {
        unregisterReceivers();
        stopTimer();
        super.onDestroy();
    }

    private void registerReceivers () {
        IntentFilter screenStateFilter = new IntentFilter();
        screenStateFilter.addAction(Intent.ACTION_SCREEN_ON);
        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(broadcastReceiver, screenStateFilter);

        IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
        registerReceiver(timeZoneReceiver, filter);
    }

    private void unregisterReceivers () {
        unregisterReceiver(broadcastReceiver);
        unregisterReceiver(timeZoneReceiver);
    }
}
