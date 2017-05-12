package com.dji.geodemo;

import android.graphics.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * This is a helper class to set different colors for different height.
 */
public class FlyfrbBasePainter {

    protected Map<Integer, Integer> mHeightToColor = new HashMap<>();

    protected final int mColorTransparent = Color.argb(0, 0, 0, 0);

    public FlyfrbBasePainter() {
        mHeightToColor.put(65, Color.argb(50, 0, 0, 0));
        mHeightToColor.put(125, Color.argb(25, 0, 0, 0));
    }

    public Map<Integer, Integer> getmHeightToColor() {
        return mHeightToColor;
    }

    public int getmColorTransparent() {
        return mColorTransparent;
    }
}
