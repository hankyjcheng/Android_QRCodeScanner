package com.hankyjcheng.qrcodescanner.activity.camera;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

public class QRCodeImageProcessor extends AsyncTask<Void, Void, Void> {

    public static final String TAG = QRCodeImageProcessor.class.getSimpleName();

    private QRCodeDetector.QRCodeDetectorCallback detectorCallback;
    private Bitmap bitmap;
    private QRCodeDetector detector;
    private int cropX;
    private int cropY;
    private int cropSize;

    QRCodeImageProcessor(Bitmap bitmap, QRCodeDetector detector, QRCodeDetector.QRCodeDetectorCallback detectorCallback,
                         int cropX, int cropY, int cropSize) {
        this.detectorCallback = detectorCallback;
        this.bitmap = bitmap;
        this.detector = detector;
        this.cropX = cropX;
        this.cropY = cropY;
        this.cropSize = cropSize;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        // Crop the bitmap to only what is inside the QR Code target box
        Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, cropX, cropY, cropSize, cropSize);
        Log.d(TAG, "Resize Bitmap Size: " + croppedBitmap.getWidth() + ", " + croppedBitmap.getHeight());

        // Evaluate the bitmap shown inside the QR Code target box
        detector.evaluateQRCode(croppedBitmap, detectorCallback);
        return null;
    }

}