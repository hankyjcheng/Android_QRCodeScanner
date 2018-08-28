package com.hankyjcheng.qrcodescanner.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;

/**
 * An utility class that contains useful methods for app-wide usage.
 */

public class Utils {

    /**
     * Check if the app has the specified permissions
     */
    public static boolean hasPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check whether camera is available to this device
     *
     * @return TRUE if camera is available to use
     */
    public static boolean checkCameraHardware(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

}