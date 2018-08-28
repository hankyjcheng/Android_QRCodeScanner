package com.hankyjcheng.qrcodescanner.activity.camera;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;

import java.util.List;

/**
 * The QR Code Detector uses Firebase's MLKit barcode scanner to evaluate the QR Code.
 */
public class QRCodeDetector {

    public interface QRCodeDetectorCallback {
        void onSuccess(String code);

        void onFailure();
    }

    public static final String TAG = QRCodeDetector.class.getSimpleName();

    private FirebaseVisionBarcodeDetector detector;

    QRCodeDetector() {
        FirebaseVisionBarcodeDetectorOptions options =
                new FirebaseVisionBarcodeDetectorOptions.Builder()
                        .setBarcodeFormats(
                                FirebaseVisionBarcode.FORMAT_QR_CODE)
                        .build();
        detector = FirebaseVision.getInstance()
                .getVisionBarcodeDetector(options);
    }

    /**
     * Evaluate the bitmap image for potential QR Code image
     */
    public void evaluateQRCode(Bitmap bitmap, final QRCodeDetectorCallback callback) {
        if (bitmap == null) {
            Log.d(TAG, "Bitmap is NULL");
            return;
        }
        final FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);

        Task<List<FirebaseVisionBarcode>> result = detector.detectInImage(image);
        result.addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionBarcode>>() {
            @Override
            public void onSuccess(List<FirebaseVisionBarcode> barcodes) {
                Log.d(TAG, "Evaluate QR Code Success");
                Log.d(TAG, "Barcode Size: " + barcodes.size());
                String code = null;
                for (FirebaseVisionBarcode barcode : barcodes) {
                    String rawValue = barcode.getRawValue();
                    Log.d(TAG, "RawValue: " + rawValue);
                    code = rawValue;
                }
                if (code != null) {
                    callback.onSuccess(code);
                }
                else {
                    callback.onFailure();
                }
            }
        });
        result.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "Evaluate QR Code Failed");
                e.printStackTrace();
                callback.onFailure();
            }
        });
    }

}