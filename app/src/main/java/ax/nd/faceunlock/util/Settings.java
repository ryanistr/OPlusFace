package ax.nd.faceunlock.util;

import android.content.Context;
import android.util.Log;

public class Settings {
    private static final String PROPERTY_FACEUNLOCK_AVAILABLE = "property_faceunlock_available";
    private static final String TAG = Settings.class.getSimpleName();

    public static void setFaceUnlockAvailable(Context context, int i) {
        SharedUtil sharedPrefUtil = new SharedUtil(context);
        sharedPrefUtil.saveIntValue(PROPERTY_FACEUNLOCK_AVAILABLE, i);
        Log.d(TAG, "setFaceUnlockAvailable: " + i);
    }

    public static boolean isFaceUnlockAvailable(Context context) {
        SharedUtil sharedPrefUtil = new SharedUtil(context);
        return sharedPrefUtil.getIntValueByKey(PROPERTY_FACEUNLOCK_AVAILABLE) == 1;
    }
}
