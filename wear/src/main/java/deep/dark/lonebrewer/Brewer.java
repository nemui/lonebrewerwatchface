package deep.dark.lonebrewer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;

public class Brewer {

    private static final int PATTERN_MOVE = 0, PATTERN_STATUS = 1, PATTERN_SLEEP = 2;
    private static final int STATUS_HUNGRY = 0, STATUS_THIRSTY = 1, STATUS_DROWSY = 2,
            STATUS_UNHAPPY = 3;
    private static final int[][][] MOVEMENT_TYPES = {
            {
                    {1, 0}, {-1, 0}, {-1, 0}, {1, 0} // left right
            },
            {
                    {0, 1}, {0, -1}, {0, -1}, {0, 1} // up down
            },
            {
                    {1, 0}, {0, 1}, {-1, 0}, {0, -1} // clockwise
            },
            {
                    {-1, 0}, {0, 1}, {1, 0}, {0, -1} // counter-clockwise
            },
    };

    AsciiObject dwarf, statusDwarf, sleepingDwarf;
    Paint dwarfPaint, sleepingDwarfPaint;
    Paint[] statusPaint;
    int pattern, status, movementType;
    boolean blinkingFlag, needsResetting;
    int col, row, diffIndex;

    public Brewer (Tileset tileset) {
        dwarf = new AsciiObject("☺", tileset);
        statusDwarf = new AsciiObject("↓", tileset);
        sleepingDwarf = new AsciiObject("Z", tileset);
        dwarfPaint = new Paint();
        dwarfPaint.setColorFilter(new LightingColorFilter(0, Color.rgb(128, 128, 0)));

        statusPaint = new Paint[4];
        statusPaint[STATUS_HUNGRY] = new Paint();
        statusPaint[STATUS_HUNGRY].setColorFilter(new LightingColorFilter(0, Color.rgb(128, 112, 0)));
        statusPaint[STATUS_THIRSTY] = new Paint();
        statusPaint[STATUS_THIRSTY].setColorFilter(new LightingColorFilter(0, Color.rgb(0, 0, 255)));
        statusPaint[STATUS_DROWSY] = new Paint();
        statusPaint[STATUS_DROWSY].setColorFilter(new LightingColorFilter(0, Color.rgb(192, 192, 192)));
        statusPaint[STATUS_UNHAPPY] = new Paint();
        statusPaint[STATUS_UNHAPPY].setColorFilter(new LightingColorFilter(0, Color.rgb(255, 0, 0)));

        sleepingDwarfPaint = new Paint();
        sleepingDwarfPaint.setColorFilter(new LightingColorFilter(0, Color.rgb(128, 128, 128)));
    }

    public void reset () {
        needsResetting = true;
    }

    public void setPosition (int col, int row) {
        this.col = col;
        this.row = row;
        dwarf.setPosition(col, row);
        statusDwarf.setPosition(col, row);
        sleepingDwarf.setPosition(col, row);
    }

    public void draw (Bitmap tilesetBitmap, Canvas canvas, Paint clearPaint) {
        if (needsResetting) {
            needsResetting = false;
            dwarf.setPosition(col, row);

            pattern = random(3);
            if (pattern == PATTERN_MOVE) {
                movementType = random(MOVEMENT_TYPES.length);
                diffIndex = 0;
            }
            else if (pattern == PATTERN_STATUS) {
                status = random(4);
            }
        }

        if (pattern == PATTERN_MOVE) {
            dwarf.setPosition(
                    dwarf.col + MOVEMENT_TYPES[movementType][diffIndex][0],
                    dwarf.row + MOVEMENT_TYPES[movementType][diffIndex][1]
            );
            diffIndex = (diffIndex + 1) % MOVEMENT_TYPES[movementType].length;
            dwarf.draw(tilesetBitmap, canvas, clearPaint, dwarfPaint);
        }
        else if (pattern == PATTERN_STATUS) {
            blinkingFlag = !blinkingFlag;
            if (blinkingFlag) {
                statusDwarf.draw(tilesetBitmap, canvas, clearPaint, statusPaint[status]);
            }
            else {
                dwarf.draw(tilesetBitmap, canvas, clearPaint, dwarfPaint);
            }
        }
        else if (pattern == PATTERN_SLEEP) {
            blinkingFlag = !blinkingFlag;
            if (blinkingFlag) {
                sleepingDwarf.draw(tilesetBitmap, canvas, clearPaint, sleepingDwarfPaint);
            }
            else {
                dwarf.draw(tilesetBitmap, canvas, clearPaint, dwarfPaint);
            }
        }
    }

    private int random (int number) {
        return (int)Math.floor(Math.random()*number);
    }
}
