package deep.dark.lonelybrewer.dataapi;

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

/**
 * Created by Dima on 2015/03/24.
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

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.DataApi.addListener(googleApiClient, DataApiHelper.this);
        updateConfigDataItemOnStartup();
    }

    @Override
    public void onConnectionSuspended(int i) {}

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

    public interface DataHelperInterface {
        public boolean updateUiForKey (String configKey, int configValue);
        public void onUiUpdated ();
    }
}