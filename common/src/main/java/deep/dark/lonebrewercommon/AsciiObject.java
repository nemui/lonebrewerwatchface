package deep.dark.lonebrewercommon;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import java.util.ArrayList;

/*
    AsciiObject can take ascii and tileset and then draw stuff using
    canvas.drawBitmap(bitmap, rect, rect, paint)
 */
public class AsciiObject {
    private static final String TAG = "AsciiObject";

    // for relative positioning
    public int width, height;

    public int col, row;
    Rect[] srcRects, dstRects;

    public AsciiObject(String ascii, Tileset tileset) {
        ArrayList<Rect> destRectArray = new ArrayList<>();
        ArrayList<Rect> srcRectArray = new ArrayList<>();

        String[] symbolArray = ascii.split("");
        String symbol;

        for (int index = 0; index < symbolArray.length; index++) {
            symbol = symbolArray[index];
            if (symbol.equals("\n")){
                row++;
                width = Math.max(width, col);
                col = 0;
            }
            else if (!symbol.isEmpty()) {
                if (!symbol.equals(" ")){
                    srcRectArray.add(new Rect(tileset.getSymbolRect(symbol.charAt(0))));
                    destRectArray.add(new Rect(
                            col * tileset.symbolWidth,
                            row * tileset.symbolHeight,
                            (col + 1) * tileset.symbolWidth,
                            (row + 1) * tileset.symbolHeight
                    ));
                }
                col++;
            }
        }
        width = Math.max(width, col);
        height = (col == 0) ? row : row + 1;
        col = row = 0;
        srcRects = srcRectArray.toArray(new Rect[srcRectArray.size()]);
        dstRects = destRectArray.toArray(new Rect[destRectArray.size()]);
    }

    public AsciiObject (int col, int row) {
        this.col = col;
        this.row = row;
    }

    public void copyAscii (AsciiObject other) {
        srcRects = other.srcRects;

        dstRects = new Rect[other.dstRects.length];
        for (int i = 0; i < dstRects.length; i++) {
            dstRects[i] = new Rect(other.dstRects[i]);
        }

        int tempCol = col;
        int tempRow = row;
        col = other.col;
        row = other.row;
        setPosition(tempCol, tempRow);
    }

    public void setPosition (int col, int row) {
        int colDiff = col - this.col;
        int rowDiff = row - this.row;
        this.col = col;
        this.row = row;
        for (int i = 0; i < dstRects.length; i++) {
            dstRects[i].offset(colDiff * dstRects[i].width(), rowDiff * dstRects[i].height());
        }
    }

    public void draw (Bitmap tilesetBitmap, Canvas canvas, Paint background, Paint foreground) {
        for (int i = 0; i < dstRects.length; i++) {
            canvas.drawRect(dstRects[i], background);
            canvas.drawBitmap(tilesetBitmap, srcRects[i], dstRects[i], foreground);
        }
    }

    // we need this method for ambient mode where everything is drawn as rock salt (additional srcRect)
    public void draw (Bitmap tilesetBitmap, Canvas canvas, Paint background, Paint foreground, Rect srcRect) {
        for (int i = 0; i < dstRects.length; i++) {
            canvas.drawRect(dstRects[i], background);
            canvas.drawBitmap(tilesetBitmap, srcRect, dstRects[i], foreground);
        }
    }
}