package deep.dark.lonebrewer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;

public class Brewer {

    private static final int PATTERN_LEFT_RIGHT = 0, PATTERN_UP_DOWN = 1,
        PATTERN_CLOCKWISE = 2, PATTERN_COUNTER_CLOCKWISE = 3, PATTERN_SLEEP = 4;
    private static final int[][][] DIFFS = {
            {
                    {1, 0}, {-1, 0}, {-1, 0}, {1, 0}
            },
            {
                    {0, 1}, {0, -1}, {0, -1}, {0, 1}
            },
            {
                    {1, 0}, {0, 1}, {-1, 0}, {0, -1}
            },
            {
                    {-1, 0}, {0, 1}, {1, 0}, {0, -1}
            },
    };

    AsciiObject dwarf, sleepingDwarf;
    Paint dwarfPaint, sleepingDwarfPaint;
    int pattern;
    boolean isSleeping, needsResetting;
    int col, row, diffIndex;

    public Brewer (Tileset tileset) {
        dwarf = new AsciiObject("☺", tileset);
        sleepingDwarf = new AsciiObject("Z", tileset);
        dwarfPaint = new Paint();
        dwarfPaint.setColorFilter(new LightingColorFilter(0, Color.rgb(128, 128, 0)));
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
        sleepingDwarf.setPosition(col, row);
    }

    public void draw (Bitmap tilesetBitmap, Canvas canvas, Paint clearPaint) {
        if (needsResetting) {
            needsResetting = false;
            dwarf.setPosition(col, row);
            diffIndex = 0;
            pattern = (int)Math.floor(Math.random()*5);
        }
        if (pattern == PATTERN_SLEEP) {
            isSleeping = !isSleeping;
            if (isSleeping) {
                sleepingDwarf.draw(tilesetBitmap, canvas, clearPaint, sleepingDwarfPaint);
            }
            else {
                dwarf.draw(tilesetBitmap, canvas, clearPaint, dwarfPaint);
            }
        }
        else {
            dwarf.setPosition(
                    dwarf.col + DIFFS[pattern][diffIndex][0],
                    dwarf.row + DIFFS[pattern][diffIndex][1]
            );
            diffIndex = (diffIndex + 1) % DIFFS[pattern].length;
            dwarf.draw(tilesetBitmap, canvas, clearPaint, dwarfPaint);
        }
    }
}
