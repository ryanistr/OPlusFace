package ax.nd.faceunlock;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;
import ax.nd.faceunlock.vendor.FacePPImpl;
import ax.nd.faceunlock.camera.CameraFaceEnrollController;
import ax.nd.faceunlock.camera.CameraFaceAuthController;
import ax.nd.faceunlock.camera.CameraService;
import ax.nd.faceunlock.util.Util; // Added Import for Util

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FaceAuthBridge {
    private static final String TAG = "FaceAuthBridge";
    private static FaceAuthBridge sInstance;
    private Context mContext;
    private FacePPImpl mFacePP;
    private Handler mHandler;
    private SurfaceTexture mDummySurface;
    private CameraFaceAuthController mAuthController;
    private static final int FRONT_CAMERA_ID = 1;
    
    // Engine Constants
    private static final int MG_UNLOCK_OK = 0;
    private static final int MG_UNLOCK_FACE_SCALE_TOO_SMALL = 4;
    private static final int MG_UNLOCK_FACE_SCALE_TOO_LARGE = 5;
    private static final int MG_UNLOCK_FACE_BLUR = 11;
    private static final int MG_UNLOCK_DARKLIGHT = 12;
    private static final int MG_UNLOCK_KEEP = 19;
    private static final int FACE_ACQUIRED_GOOD = 0;
    private static final int FACE_ACQUIRED_INSUFFICIENT = 1;
    private static final int FACE_ACQUIRED_TOO_DARK = 3;
    private static final int FACE_ACQUIRED_TOO_CLOSE = 4;
    private static final int FACE_ACQUIRED_TOO_FAR = 5;

    private volatile boolean mEnrollFinished = false;
    private volatile boolean mEngineSuccess = false;
    private static final int TOTAL_STEPS = 20; 
    private int mCurrentSteps = TOTAL_STEPS;
    private int mPendingFaceId = 0;
    private long mLastUpdateTime = 0;

    private FaceAuthBridge(Context context) {
        mContext = context;
        mHandler = new Handler(Looper.getMainLooper());
        mFacePP = new FacePPImpl(context);
        mDummySurface = new SurfaceTexture(10); 
    }

    public static void init(Context context) {
        if (sInstance == null) {
            try {
                sInstance = new FaceAuthBridge(context);
                sInstance.mFacePP.init();
            } catch (Throwable t) {}
        }
    }

    public static FaceAuthBridge getInstance() {
        return sInstance;
    }

    public SurfaceTexture getDummySurface() {
        return mDummySurface;
    }

    public boolean isHardwareDetected() {
        return true; 
    }

    // --- CHALLENGE ---
    public void generateChallenge(final int sensorId, final int userId, final Object receiver) {
        mHandler.post(() -> notifyChallengeGenerated(receiver, sensorId, userId, new Random().nextLong()));
    }

    public void revokeChallenge(final int sensorId, final int userId, final Object receiver) {
        mHandler.post(() -> notifyChallengeRevoked(receiver, sensorId, userId, 0L));
    }

    // --- ENROLLMENT ---
    public void startEnroll(final int userId, final Object receiverObject, final Surface previewSurface) {
        Log.d(TAG, "startEnroll: User " + userId);
        mEnrollFinished = false;
        mEngineSuccess = false;
        mCurrentSteps = TOTAL_STEPS;
        
        mHandler.post(() -> {
            try {
                forceReleaseCamera();
                try { Thread.sleep(150); } catch (Exception e) {}
                notifyEnrollResult(receiverObject, 0, userId, TOTAL_STEPS);
                mFacePP.saveFeatureStart();
                CameraFaceEnrollController.getInstance(mContext).start(new CameraFaceEnrollController.CameraCallback() {
                    byte[] mFeature = new byte[10000]; 
                    byte[] mFaceData = new byte[40000];
                    int[] mOutId = new int[1];
                    @Override public int handleSaveFeature(byte[] data, int width, int height, int angle) {
                        if (mEnrollFinished || mEngineSuccess) return 0;
                        return mFacePP.saveFeature(data, width, height, angle, true, mFeature, mFaceData, mOutId);
                    }
                    @Override public void handleSaveFeatureResult(int res) {
                        if (mEnrollFinished || mEngineSuccess) return;
                        if (res == MG_UNLOCK_OK) { 
                            mEngineSuccess = true;
                            int finalFaceId = (mOutId[0] <= 0) ? 1 : mOutId[0];
                            mPendingFaceId = finalFaceId;
                            Log.i(TAG, "Enroll Success ID: " + finalFaceId);
                            runProgressAnimation(receiverObject, userId);
                            return;
                        }
                        long now = System.currentTimeMillis();
                        if (now - mLastUpdateTime < 100) return;
                        mLastUpdateTime = now;
                        int info = -1;
                        if (res == MG_UNLOCK_KEEP) {
                             if (mCurrentSteps > 5) {
                                 mCurrentSteps--;
                                 notifyEnrollResult(receiverObject, 0, userId, mCurrentSteps);
                             }
                             info = FACE_ACQUIRED_GOOD;
                        }
                        if (info != -1) notifyAcquired(receiverObject, userId, info, 0);
                    }
                    @Override public void onFaceDetected() {}
                    @Override public void onTimeout() { if(!mEngineSuccess) { stopEnroll(); notifyError(receiverObject, 3, 0); } }
                    @Override public void onCameraError() { if(!mEngineSuccess) { stopEnroll(); notifyError(receiverObject, 1, 0); } }
                    @Override public void setDetectArea(android.hardware.Camera.Size size) { mFacePP.setDetectArea(0, 0, size.height, size.width); }
                }, FRONT_CAMERA_ID, previewSurface); 
            } catch (Throwable t) {
                forceReleaseCamera();
                notifyError(receiverObject, 1, 0);
            }
        });
    }

    private void runProgressAnimation(final Object receiver, final int userId) {
        final int DELAY_MS = 50;
        mHandler.post(new Runnable() {
            public void run() {
                if (mEnrollFinished) return;
                mCurrentSteps--;
                if (mCurrentSteps <= 0) {
                    mCurrentSteps = 0;
                    mEnrollFinished = true;
                    notifySystemUIonFaceChanged(true);
                    notifyEnrollResult(receiver, mPendingFaceId, userId, 0);
                    stopEnrollControllerOnly();
                } else {
                    notifyEnrollResult(receiver, 0, userId, mCurrentSteps); 
                    mHandler.postDelayed(this, DELAY_MS);
                }
            }
        });
    }

    public void stopEnroll() {
        try {
            mEnrollFinished = true;
            mFacePP.saveFeatureStop();
            stopEnrollControllerOnly();
            forceReleaseCamera();
        } catch (Throwable t) {}
    }

    private void stopEnrollControllerOnly() {
        CameraFaceEnrollController.getInstance(mContext).stop(null);
    }

    // --- AUTHENTICATION ---
    public void startAuthenticate(final int sensorId, final int userId, final Object receiverObject) {
        Log.d(TAG, "startAuthenticate: Sensor " + sensorId + " User " + userId);
        mHandler.post(() -> {
            try {
                forceReleaseCamera(); 
                try { Thread.sleep(100); } catch (Exception e) {}
                mFacePP.compareStart();
                mAuthController = new CameraFaceAuthController(mContext, new CameraFaceAuthController.ServiceCallback() {
                    @Override
                    public int handlePreviewData(byte[] data, int width, int height) {
                        int[] scores = new int[20];
                        int res = mFacePP.compare(data, width, height, 0, true, true, scores);
                        if (res == 0) { 
                            Log.i(TAG, "Auth Success! Unlocking...");
                            stopAuthenticateInternal(); 
                            notifyAuthenticated(receiverObject, sensorId, 1, userId); 
                        }
                        return res;
                    }
                    @Override public void setDetectArea(android.hardware.Camera.Size size) { mFacePP.setDetectArea(0, 0, size.height, size.width); }
                    @Override public void onTimeout(boolean b) { stopAuthenticateInternal(); notifyError(receiverObject, 3, 0); }
                    @Override public void onCameraError() { stopAuthenticateInternal(); notifyError(receiverObject, 1, 0); }
                });
                mAuthController.start(FRONT_CAMERA_ID, mDummySurface); 
            } catch (Throwable t) {
                forceReleaseCamera();
                notifyError(receiverObject, 1, 0);
            }
        });
    }

    public void stopAuthenticate() {
        stopAuthenticateInternal();
    }

    private void stopAuthenticateInternal() {
        try {
            mFacePP.compareStop();
            if (mAuthController != null) {
                mAuthController.stop();
                mAuthController = null;
            }
            forceReleaseCamera();
        } catch (Throwable t) {}
    }

    private void forceReleaseCamera() {
        CameraService.closeCamera(null);
    }

    public void remove(final int userId, final int faceId, final Object receiver) {
        Log.d(TAG, "Removing Face ID: " + faceId);
        mHandler.post(() -> {
            notifySystemUIonFaceChanged(false);
            mFacePP.deleteFeature(faceId); 
            notifyRemoved(receiver, faceId, userId, 0);
            
        });
    }

    public long getAuthenticatorId() {
        return 123456789L; 
    }

    public List<Object> getEnrolledFaces(int sensorId, int userId) {
        List<Object> faces = new ArrayList<>();
        if (mFacePP.hasEnrolledFaces()) { 
            try {
                Class<?> faceClass = Class.forName("android.hardware.face.Face");
                java.lang.reflect.Constructor<?> ctor = faceClass.getConstructor(CharSequence.class, int.class, long.class);
                Object face = ctor.newInstance("Face 1", 1, (long)sensorId);
                faces.add(face);
            } catch (Exception e) {}
        }
        return faces;
    }

    // --- REFLECTION HELPERS ---
    
    private void notifyChallengeGenerated(Object receiver, int sensorId, int userId, long challenge) {
        try {
            Method m = receiver.getClass().getMethod("onChallengeGenerated", int.class, int.class, long.class);
            m.invoke(receiver, sensorId, userId, challenge);
        } catch (Exception e) {}
    }
    
    private void notifyChallengeRevoked(Object receiver, int sensorId, int userId, long challenge) {}

    private void notifyRemoved(Object receiver, int faceId, int userId, int remaining) {
        try {
            Class<?> faceClass = Class.forName("android.hardware.face.Face");
            java.lang.reflect.Constructor<?> ctor = faceClass.getConstructor(CharSequence.class, int.class, long.class);
            Object faceObj = ctor.newInstance("", faceId, 0L);
            Method m = receiver.getClass().getMethod("onRemoved", faceClass, int.class);
            m.invoke(receiver, faceObj, remaining);
        } catch (Exception e) {}
    }

    private void notifyAcquired(Object receiver, int userId, int acquiredInfo, int vendorCode) {
        try {
            try {
                Method m = receiver.getClass().getMethod("onAcquired", int.class, int.class, int.class);
                m.invoke(receiver, 0, acquiredInfo, vendorCode);
            } catch (NoSuchMethodException e) {
                Method m = receiver.getClass().getMethod("onAcquired", int.class, int.class);
                m.invoke(receiver, acquiredInfo, vendorCode);
            }
        } catch (Exception e) {}
    }

    private void notifyEnrollResult(Object receiver, int faceId, int userId, int remaining) {
        try {
            Class<?> faceClass = Class.forName("android.hardware.face.Face");
            java.lang.reflect.Constructor<?> ctor = faceClass.getConstructor(CharSequence.class, int.class, long.class);
            Object faceObj = ctor.newInstance("", faceId, 0L);
            Method m = receiver.getClass().getMethod("onEnrollResult", faceClass, int.class);
            m.invoke(receiver, faceObj, remaining);
        } catch (Exception e) {}
    }

    // CRITICAL FIX: UNWRAP CONVERTER
    private void notifyAuthenticated(Object receiver, int deviceId, int faceId, int userId) {
        try {
            Class<?> faceClass = Class.forName("android.hardware.face.Face");
            java.lang.reflect.Constructor<?> ctor = faceClass.getConstructor(CharSequence.class, int.class, long.class);
            Object faceObj = ctor.newInstance("", faceId, (long)deviceId);
            
            Object targetReceiver = receiver;

            // SMART UNWRAP: Extract inner receiver from Wrapper/Converter
            if (receiver.getClass().getName().contains("ClientMonitorCallbackConverter") || 
                receiver.getClass().getName().contains("Wrapper")) {
                
                try {
                    Field[] fields = receiver.getClass().getDeclaredFields();
                    for (Field f : fields) {
                        f.setAccessible(true);
                        Object val = f.get(receiver);
                        if (val != null && val.getClass().getName().contains("IFaceServiceReceiver")) {
                            Log.d(TAG, "Unwrapped receiver to: " + val.getClass().getName());
                            targetReceiver = val;
                            break;
                        }
                    }
                } catch (Exception ex) {
                    Log.w(TAG, "Failed to unwrap receiver", ex);
                }
            }

            // Now call on the (hopefully) unwrapped AIDL proxy
            try {
                Method m = targetReceiver.getClass().getMethod("onAuthenticationSucceeded", faceClass, int.class, boolean.class);
                m.invoke(targetReceiver, faceObj, userId, true);
                Log.d(TAG, "Invoked onAuthenticationSucceeded successfully");
            } catch (NoSuchMethodException e) {
                // Fallback: Try byte[] signature if boolean fails (unlikely for AIDL Proxy but good safety)
                try {
                    Method m = targetReceiver.getClass().getMethod("onAuthenticationSucceeded", faceClass, int.class, byte[].class);
                    m.invoke(targetReceiver, faceObj, userId, new byte[0]);
                    Log.d(TAG, "Invoked onAuthenticationSucceeded (byte[])");
                } catch (Exception ex2) {
                    Log.e(TAG, "FATAL: Could not find method on receiver: " + targetReceiver.getClass().getName());
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Notify Auth Failed", e);
        }
    }

    private void notifyError(Object receiver, int error, int vendorCode) {
        try {
            Method m = receiver.getClass().getMethod("onError", int.class, int.class);
            m.invoke(receiver, error, vendorCode);
        } catch (Exception e) {}
    }

    /**
     * Updates persist.sys.oplus.isFaceEnrolled system property.
     * 1 = Enrolled
     * 0 = Not Enrolled / Removed
     */
    private void notifySystemUIonFaceChanged(boolean isEnrolled) {
        String val = isEnrolled ? "1" : "0";
        Util.setSystemProperty("persist.sys.oplus.isFaceEnrolled", val);
        Log.i(TAG, "SystemUI Notified: persist.sys.oplus.isFaceEnrolled = " + val);
    }
}