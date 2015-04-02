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

/*
    DataApi-related class used both in companion and wearable configuration activities
 */
public final class LBWFUtil {
    private static final String TAG = "LBWFUtil";

    /**
     * The {@link DataMap} keys for LoneBrewerWatchFace configuration.
     */
    public static final String KEY_TIME_MODE = "TIME_MODE", KEY_HEMISPHERE = "HEMISPHERE",
            KEY_DAY_OF_THE_WEEK = "DAY_OF_THE_WEEK", KEY_BARRELS_COLOUR = "BARREL_COLOUR",
            KEY_CROPS_COLOUR = "CROPS_COLOUR", KEY_MAGMA_COLOUR = "MAGMA_COLOUR";

    /**
     * The path for the {@link DataItem} containing LoneBrewerWatchFace configuration.
     */
    public static final String PATH_WITH_FEATURE = "/watch_face_config/LoneBrewerWatchFace";

    // defaults
    public static final int IS_12H_MODE_ON_DEFAULT = 1,
            HEMISPHERE_NORTHERN = 0, HEMISPHERE_SOUTHERN = 1,
            HEMISPHERE_DEFAULT = HEMISPHERE_NORTHERN,
            IS_DAY_OF_THE_WEEK_ON_DEFAULT = 1, BARRELS_COLOUR_DEFAULT = Color.YELLOW,
            CROPS_COLOUR_DEFAULT = Color.YELLOW, MAGMA_COLOUR_DEFAULT = Color.RED;

    /**
     * Callback interface to perform an action with the current config {@link DataMap} for
     * LoneBrewerWatchFace.
     */
    public interface FetchConfigDataMapCallback {
        /**
         * Callback invoked with the current config {@link DataMap} for
         * LoneBrewerWatchFace.
         */
        void onConfigDataMapFetched(DataMap config);
    }

    /**
     * Asynchronously fetches the current config {@link DataMap} for LoneBrewerWatchFace
     * and passes it to the given callback.
     * <p>
     * If the current config {@link DataItem} doesn't exist, it isn't created and the callback
     * receives an empty DataMap.
     */
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

    /**
     * Overwrites (or sets, if not present) the keys in the current config {@link DataItem} with
     * the ones appearing in the given {@link DataMap}. If the config DataItem doesn't exist,
     * it's created.
     * <p>
     * It is allowed that only some of the keys used in the config DataItem appear in
     * {@code configKeysToOverwrite}. The rest of the keys remains unmodified in this case.
     */
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

    /**
     * Overwrites the current config {@link DataItem}'s {@link DataMap} with {@code newConfig}.
     * If the config DataItem doesn't exist, it's created.
     */
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
        if (!config.containsKey(KEY_DAY_OF_THE_WEEK)){
            config.putInt(KEY_DAY_OF_THE_WEEK, IS_DAY_OF_THE_WEEK_ON_DEFAULT);
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
