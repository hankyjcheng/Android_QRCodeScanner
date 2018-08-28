package com.hankyjcheng.qrcodescanner.activity.main;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.hankyjcheng.qrcodescanner.R;
import com.hankyjcheng.qrcodescanner.activity.camera.CameraActivity;
import com.hankyjcheng.qrcodescanner.databinding.ActivityMainBinding;

import static com.hankyjcheng.qrcodescanner.activity.camera.CameraActivity.INTENT_EXTRA_QR_CODE;
import static com.hankyjcheng.qrcodescanner.activity.camera.CameraActivity.INTENT_EXTRA_SUCCESS;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getSimpleName();

    private ActivityMainBinding binding;

    public final static int REQUEST_CAMERA_SCAN = 1234;

    private Toast backPressToast;
    private long lastBackPressTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        setContentView(binding.getRoot());

        binding.cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchCameraActivity();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CAMERA_SCAN) {
                if (data.hasExtra(INTENT_EXTRA_SUCCESS)) {
                    boolean success = data.getBooleanExtra(INTENT_EXTRA_SUCCESS, false);
                    if (success) {
                        String serial = data.getStringExtra(INTENT_EXTRA_QR_CODE);
                        Log.i(TAG, "Serial #: " + serial);
                        showSerialNumberDialog(serial);
                    }
                    else {
                        showCameraErrorDialog();
                    }
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (lastBackPressTime < System.currentTimeMillis() - 5000) {
            backPressToast = Toast.makeText(getApplicationContext(), R.string.back_press_exit_prompt, Toast.LENGTH_LONG);
            backPressToast.show();
            lastBackPressTime = System.currentTimeMillis();
        }
        else {
            if (backPressToast != null) {
                backPressToast.cancel();
            }
            finish();
        }
    }

    private void launchCameraActivity() {
        Intent intent = new Intent(getApplicationContext(), CameraActivity.class);
        startActivityForResult(intent, REQUEST_CAMERA_SCAN);
    }

    /**
     * Show the dialog for user to confirm that the QR Code is correct.
     */
    private void showSerialNumberDialog(final String text) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.qr_code_confirm_dialog_title);
        builder.setMessage("QR Code: " + text);
        builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Show the dialog for when there's something wrong with the camera
     */
    private void showCameraErrorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.camera_error_dialog_title);
        builder.setMessage(R.string.camera_error_dialog_message);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

}