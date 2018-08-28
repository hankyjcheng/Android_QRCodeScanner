package com.hankyjcheng.qrcodescanner.activity.camera;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;

import com.hankyjcheng.qrcodescanner.R;
import com.hankyjcheng.qrcodescanner.databinding.ActivityCameraBinding;
import com.hankyjcheng.qrcodescanner.util.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class CameraActivity extends AppCompatActivity {

    public static final String TAG = CameraActivity.class.getSimpleName();

    public final static String INTENT_EXTRA_QR_CODE = "intent_extra_qr_code";
    public final static String INTENT_EXTRA_SUCCESS = "intent_extra_success";

    private ActivityCameraBinding binding;

    private static final String[] APP_PERMISSIONS =
            new String[]{
                    Manifest.permission.CAMERA,
            };
    private static final int REQUEST_APP_PERMISSION = 1234;

    private QRCodeDetector qrCodeDetector;
    private Timer photoTimer;

    private int bitmapX;
    private int bitmapY;
    private int bitmapSize;

    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private String cameraId;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private CameraCaptureSession cameraCaptureSession;
    private Size previewSize;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_camera);
        setContentView(binding.getRoot());

        binding.cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        photoTimer = new Timer();
        qrCodeDetector = new QRCodeDetector();

        if (Utils.hasPermissions(this, APP_PERMISSIONS)) {
            startCamera();
        }
        else {
            Log.i(TAG, "Request Camera Permission");
            requestAppPermissions();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (photoTimer != null) {
            photoTimer.cancel();
        }
        closeCamera();
        closeBackgroundThread();
    }

    /**
     * Callback received when a permission request has been completed. If the permission is denied,
     * go back to MainActivity
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_APP_PERMISSION) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    finish();
                    return;
                }
            }
            if (Utils.hasPermissions(this, APP_PERMISSIONS)) {
                startCamera();
            }
            else {
                requestAppPermissions();
            }
        }
    }

    /**
     * When QR Code evaluation comes back successfully, stop the periodic photo capture timer, and
     * return captured code to the AddressActivity
     */
    private QRCodeDetector.QRCodeDetectorCallback qrCodeDetectorCallback = new QRCodeDetector.QRCodeDetectorCallback() {
        @Override
        public void onSuccess(String code) {
            Log.i(TAG, "Success QR Code: " + code);
            photoTimer.cancel();

            Intent data = new Intent();
            data.putExtra(INTENT_EXTRA_QR_CODE, code);
            data.putExtra(INTENT_EXTRA_SUCCESS, true);
            setResult(RESULT_OK, data);
            finish();
        }

        @Override
        public void onFailure() {
            Log.d(TAG, "Detect QR Code Failure");
        }
    };

    private void startCamera() {
        openBackgroundThread();
        if (binding.cameraPreview.isAvailable()) {
            setupCamera();
            openCamera();
        }
        else {
            binding.cameraPreview.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                    setupCamera();
                    openCamera();
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                    return false;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surface) {

                }
            });
        }
    }

    private void setupCamera() {
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                    StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    if (streamConfigurationMap != null) {
                        previewSize = streamConfigurationMap.getOutputSizes(SurfaceTexture.class)[0];
                        Log.i(TAG, "Preview Size: " + previewSize.getWidth() + ", " + previewSize.getHeight());
                        this.cameraId = cameraId;
                        adjustLayout();
                    }
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void adjustLayout() {
        // Determine the photo ratio so that we can resize the preview view into the same ratio for user to reference
        float ratio = (float) previewSize.getHeight() / previewSize.getWidth();
        if (ratio < 1) {
            ratio = 1 / ratio;
        }

        // Determine the new size for the preview view from the ratio
        int frameWidth = binding.cameraPreview.getWidth();
        int frameHeight = binding.cameraPreview.getHeight();
        int previewHeight = (int) (frameWidth * ratio);

        FrameLayout.LayoutParams previewLayoutParams = (FrameLayout.LayoutParams) binding.cameraPreview.getLayoutParams();
        previewLayoutParams.width = frameWidth;
        previewLayoutParams.height = previewHeight;
        previewLayoutParams.setMarginStart(0);
        previewLayoutParams.topMargin = (frameHeight - previewHeight) / 2;
        binding.cameraPreview.setLayoutParams(previewLayoutParams);

        Log.i(TAG, "FrameSize: " + frameWidth + ", " + frameHeight);
        int min = Math.min(frameWidth, frameHeight);
        int size = (int) (min * 0.33f);

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(size, size);
        layoutParams.setMarginStart((frameWidth - size) / 2);
        layoutParams.topMargin = (frameHeight - size) / 2;
        binding.focusSquare.setLayoutParams(layoutParams);

        int lineWidth = size / 30;
        bitmapX = (frameWidth - size) / 2 + lineWidth;
        bitmapY = (previewHeight - size) / 2 + lineWidth;
        bitmapSize = size - 2 * lineWidth;
    }

    private void createPreviewSession() {
        try {
            SurfaceTexture surfaceTexture = binding.cameraPreview.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface previewSurface = new Surface(surfaceTexture);
            final CaptureRequest.Builder captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(previewSurface);

            cameraDevice.createCaptureSession(Collections.singletonList(previewSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            if (cameraDevice == null) {
                                return;
                            }

                            try {
                                CaptureRequest captureRequest = captureRequestBuilder.build();
                                CameraActivity.this.cameraCaptureSession = cameraCaptureSession;
                                CameraActivity.this.cameraCaptureSession.setRepeatingRequest(captureRequest,
                                        null, backgroundHandler);
                                beginCapture();
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                        }
                    }, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                cameraManager.openCamera(cameraId, stateCallback, backgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openBackgroundThread() {
        backgroundThread = new HandlerThread("camera_background_thread");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            CameraActivity.this.cameraDevice = cameraDevice;
            createPreviewSession();
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            cameraDevice.close();
            CameraActivity.this.cameraDevice = null;
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            cameraDevice.close();
            CameraActivity.this.cameraDevice = null;
        }
    };

    private void closeCamera() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }

        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    private void closeBackgroundThread() {
        if (backgroundHandler != null) {
            backgroundThread.quitSafely();
            backgroundThread = null;
            backgroundHandler = null;
        }
    }

    /**
     * Begin to take photo every 300 millisecond, with an initial delay of 2 seconds.
     */
    private void beginCapture() {
        photoTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Bitmap bitmap = binding.cameraPreview.getBitmap();
                QRCodeImageProcessor imageProcessor = new QRCodeImageProcessor(bitmap, qrCodeDetector, qrCodeDetectorCallback, bitmapX, bitmapY, bitmapSize);
                imageProcessor.execute();
                Log.d(TAG, "Take Picture");
            }
        }, 2000, 300);
    }

    /**
     * Request the permission for the app to gain access to the camera
     */
    private void requestAppPermissions() {
        List<String> permissionRequests = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            permissionRequests.add(Manifest.permission.CAMERA);
        }
        ActivityCompat.requestPermissions(this, permissionRequests.toArray(new String[0]), REQUEST_APP_PERMISSION);
    }

}