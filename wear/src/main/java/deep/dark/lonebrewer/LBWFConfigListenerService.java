package deep.dark.lonebrewer;

import android.os.Bundle;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import java.util.concurrent.TimeUnit;
import deep.dark.lonebrewercommon.LBWFUtil;

public class LBWFConfigListenerService extends WearableListenerService
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "LBWFConfigListenerService";

    private GoogleApiClient googleApiClient;

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (!messageEvent.getPath().equals(LBWFUtil.PATH_WITH_FEATURE)) {
            return;
        }
        byte[] rawData = messageEvent.getData();

        DataMap configKeysToOverwrite = DataMap.fromByteArray(rawData);

        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this).addApi(Wearable.API).build();
        }
        if (!googleApiClient.isConnected()) {
            ConnectionResult connectionResult =
                    googleApiClient.blockingConnect(30, TimeUnit.SECONDS);

            if (!connectionResult.isSuccess()) {
                return;
            }
        }

        LBWFUtil.overwriteKeysInConfigDataMap(googleApiClient, configKeysToOverwrite);
    }

    @Override
    public void onConnected(Bundle connectionHint) {}

    @Override
    public void onConnectionSuspended(int cause) {}

    @Override
    public void onConnectionFailed(ConnectionResult result) {}
}
