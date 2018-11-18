package com.example.detectorapp.CameraClasses;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import com.example.detectorapp.CameraClasses.GraphicOverlay;
import com.example.detectorapp.CameraClasses.GraphicOverlay.Graphic;

public class CameraImageGraphic extends Graphic {
    private final Bitmap bitmap;

    public CameraImageGraphic(GraphicOverlay overlay, Bitmap bitmap) {
        super(overlay);
        this.bitmap = bitmap;
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawBitmap(bitmap, null, new Rect(0, 0, canvas.getWidth(), canvas.getHeight()), null);

    }
}
