package deep.dark.lonebrewer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.WindowInsets;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import deep.dark.lonebrewercommon.LBWFUtil;

public class LoneBrewerWatchFace extends CanvasWatchFaceService {

    private static final String TAG = "LoneBrewerWatchFace";

    // updating only once per second - efficient!
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);


    private static final String[] MONTHS = {
            "Opal", "Obsidian", "Granite",
            "Slate", "Felsite", "Hematite",
            "Malachite", "Galena", "Limestone",
            "Sandstone", "Timber", "Moonstone"
    };

    private static final String[] DAYS_OF_THE_WEEK = {
            "su", "mo", "tu", "we", "th", "fr", "sa"
    };

    private static final String[] ALPHABET = {
            "a", "e", "f", "h", "m", "o", "p", "r", "s", "t", "u", "w"
    };
    private static final String[] SEASONS = {"Spring", "Summer", "Autumn", "Winter"};
    private static final String[] SEASONS_MOD = {"Early", "Mid", "Late"};

    // an arbitrary row number (counting from the top) for calendar text on a rectangular device
    private static final int DATE_ROW_SQUARE = 7, HALL_WIDTH = 1;

    private static final char[] ROCK_SYMBOLS = {'░', '▒', '▓'},
            UNEXPLORED_CODES = {'%', '\'', ',', '.', '`'},
            ROUGH_FLOOR_CODES = {'\'', ',', '.', '`'};

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    static class TimeHandler extends Handler {
        private final WeakReference<Engine> engineReference;

        TimeHandler(Engine engine) {
            engineReference = new WeakReference<>(engine);
        }
        @Override
        public void handleMessage(Message msg)
        {
            Engine engine = engineReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case Engine.MSG_UPDATE_TIME:
                        engine.invalidate();
                        if (engine.shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = INTERACTIVE_UPDATE_RATE_MS
                                    - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                            sendEmptyMessageDelayed(Engine.MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        static final int MSG_UPDATE_TIME = 0;

        private final TimeHandler updateTimeHandler = new TimeHandler(this);

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                calendar.setTimeZone(TimeZone.getTimeZone(intent.getStringExtra("time-zone")));
                prevMinutes = -1;
                invalidate();
            }
        };

        boolean registeredTimeZoneReceiver = false;
        // we need to know if device is in ambient mode when config changes
        // otherwise we might start applying colours when in b/w
        boolean isInAmbientMode = false;
        // calendar line placement is a tad different for circular devices
        boolean isRound;
        // still and brewer placement is a tad different for circular devices with a chin
        int chinHeight;
        int hemisphere = LBWFUtil.HEMISPHERE_DEFAULT;
        boolean is12hModeOn = LBWFUtil.IS_12H_MODE_ON_DEFAULT == 1;
        boolean areWeekdaysOn = LBWFUtil.IS_DAY_OF_THE_WEEK_ON_DEFAULT == 1;

        // original tileset and our very own background bitmaps
        Bitmap tilesetBitmap, backgroundBitmap;
        // tileset can take cp437 char and return rect of that char in the tileset bitmap
        Tileset tileset;
        // filters allow us to change bitmap colour
        LightingColorFilter barrelUsualFilter, ambientFilter, cropsUsualFilter, magmaUsualFilter;
        Paint backgroundPaint, datePaint, barrelPaint, magmaPaint, cropsPaint, clearPaint;
        Calendar calendar;
        // the majority of updates happen only every minute
        // now problem is, prevMinutes needs to be set to something like -1 every time
        // we want to update UI
        int minutes, prevMinutes;
        // AsciiObject can take ascii and tileset and then draw stuff using
        // canvas.drawBitmap(bitmap, rect, rect, paint);
        AsciiObject[] digits;
        AsciiObject datePart1, datePart2, hour10, hour01, colon, minute10, minute01;
        AsciiObject dayOfTheWeek1, dayOfTheWeek2, apm1, apm2;
        AsciiObject magma;
        // often used symbol rects
        Rect barrelRect, stoneRect, currentStockpileRect, cropsRect, currentCropsRect, magmaRect, currentMagmaRect;
        // this will change depending of device form-factor
        int dateRow;
        // this as well
        String dateDelimiter;
        // available width and height in symbols
        int symbolCols, symbolRows;
        Still still;
        Brewer brewer;
        // an attempt to get a bit of data api out of the way
        DataApiHelper dataApiHelper;
        HashMap<String, AsciiObject> alphabet;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(LoneBrewerWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setStatusBarGravity(Gravity.TOP | Gravity.START)
                    .setHotwordIndicatorGravity(Gravity.TOP | Gravity.END)
                    .build());

            AssetManager assetManager = LoneBrewerWatchFace.this.getAssets();
            try {
                tilesetBitmap = BitmapFactory.decodeStream(assetManager.open("tileset.png"));
                tileset = new Tileset(tilesetBitmap);

                barrelRect = tileset.getSymbolRect('÷');
                stoneRect = tileset.getSymbolRect('•');
                currentStockpileRect = barrelRect;
                cropsRect = tileset.getSymbolRect('═');
                currentCropsRect = cropsRect;
                magmaRect = tileset.getSymbolRect('≈');
                currentMagmaRect = magmaRect;

                digits = new AsciiObject[10];
                for (int i = 0; i < 10; i++) {
                    digits[i] = new AsciiObject(readFileToString("digits/digit" + i + ".txt", assetManager), tileset);
                }

                alphabet = new HashMap<>();
                for(String letter : ALPHABET){
                    alphabet.put(letter, new AsciiObject(readFileToString("letters/" + letter + ".txt", assetManager), tileset));
                }

                colon = new AsciiObject(readFileToString("colon.txt", assetManager), tileset);

                // still has three colours, so we need three parts
                still = new Still(tileset,
                        readFileToString("stillPartA.txt", assetManager),
                        readFileToString("stillPartB.txt", assetManager),
                        readFileToString("stillPartC.txt", assetManager)
                );

                brewer = new Brewer(tileset);

                magma = new AsciiObject(readFileToString("magma.txt", assetManager), tileset);
            }
            catch (IOException e) {
                Log.e(TAG, "IO-related horrors");
            }

            backgroundPaint = new Paint();
            backgroundPaint.setColorFilter(new LightingColorFilter(0, Color.rgb(128, 128, 128)));

            datePaint = new Paint();
            datePaint.setColorFilter(new LightingColorFilter(0, Color.WHITE));

            ambientFilter = new LightingColorFilter(0, Color.WHITE);
            barrelUsualFilter = new LightingColorFilter(0, LBWFUtil.BARRELS_COLOUR_DEFAULT);
            barrelPaint = new Paint();
            barrelPaint.setColorFilter(barrelUsualFilter);
            cropsUsualFilter = new LightingColorFilter(0, LBWFUtil.CROPS_COLOUR_DEFAULT);
            cropsPaint = new Paint();
            cropsPaint.setColorFilter(cropsUsualFilter);
            magmaUsualFilter = new LightingColorFilter(0, LBWFUtil.MAGMA_COLOUR_DEFAULT);
            magmaPaint = new Paint();
            magmaPaint.setColorFilter(magmaUsualFilter);

            // in theory, we can have background colour other than black
            clearPaint = new Paint();
            clearPaint.setColorFilter(new LightingColorFilter(0, Color.BLACK));

            calendar = Calendar.getInstance();
            prevMinutes = -1;

            dataApiHelper = new DataApiHelper(LoneBrewerWatchFace.this, new DataApiHelper.DataHelperInterface() {

                @Override
                public boolean updateUiForKey(String configKey, int configValue) {
                    boolean updated = true;
                    switch (configKey) {
                        case LBWFUtil.KEY_TIME_MODE:
                            is12hModeOn = configValue == 1;
                            break;
                        case LBWFUtil.KEY_DAY_OF_THE_WEEK:
                            areWeekdaysOn = configValue == 1;
                            break;
                        case LBWFUtil.KEY_HEMISPHERE:
                            hemisphere = configValue;
                            break;
                        case LBWFUtil.KEY_BARRELS_COLOUR:
                            barrelUsualFilter = new LightingColorFilter(0, configValue);
                            // we don't want to start using colour while in ambient mode
                            if (!isInAmbientMode) barrelPaint.setColorFilter(barrelUsualFilter);
                            break;
                        case LBWFUtil.KEY_CROPS_COLOUR:
                            cropsUsualFilter = new LightingColorFilter(0, configValue);
                            if (!isInAmbientMode) cropsPaint.setColorFilter(cropsUsualFilter);
                            break;
                        case LBWFUtil.KEY_MAGMA_COLOUR:
                            magmaUsualFilter = new LightingColorFilter(0, configValue);
                            if (!isInAmbientMode) magmaPaint.setColorFilter(magmaUsualFilter);
                            break;
                        default:
                            updated = false;
                    }
                    return updated;
                }

                @Override
                public void onUiUpdated() {
                    prevMinutes = -1;
                    invalidate();
                }
            });
        }

        private String readFileToString(String fileName, AssetManager assetManager) throws IOException {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(assetManager.open(fileName)));
            StringBuilder total = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                total.append(line);
                total.append("\n");
            }
            return total.toString();
        }

        @Override
        public void onDestroy() {
            updateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);

            isInAmbientMode = inAmbientMode;
            // turn everything to white rock salt
            if (inAmbientMode) {
                barrelPaint.setColorFilter(ambientFilter);
                cropsPaint.setColorFilter(ambientFilter);
                magmaPaint.setColorFilter(ambientFilter);
                currentStockpileRect = stoneRect;
                currentCropsRect = stoneRect;
                currentMagmaRect = stoneRect;
            }
            // turn everything back
            else {
                barrelPaint.setColorFilter(barrelUsualFilter);
                cropsPaint.setColorFilter(cropsUsualFilter);
                magmaPaint.setColorFilter(magmaUsualFilter);
                currentStockpileRect = barrelRect;
                currentCropsRect = cropsRect;
                currentMagmaRect = magmaRect;
                brewer.reset();
            }
            prevMinutes = -1;
            invalidate();

            // whether the timer should be running depends on whether we're in ambient mode (as well
            // as whether we're visible), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);
            isRound = insets.isRound();
            chinHeight = insets.getSystemWindowInsetBottom();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (backgroundBitmap == null
                    || backgroundBitmap.getWidth() != width
                    || backgroundBitmap.getHeight() != height) {

                // getting the size of the screen in symbols
                symbolCols = width / tileset.symbolWidth;
                symbolRows = height / tileset.symbolHeight;

                // calendar line has to be broken in two on circular devices
                if (isRound) {
                    dateDelimiter = "";
                    dateRow = symbolCols / 4 - 3;
                    generateBackground(width, height, symbolCols / 4);
                }
                else {
                    dateDelimiter = ", ";
                    dateRow = DATE_ROW_SQUARE;
                    generateBackground(width, height, dateRow + 2);
                }
            }
            super.onSurfaceChanged(holder, format, width, height);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            calendar.setTimeInMillis(System.currentTimeMillis());
            minutes = calendar.get(Calendar.MINUTE);

            if (prevMinutes != minutes) {

                int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
                int month = calendar.get(Calendar.MONTH);

                if (hemisphere == LBWFUtil.HEMISPHERE_SOUTHERN) {
                    // seems legit
                    month = (month < 6) ? month + 6 : month - 6;
                }
                // a bit of trickery to get month indices to map nicely into season indices
                int seasonMonth = month - 2 >= 0 ? month - 2 : 12 + month - 2;
                int season = seasonMonth / 3;

                datePart1 = new AsciiObject(dayOfMonth + getDayOfMonthSuffix(dayOfMonth)
                        + " " + MONTHS[month]
                        + ", " + calendar.get(Calendar.YEAR), tileset);
                datePart2 = new AsciiObject(dateDelimiter + SEASONS_MOD[seasonMonth - 3 * season]
                        + " " + SEASONS[season], tileset);

                if (isRound) {
                    datePart1.setPosition((symbolCols - datePart1.width) / 2, dateRow - 1);
                    datePart2.setPosition((symbolCols - datePart2.width) / 2, dateRow + 1);
                }
                else {
                    datePart1.setPosition((symbolCols - (datePart1.width + datePart2.width)) / 2, dateRow);
                    datePart2.setPosition(datePart1.col + datePart1.width, dateRow);
                }

                String abbreviation = DAYS_OF_THE_WEEK[calendar.get(Calendar.DAY_OF_WEEK) - 1];
                dayOfTheWeek1.copyAscii(alphabet.get(abbreviation.substring(0, 1)));
                dayOfTheWeek2.copyAscii(alphabet.get(abbreviation.substring(1, 2)));

                if (is12hModeOn) {
                    int hour = calendar.get(Calendar.HOUR);
                    // apm is confusing
                    setTwoDigitValue(hour10, hour01, hour == 0 ? 12 : hour);
                    apm1.copyAscii(calendar.get(Calendar.AM_PM) == Calendar.AM
                            ? alphabet.get("a")
                            : alphabet.get("p")
                    );
                }
                else {
                    setTwoDigitValue(hour10, hour01, calendar.get(Calendar.HOUR_OF_DAY));
                }

                setTwoDigitValue(minute10, minute01, minutes);

                prevMinutes = minutes;
            }

            canvas.drawColor(Color.BLACK);
            datePart1.draw(tilesetBitmap, canvas, clearPaint, datePaint);
            datePart2.draw(tilesetBitmap, canvas, clearPaint, datePaint);

            if (!isInAmbientMode()) {
                canvas.drawBitmap(backgroundBitmap, 0, 0, backgroundPaint);
                still.draw(tilesetBitmap, canvas, clearPaint);
                brewer.draw(tilesetBitmap, canvas, clearPaint);
                if (!areWeekdaysOn) magma.draw(tilesetBitmap, canvas, clearPaint, magmaPaint);
            }

            hour10.draw(tilesetBitmap, canvas, clearPaint, barrelPaint, currentStockpileRect);
            hour01.draw(tilesetBitmap, canvas, clearPaint, barrelPaint, currentStockpileRect);
            colon.draw(tilesetBitmap, canvas, clearPaint, barrelPaint, currentStockpileRect);
            minute10.draw(tilesetBitmap, canvas, clearPaint, barrelPaint, currentStockpileRect);
            minute01.draw(tilesetBitmap, canvas, clearPaint, barrelPaint, currentStockpileRect);

            if (areWeekdaysOn) {
                dayOfTheWeek1.draw(tilesetBitmap, canvas, clearPaint, magmaPaint, currentMagmaRect);
                dayOfTheWeek2.draw(tilesetBitmap, canvas, clearPaint, magmaPaint, currentMagmaRect);
            }

            if (is12hModeOn) {
                apm1.draw(tilesetBitmap, canvas, clearPaint, cropsPaint, currentCropsRect);
                apm2.draw(tilesetBitmap, canvas, clearPaint, cropsPaint, currentCropsRect);
            }
        }

        private void setTwoDigitValue(AsciiObject digit10, AsciiObject digit01, int value) {
            digit10.copyAscii(digits[value / 10]);
            digit01.copyAscii(digits[value % 10]);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                dataApiHelper.connect();
                registerReceiver();

                // update time zone in case it changed while we weren't visible.
                calendar.setTimeZone(TimeZone.getDefault());
            } else {
                unregisterReceiver();
                dataApiHelper.disconnect();
            }

            // whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (registeredTimeZoneReceiver) {
                return;
            }
            registeredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            LoneBrewerWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!registeredTimeZoneReceiver) {
                return;
            }
            registeredTimeZoneReceiver = false;
            LoneBrewerWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }


        // starts the updateTimeHandler timer if it should be running and isn't currently
        // stops it if it shouldn't be running but currently is.
        private void updateTimer() {
            updateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                updateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        private String getDayOfMonthSuffix(final int n) {
            if (n >= 11 && n <= 13) {
                return "th";
            }
            switch (n % 10) {
                case 1:
                    return "st";
                case 2:
                    return "nd";
                case 3:
                    return "rd";
                default:
                    return "th";
            }
        }

        // returns whether the updateTimeHandler timer should be running. The timer should
        // only run when we're visible and in interactive mode.
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        private void generateBackground(int width, int height, int stockpileStartRow) {
            // generating static background image
            backgroundBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(backgroundBitmap);
            // selecting random rock for walls
            Rect rock = tileset.getSymbolRect(ROCK_SYMBOLS[(int) (Math.random() * ROCK_SYMBOLS.length)]);
            // setting floor symbols
            Rect stockpileFloor = tileset.getSymbolRect('=');
            Rect smoothFloor = tileset.getSymbolRect('+');
            Rect farmPlot = tileset.getSymbolRect('≈');
            // stockpile goes on enough to accommodate time barrels
            int stockpileEndRow = stockpileStartRow + digits[0].height + 3;
            int timeBarrelsRow = stockpileStartRow + 2;
            // four digits + two digit gaps + colon
            int digitWidth = digits[0].width;
            int digitGap = digitWidth / 3;
            int timeBarrelsWidth = digitWidth * 4 + digitGap * 2 + colon.width;
            int timeBarrelsCol = (symbolCols - timeBarrelsWidth) / 2;

            // time barrels!
            hour10 = new AsciiObject(timeBarrelsCol, timeBarrelsRow);
            hour01 = new AsciiObject(hour10.col + digitWidth + digitGap, timeBarrelsRow);
            colon.setPosition(hour01.col + digitWidth, timeBarrelsRow);
            minute10 = new AsciiObject(colon.col + colon.width, timeBarrelsRow);
            minute01 = new AsciiObject(minute10.col + digitWidth + digitGap, timeBarrelsRow);

            // placing day of the week and apm directly below time barrels
            int weekdayRow = stockpileEndRow + 1;
            magma.setPosition(hour10.col, weekdayRow);
            dayOfTheWeek1 = new AsciiObject(hour10.col, weekdayRow);
            dayOfTheWeek2 = new AsciiObject(hour01.col, weekdayRow);
            apm1 = new AsciiObject(minute10.col, weekdayRow);
            apm2 = new AsciiObject(minute01.col, weekdayRow);
            apm2.copyAscii(alphabet.get("m"));

            // then there is a narrow corridor
            int entryEndCol = minute10.col - 2;
            int entryStartCol = entryEndCol - HALL_WIDTH + 1;

            // with a brewery entrance in it
            int breweryEntranceCol = entryEndCol + 1;
            int breweryEntranceRow = stockpileEndRow + 3;

            // brewer and his still should be above the chin at all times
            int stillRow = symbolRows - (4 + (int)Math.ceil(chinHeight / tileset.symbolHeight));
            still.setPosition(breweryEntranceCol + 1, stillRow);
            brewer.setPosition(breweryEntranceCol + 1 + 4, stillRow);

            Rect dest = new Rect(), src;
            for (int row = stockpileStartRow; row < symbolRows; row++) {
                for (int col = 0; col < symbolCols; col++) {
                    dest.set(
                            col * tileset.symbolWidth,
                            row * tileset.symbolHeight,
                            (col + 1) * tileset.symbolWidth,
                            (row + 1) * tileset.symbolHeight
                    );
                    // inside the storage
                    if (row <= stockpileEndRow) {
                        // carving an exit at the bottom
                        if (row == stockpileEndRow) {
                            if (col < entryStartCol || col > entryEndCol) {
                                src = rock;
                            } else {
                                src = smoothFloor;
                            }
                        }
                        // the rest is either stockpile or walls
                        else if (row == stockpileStartRow || col == 0 || col == symbolCols - 1) {
                            src = rock;
                        } else {
                            src = stockpileFloor;
                        }
                    }
                    // below storage
                    else {
                        // carving an entrance into the brewery
                        if (row == breweryEntranceRow && col == breweryEntranceCol) {
                            src = smoothFloor;
                        }
                        // carving the rest of the corridor
                        else if (col >= entryStartCol - 1 && col <= entryEndCol + 1) {
                            if (col == entryStartCol - 1 || col == entryEndCol + 1) {
                                src = rock;
                            } else {
                                src = smoothFloor;
                            }
                        }
                        // inside the brewery it's either farm plots or rough floor (and walls)
                        else if (col >= entryEndCol) {
                            if (col == symbolCols - 1 || row == symbolRows - 1) {
                                src = rock;
                            } else if (col >= minute10.col && col < minute01.col + digitWidth && row <= stockpileEndRow + 6) {
                                src = farmPlot;
                            } else {
                                src = tileset.getSymbolRect(ROUGH_FLOOR_CODES[(int) (Math.random() * ROUGH_FLOOR_CODES.length)]);
                            }
                        }
                        // outside is the unexplored dark
                        else {
                            if (Math.random() * 10 < 9.6f) {
                                src = tileset.getSymbolRect(' ');
                            } else {
                                src = tileset.getSymbolRect(UNEXPLORED_CODES[(int) (Math.random() * UNEXPLORED_CODES.length)]);
                            }
                        }
                    }
                    canvas.drawBitmap(tilesetBitmap, src, dest, backgroundPaint);
                }
            }
        }
    }
}