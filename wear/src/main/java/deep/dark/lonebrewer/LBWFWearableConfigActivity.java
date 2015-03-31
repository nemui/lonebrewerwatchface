package deep.dark.lonebrewer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.view.CircledImageView;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import org.jraf.android.androidwearcolorpicker.app.ColorPickActivity;

import deep.dark.lonebrewercommon.LBWFUtil;

public class LBWFWearableConfigActivity extends Activity {
    private static final String TAG = "LBWFConfigActivity";

    private static final int REQUEST_PICK_COLOUR = 0;
    private static final int BARRELS_COLOUR = 0, MAGMA_COLOUR = 1, CROPS_COLOUR = 2;

    private Switch timeModeSwitch;
    private CircledImageView barrelsColourView, magmaColourView, cropsColourView;
    private int barrelsColour, magmaColour, cropsColour, whichColour;
    private boolean is12hModeOn = true;
    private DataApiHelper dataApiHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wearable_config);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                timeModeSwitch = (Switch) stub.findViewById(R.id.timeModeSwitch);
                timeModeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        is12hModeOn = isChecked;
                        dataApiHelper.updateConfigDataItem(LBWFUtil.KEY_TIME_MODE, isChecked ? 1 : 0);
                    }
                });

                Button okButton = (Button) stub.findViewById(R.id.okButton);
                okButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        LBWFWearableConfigActivity.this.finish();
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

        dataApiHelper = new DataApiHelper(this, new DataApiHelper.DataHelperInterface() {
            @Override
            public boolean updateUiForKey(String configKey, int configValue) {
                boolean updated = true;
                switch (configKey) {
                    case LBWFUtil.KEY_TIME_MODE:
                        is12hModeOn = configValue == 1;
                        break;
                    case LBWFUtil.KEY_BARRELS_COLOUR:
                        barrelsColour = configValue;
                        break;
                    case LBWFUtil.KEY_CROPS_COLOUR:
                        cropsColour = configValue;
                        break;
                    case LBWFUtil.KEY_MAGMA_COLOUR:
                        magmaColour = configValue;
                        break;
                    default:
                        updated = false;
                }
                return updated;
            }

            @Override
            public void onUiUpdated() {
                LBWFWearableConfigActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateUI();
                    }
                });
            }
        });
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
                dataApiHelper.updateConfigDataItem(key, pickedColor);
                updateUI();
                break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        dataApiHelper.connect();
    }

    @Override
    protected void onStop() {
        dataApiHelper.disconnect();
        super.onStop();
    }

    private void updateUI () {
        timeModeSwitch.setChecked(is12hModeOn);
        barrelsColourView.setCircleColor(barrelsColour);
        magmaColourView.setCircleColor(magmaColour);
        cropsColourView.setCircleColor(cropsColour);
    }
}