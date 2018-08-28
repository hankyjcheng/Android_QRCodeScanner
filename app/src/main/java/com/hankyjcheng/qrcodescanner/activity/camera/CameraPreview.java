package com.hankyjcheng.qrcodescanner.activity.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

@SuppressLint("ViewConstructor")
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    public static final String TAG = CameraPreview.class.getSimpleName();

    private SurfaceHolder holder;
    private Camera camera;

    public CameraPreview(Context context, Camera camera) {
        super(context);
        this.camera = camera;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        holder = getHolder();
        holder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            // Set the camera parameters to help with taking the photos with better quality
            Camera.Parameters camParams = camera.getParameters();
            if (camParams.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                camParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }
            Camera.Size bestPreviewSize = determineBestPreviewSize(camParams);
            camParams.setPreviewSize(bestPreviewSize.width, bestPreviewSize.height);
            if (camParams.getSupportedWhiteBalance().contains(Camera.Parameters.WHITE_BALANCE_AUTO)) {
                camParams.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
                Log.i(TAG, "CamParams Set White Balance");
            }
            if (camParams.getSupportedSceneModes().contains(Camera.Parameters.SCENE_MODE_BARCODE)) {
                camParams.setSceneMode(Camera.Parameters.SCENE_MODE_BARCODE);
                Log.i(TAG, "CamParams Set Scene Barcode");
            }
            camera.setParameters(camParams);

            // Rotate the camera display so that on the preview it is shown in portrait mode
            camera.setDisplayOrientation(90);
            camera.setPreviewDisplay(holder);
            camera.startPreview();
        } catch (IOException e) {
            Log.e(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (this.holder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            camera.stopPreview();
        } catch (Exception e) {
            e.printStackTrace();
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
            camera.setPreviewDisplay(this.holder);
            camera.startPreview();

        } catch (Exception e) {
            Log.e(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    private static Camera.Size determineBestPreviewSize(Camera.Parameters parameters) {
        List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
        return determineBestSize(sizes);
    }

    private static Camera.Size determineBestSize(List<Camera.Size> sizes) {
        Camera.Size bestSize = null;
        long used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long availableMemory = Runtime.getRuntime().maxMemory() - used;
        for (Camera.Size currentSize : sizes) {
            int newArea = currentSize.width * currentSize.height;
            long neededMemory = newArea * 4 * 4; // newArea * 4 Bytes/pixel * 4 needed copies of the bitmap (for safety :) )
            boolean isDesiredRatio = (currentSize.width / 4) == (currentSize.height / 3);
            boolean isBetterSize = (bestSize == null || currentSize.width > bestSize.width);
            boolean isSafe = neededMemory < availableMemory;
            if (isDesiredRatio && isBetterSize && isSafe) {
                bestSize = currentSize;
            }
        }
        if (bestSize == null) {
            return sizes.get(0);
        }
        return bestSize;
    }
}