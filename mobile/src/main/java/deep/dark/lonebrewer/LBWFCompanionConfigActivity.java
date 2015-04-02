package deep.dark.lonebrewer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.wearable.companion.WatchFaceCompanion;
import android.support.wearable.view.CircledImageView;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import com.chiralcode.colorpicker.ColorPickerDialog;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import deep.dark.lonebrewercommon.LBWFUtil;

/**
 * The phone-side config activity for {@code LoneBrewerWatchFace}. Like the watch-side config
 * activity ({@code LBWFWearableConfigActivity}), allows for toggling 12 hour mode as well as
 * setting  the colours for barrels, magma and crops.
 * Additionally, enables toggling day of the week display and selecting the hemisphere.
 */
public class LBWFCompanionConfigActivity extends Activity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        ResultCallback<DataApi.DataItemResult> {
    private static final String TAG = "LBWFCompanionConfig";

    private static final int BARRELS_COLOUR = 0, MAGMA_COLOUR = 1, CROPS_COLOUR = 2;

    private String peerId;
    private Switch timeModeSwitch, weekdaysSwitch, hemisphereSwitch;
    private CircledImageView barrelsColourView, magmaColourView, cropsColourView;
    private int barrelsColour, magmaColour, cropsColour, whichColour;
    private GoogleApiClient googleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.companion_config);

        timeModeSwitch = (Switch) findViewById(R.id.timeModeSwitch);
        timeModeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sendConfigUpdateMessage(LBWFUtil.KEY_TIME_MODE, isChecked ? 1 : 0);
            }
        });

        weekdaysSwitch = (Switch) findViewById(R.id.weekdaysSwitch);
        weekdaysSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sendConfigUpdateMessage(LBWFUtil.KEY_DAY_OF_THE_WEEK, isChecked ? 1 : 0);
            }
        });

        hemisphereSwitch = (Switch) findViewById(R.id.hemisphereSwitch);
        hemisphereSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sendConfigUpdateMessage(LBWFUtil.KEY_HEMISPHERE, isChecked ? 0 : 1);
            }
        });

        Button okButton = (Button) findViewById(R.id.okButton);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LBWFCompanionConfigActivity.this.finish();
            }
        });

        barrelsColourView = (CircledImageView) findViewById(R.id.barrelColour);
        barrelsColourView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                whichColour = BARRELS_COLOUR;
                startColourPicking(barrelsColour);
            }
        });
        magmaColourView = (CircledImageView) findViewById(R.id.magmaColour);
        magmaColourView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                whichColour = MAGMA_COLOUR;
                startColourPicking(magmaColour);
            }
        });
        cropsColourView = (CircledImageView) findViewById(R.id.cropsColour);
        cropsColourView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                whichColour = CROPS_COLOUR;
                startColourPicking(cropsColour);
            }
        });

        peerId = getIntent().getStringExtra(WatchFaceCompanion.EXTRA_PEER_ID);
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();
    }

    private void startColourPicking (int colour) {
        ColorPickerDialog colorPickerDialog = new ColorPickerDialog(this, colour, new ColorPickerDialog.OnColorSelectedListener() {

            @Override
            public void onColorSelected(int color) {
                if (whichColour == BARRELS_COLOUR) {
                    barrelsColour = color;
                    barrelsColourView.setCircleColor(barrelsColour);
                    sendConfigUpdateMessage(LBWFUtil.KEY_BARRELS_COLOUR, barrelsColour);
                }
                else if (whichColour == MAGMA_COLOUR) {
                    magmaColour = color;
                    magmaColourView.setCircleColor(magmaColour);
                    sendConfigUpdateMessage(LBWFUtil.KEY_MAGMA_COLOUR, magmaColour);
                }
                else if (whichColour == CROPS_COLOUR) {
                    cropsColour = color;
                    cropsColourView.setCircleColor(cropsColour);
                    sendConfigUpdateMessage(LBWFUtil.KEY_CROPS_COLOUR, cropsColour);
                }
            }
        });
        colorPickerDialog.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if (googleApiClient != null && googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if (peerId != null) {
            Uri.Builder builder = new Uri.Builder();
            Uri uri = builder.scheme("wear").path(LBWFUtil.PATH_WITH_FEATURE).authority(peerId).build();
            Wearable.DataApi.getDataItem(googleApiClient, uri).setResultCallback(this);
        }
        else {
            displayNoConnectedDeviceDialog();
        }
    }

    @Override
    public void onResult(DataApi.DataItemResult dataItemResult) {
        DataMap config;
        if (dataItemResult.getStatus().isSuccess() && dataItemResult.getDataItem() != null) {
            DataItem configDataItem = dataItemResult.getDataItem();
            DataMapItem dataMapItem = DataMapItem.fromDataItem(configDataItem);
            config = dataMapItem.getDataMap();
        }
        else {
            // If DataItem with the current config can't be retrieved, select the default items on
            // each picker.
            config = new DataMap();
            LBWFUtil.setDefaultValuesForMissingConfigKeys(config);
        }
        setUpUI(config);
    }

    @Override
    public void onConnectionSuspended(int cause) {}

    @Override
    public void onConnectionFailed(ConnectionResult result) {}

    private void displayNoConnectedDeviceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String messageText = getResources().getString(R.string.title_no_device_connected);
        String okText = getResources().getString(R.string.ok_no_device_connected);
        builder.setMessage(messageText)
                .setCancelable(false)
                .setPositiveButton(okText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) { }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void setUpUI(DataMap config) {
        timeModeSwitch.setChecked(config.getInt(LBWFUtil.KEY_TIME_MODE) == 1);
        weekdaysSwitch.setChecked(config.getInt(LBWFUtil.KEY_DAY_OF_THE_WEEK) == 1);
        hemisphereSwitch.setChecked(config.getInt(LBWFUtil.KEY_HEMISPHERE) == 0);
        barrelsColour = config.getInt(LBWFUtil.KEY_BARRELS_COLOUR);
        magmaColour = config.getInt(LBWFUtil.KEY_MAGMA_COLOUR);
        cropsColour = config.getInt(LBWFUtil.KEY_CROPS_COLOUR);
        barrelsColourView.setCircleColor(barrelsColour);
        magmaColourView.setCircleColor(magmaColour);
        cropsColourView.setCircleColor(cropsColour);
    }

    private void sendConfigUpdateMessage(String configKey, int color) {
        if (peerId != null) {
            DataMap config = new DataMap();
            config.putInt(configKey, color);
            byte[] rawData = config.toByteArray();
            Wearable.MessageApi.sendMessage(googleApiClient, peerId, LBWFUtil.PATH_WITH_FEATURE, rawData);
        }
    }
}