package deep.dark.lonebrewercommon;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;

public class Still {
    private static final String TAG = "Still";
    AsciiObject partA, partB, partC;
    Paint paint1, paint2;

    public Still (Tileset tileset, String asciiA, String asciiB, String asciiC) {
        partA = new AsciiObject(asciiA, tileset);
        partB = new AsciiObject(asciiB, tileset);
        partC = new AsciiObject(asciiC, tileset);
        paint1 = new Paint();
        paint1.setColorFilter(new LightingColorFilter(0, Color.rgb(128, 128, 128)));
        paint2 = new Paint();
        paint2.setColorFilter(new LightingColorFilter(0, Color.rgb(0, 128, 0)));
    }

    public void setPosition (int col, int row) {
        partA.setPosition(col, row);
        partB.setPosition(col, row);
        partC.setPosition(col, row);
    }

    public void draw (Bitmap tilesetBitmap, Canvas canvas, Paint clearPaint) {
        partA.draw(tilesetBitmap, canvas, paint1, clearPaint);
        partB.draw(tilesetBitmap, canvas, clearPaint, paint1);
        partC.draw(tilesetBitmap, canvas, clearPaint, paint2);
    }
}