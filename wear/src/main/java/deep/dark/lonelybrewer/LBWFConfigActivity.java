package deep.dark.lonelybrewer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.view.CircledImageView;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Wearable;

import org.jraf.android.androidwearcolorpicker.app.ColorPickActivity;

import deep.dark.lonelybrewer.dataapi.LBWFUtil;

public class LBWFConfigActivity extends Activity {
    private static final String TAG = "LBWFConfigActivity";

    private static final int REQUEST_PICK_COLOUR = 0;
    private static final int BARRELS_COLOUR = 0, MAGMA_COLOUR = 1, CROPS_COLOUR = 2;

    private GoogleApiClient googleApiClient;
    private Switch timeModeSwitch;
    private CircledImageView barrelsColourView, magmaColourView, cropsColourView;
    private int barrelsColour, magmaColour, cropsColour, whichColour;
    private boolean is12hModeOn = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dfwatchface_config);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                timeModeSwitch = (Switch) stub.findViewById(R.id.timeModeSwitch);
                timeModeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        is12hModeOn = isChecked;
                        updateConfigDataItem(LBWFUtil.KEY_TIME_MODE, isChecked ? 1 : 0);
                    }
                });

                Button okButton = (Button) stub.findViewById(R.id.okButton);
                okButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        LBWFConfigActivity.this.finish();
                    }
                });

                barrelsColourView = (CircledImageView) stub.findViewById(R.id.barrelColour);
                barrelsColourView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        whichColour = BARRELS_COLOUR;
                        startColourPicking(barrelsColour);
                    }
                });
                magmaColourView = (CircledImageView) stub.findViewById(R.id.magmaColour);
                magmaColourView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        whichColour = MAGMA_COLOUR;
                        startColourPicking(magmaColour);
                    }
                });
                cropsColourView = (CircledImageView) stub.findViewById(R.id.cropsColour);
                cropsColourView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        whichColour = CROPS_COLOUR;
                        startColourPicking(cropsColour);
                    }
                });
            }
        });

        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        updateConfigDataItemOnStartup();
                    }

                    @Override
                    public void onConnectionSuspended(int cause) {}
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                    }
                })
                .addApi(Wearable.API)
                .build();
    }

    private void startColourPicking (int oldColour) {
        Intent intent = new ColorPickActivity.IntentBuilder().oldColor(oldColour).build(this);
        startActivityForResult(intent, REQUEST_PICK_COLOUR);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_PICK_COLOUR:
                if (resultCode == RESULT_CANCELED) {
                    // The user pressed 'Cancel'
                    break;
                }

                int pickedColor = ColorPickActivity.getPickedColor(data);
                String key;
                if (whichColour == BARRELS_COLOUR) {
                    barrelsColour = pickedColor;
                    key = LBWFUtil.KEY_BARRELS_COLOUR;
                }
                else if (whichColour == MAGMA_COLOUR) {
                    magmaColour = pickedColor;
                    key = LBWFUtil.KEY_MAGMA_COLOUR;
                }
                else {
                    cropsColour = pickedColor;
                    key = LBWFUtil.KEY_CROPS_COLOUR;
                }
                updateConfigDataItem(key, pickedColor);
                updateUI();
                break;
        }
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

    private void updateConfigDataItem(final String configCode, final int configValue) {
        DataMap configKeysToOverwrite = new DataMap();
        configKeysToOverwrite.putInt(configCode, configValue);
        LBWFUtil.overwriteKeysInConfigDataMap(googleApiClient, configKeysToOverwrite);
    }

    private void updateConfigDataItemOnStartup() {
        LBWFUtil.fetchConfigDataMap(googleApiClient,
                new LBWFUtil.FetchConfigDataMapCallback() {
                    @Override
                    public void onConfigDataMapFetched(DataMap startupConfig) {
                        // If the DataItem hasn't been created yet or some keys are missing,
                        // use the default values.
                        LBWFUtil.setDefaultValuesForMissingConfigKeys(startupConfig);
                        LBWFUtil.putConfigDataItem(googleApiClient, startupConfig);
                        is12hModeOn = startupConfig.getInt(LBWFUtil.KEY_TIME_MODE) == 1;
                        barrelsColour = startupConfig.getInt(LBWFUtil.KEY_BARRELS_COLOUR);
                        magmaColour = startupConfig.getInt(LBWFUtil.KEY_MAGMA_COLOUR);
                        cropsColour = startupConfig.getInt(LBWFUtil.KEY_CROPS_COLOUR);
                        updateUI();
                    }
                }
        );
    }

    private void updateUI () {
        timeModeSwitch.setChecked(is12hModeOn);
        barrelsColourView.setCircleColor(barrelsColour);
        magmaColourView.setCircleColor(magmaColour);
        cropsColourView.setCircleColor(cropsColour);
    }
}

