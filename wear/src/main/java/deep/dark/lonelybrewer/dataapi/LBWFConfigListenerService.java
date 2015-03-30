package deep.dark.lonelybrewer.dataapi;

import android.os.Bundle;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import java.util.concurrent.TimeUnit;

import deep.dark.lonelybrewer.dataapi.LBWFUtil;

public class LBWFConfigListenerService extends WearableListenerService
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "LBWFConfigListenerService";

    private GoogleApiClient mGoogleApiClient;

    @Override // WearableListenerService
    public void onMessageReceived(MessageEvent messageEvent) {
        if (!messageEvent.getPath().equals(LBWFUtil.PATH_WITH_FEATURE)) {
            return;
        }
        byte[] rawData = messageEvent.getData();
        // It's allowed that the message carries only some of the keys used in the config DataItem
        // and skips the ones that we don't want to change.
        DataMap configKeysToOverwrite = DataMap.fromByteArray(rawData);

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this).addApi(Wearable.API).build();
        }
        if (!mGoogleApiClient.isConnected()) {
            ConnectionResult connectionResult =
                    mGoogleApiClient.blockingConnect(30, TimeUnit.SECONDS);

            if (!connectionResult.isSuccess()) {
                return;
            }
        }

        LBWFUtil.overwriteKeysInConfigDataMap(mGoogleApiClient, configKeysToOverwrite);
    }

    @Override // GoogleApiClient.ConnectionCallbacks
    public void onConnected(Bundle connectionHint) {}

    @Override  // GoogleApiClient.ConnectionCallbacks
    public void onConnectionSuspended(int cause) {}

    @Override  // GoogleApiClient.OnConnectionFailedListener
    public void onConnectionFailed(ConnectionResult result) {}
}
