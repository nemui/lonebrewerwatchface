package deep.dark.lonebrewer;

import android.content.Context;
import android.os.Bundle;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import deep.dark.lonebrewercommon.LBWFUtil;

/*
    A feeble attempt to decouple DataApi from the rest of the stuff.
 */
public class DataApiHelper implements DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    GoogleApiClient googleApiClient;
    DataHelperInterface dataHelperListener;

    public DataApiHelper (Context context, DataHelperInterface dataHelperListener) {
        googleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();
        this.dataHelperListener = dataHelperListener;
    }

    // updates config and UI on startup
    private void updateConfigDataItemOnStartup() {
        LBWFUtil.fetchConfigDataMap(googleApiClient,
                new LBWFUtil.FetchConfigDataMapCallback() {
                    @Override
                    public void onConfigDataMapFetched(DataMap startupConfig) {
                        LBWFUtil.setDefaultValuesForMissingConfigKeys(startupConfig);
                        LBWFUtil.putConfigDataItem(googleApiClient, startupConfig);

                        updateUiForConfigDataMap(startupConfig);
                    }
                }
        );
    }

    public void connect () {
        googleApiClient.connect();
    }

    public void disconnect () {
        if (googleApiClient != null && googleApiClient.isConnected()) {
            Wearable.DataApi.removeListener(googleApiClient, this);
            googleApiClient.disconnect();
        }
    }

    // potentially updates UI for config
    // listener gets notified only if there is, in fact, an update
    private void updateUiForConfigDataMap(final DataMap config) {
        boolean uiUpdated = false;
        for (String configKey : config.keySet()) {
            if (!config.containsKey(configKey)) {
                continue;
            }
            if (dataHelperListener.updateUiForKey(configKey, config.getInt(configKey))) {
                uiUpdated = true;
            }
        }
        if (uiUpdated) {
            dataHelperListener.onUiUpdated();
        }
    }

    // sets new value in config
    public void updateConfigDataItem (final String configCode, final int configValue) {
        DataMap configKeysToOverwrite = new DataMap();
        configKeysToOverwrite.putInt(configCode, configValue);
        LBWFUtil.overwriteKeysInConfigDataMap(googleApiClient, configKeysToOverwrite);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.DataApi.addListener(googleApiClient, DataApiHelper.this);
        // after connecting successfully, we want to get current config values and update
        // UI accordingly
        updateConfigDataItemOnStartup();
    }

    @Override
    public void onConnectionSuspended(int i) {}

    // new config data has arrived
    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        try {
            for (DataEvent dataEvent : dataEvents) {
                if (dataEvent.getType() != DataEvent.TYPE_CHANGED) {
                    continue;
                }

                DataItem dataItem = dataEvent.getDataItem();
                if (!dataItem.getUri().getPath().equals(LBWFUtil.PATH_WITH_FEATURE)) {
                    continue;
                }

                DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
                DataMap config = dataMapItem.getDataMap();
                updateUiForConfigDataMap(config);
            }
        }
        finally {
            dataEvents.close();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {}

    // listener needs to be able to handle a given config key-value pair
    // (or ignore a given config key-value pair)
    // it also needs to be notified in case something gets updated
    public interface DataHelperInterface {
        public boolean updateUiForKey (String configKey, int configValue);
        public void onUiUpdated ();
    }
}