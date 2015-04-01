package deep.dark.lonebrewercommon;

import android.graphics.Color;
import android.net.Uri;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

public final class LBWFUtil {
    private static final String TAG = "LBWFUtil";

    public static final String KEY_TIME_MODE = "TIME_MODE", KEY_HEMISPHERE = "HEMISPHERE",
            KEY_WEEKDAYS = "WEEKDAYS", KEY_BARRELS_COLOUR = "BARREL_COLOUR",
            KEY_CROPS_COLOUR = "CROPS_COLOUR", KEY_MAGMA_COLOUR = "MAGMA_COLOUR";

    public static final String PATH_WITH_FEATURE = "/watch_face_config/LoneBrewerWatchFace";

    public static final int IS_12H_MODE_ON_DEFAULT = 1,
            HEMISPHERE_NORTHERN = 0, HEMISPHERE_SOUTHERN = 1,
            HEMISPHERE_DEFAULT = HEMISPHERE_NORTHERN,
            ARE_WEEKDAYS_ON_DEFAULT = 1, BARRELS_COLOUR_DEFAULT = Color.YELLOW,
            CROPS_COLOUR_DEFAULT = Color.YELLOW, MAGMA_COLOUR_DEFAULT = Color.RED;

    public interface FetchConfigDataMapCallback {
        void onConfigDataMapFetched(DataMap config);
    }

    public static void fetchConfigDataMap(final GoogleApiClient client,
            final FetchConfigDataMapCallback callback) {
        Wearable.NodeApi.getLocalNode(client).setResultCallback(
                new ResultCallback<NodeApi.GetLocalNodeResult>() {
                    @Override
                    public void onResult(NodeApi.GetLocalNodeResult getLocalNodeResult) {
                        String localNode = getLocalNodeResult.getNode().getId();
                        Uri uri = new Uri.Builder()
                                .scheme("wear")
                                .path(LBWFUtil.PATH_WITH_FEATURE)
                                .authority(localNode)
                                .build();
                        Wearable.DataApi.getDataItem(client, uri)
                                .setResultCallback(new DataItemResultCallback(callback));
                    }
                }
        );
    }

    public static void overwriteKeysInConfigDataMap(final GoogleApiClient googleApiClient,
            final DataMap configKeysToOverwrite) {

        LBWFUtil.fetchConfigDataMap(googleApiClient,
                new FetchConfigDataMapCallback() {
                    @Override
                    public void onConfigDataMapFetched(DataMap currentConfig) {
                        DataMap overwrittenConfig = new DataMap();
                        overwrittenConfig.putAll(currentConfig);
                        overwrittenConfig.putAll(configKeysToOverwrite);
                        LBWFUtil.putConfigDataItem(googleApiClient, overwrittenConfig);
                    }
                }
        );
    }

    public static void putConfigDataItem(GoogleApiClient googleApiClient, DataMap newConfig) {
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(PATH_WITH_FEATURE);
        DataMap configToPut = putDataMapRequest.getDataMap();
        configToPut.putAll(newConfig);
        Wearable.DataApi.putDataItem(googleApiClient, putDataMapRequest.asPutDataRequest())
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(DataApi.DataItemResult dataItemResult) {}
                });
    }

    private static class DataItemResultCallback implements ResultCallback<DataApi.DataItemResult> {

        private final FetchConfigDataMapCallback mCallback;

        public DataItemResultCallback(FetchConfigDataMapCallback callback) {
            mCallback = callback;
        }

        @Override
        public void onResult(DataApi.DataItemResult dataItemResult) {
            if (dataItemResult.getStatus().isSuccess()) {
                if (dataItemResult.getDataItem() != null) {
                    DataItem configDataItem = dataItemResult.getDataItem();
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(configDataItem);
                    DataMap config = dataMapItem.getDataMap();
                    mCallback.onConfigDataMapFetched(config);
                } else {
                    mCallback.onConfigDataMapFetched(new DataMap());
                }
            }
        }
    }

    public static void setDefaultValuesForMissingConfigKeys(DataMap config) {
        if (!config.containsKey(KEY_TIME_MODE)){
            config.putInt(KEY_TIME_MODE, IS_12H_MODE_ON_DEFAULT);
        }
        if (!config.containsKey(KEY_HEMISPHERE)){
            config.putInt(KEY_HEMISPHERE, HEMISPHERE_DEFAULT);
        }
        if (!config.containsKey(KEY_WEEKDAYS)){
            config.putInt(KEY_WEEKDAYS, ARE_WEEKDAYS_ON_DEFAULT);
        }
        if (!config.containsKey(KEY_BARRELS_COLOUR)){
            config.putInt(KEY_BARRELS_COLOUR, BARRELS_COLOUR_DEFAULT);
        }
        if (!config.containsKey(KEY_CROPS_COLOUR)){
            config.putInt(KEY_CROPS_COLOUR, CROPS_COLOUR_DEFAULT);
        }
        if (!config.containsKey(KEY_MAGMA_COLOUR)){
            config.putInt(KEY_MAGMA_COLOUR, MAGMA_COLOUR_DEFAULT);
        }
    }
}
