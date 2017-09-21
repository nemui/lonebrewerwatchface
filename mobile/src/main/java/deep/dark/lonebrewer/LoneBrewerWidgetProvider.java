package deep.dark.lonebrewer;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;

public class LoneBrewerWidgetProvider extends AppWidgetProvider {

    @SuppressWarnings("unused")
    private static final String TAG = LoneBrewerWidgetProvider.class.getSimpleName();

    private static Intent service;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        startService(context);
    }

    @Override
    public void onEnabled (Context context) {
        super.onEnabled(context);
        startService(context);
    }

    private void startService (Context context) {
        if (service == null) {
            service = new Intent(context.getApplicationContext(), LoneBrewerWidgetService.class);
            context.startService(service);
        }
    }

    @Override
    public void onDisabled (Context context) {
        if (service != null) {
            context.stopService(service);
            service = null;
        }
        super.onDisabled(context);
    }
}
