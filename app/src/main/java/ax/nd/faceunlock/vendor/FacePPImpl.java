package ax.nd.faceunlock.vendor;

import android.content.Context;
import android.util.Log;
import ax.nd.faceunlock.backend.CustomUnlockEncryptor;
import ax.nd.faceunlock.backend.FaceUnlockVendorImpl;
import java.io.File;

public class FacePPImpl {
    private static final String TAG = "FacePPImpl";
    private static final String MODEL_PATH = "/system/etc/face/model_file";
    private static final String PANORAMA_PATH = "/system/etc/face/panorama_mgb";
    private static final String DATA_PATH = "/data/system/face_unlock_data"; 

    private Context mContext;
    private boolean mIsInit = false;
    private int mFaceCount = 0; 

    public FacePPImpl(Context context) { mContext = context; }

    public void init() {
        synchronized (this) {
            if (mIsInit) return;
            File dir = new File(DATA_PATH);
            if (!dir.exists()) dir.mkdirs();
            
            FaceUnlockVendorImpl.getInstance().initHandle(dir.getAbsolutePath(), new CustomUnlockEncryptor());
            long res = FaceUnlockVendorImpl.getInstance().initAllWithPath(PANORAMA_PATH, "", MODEL_PATH);
            
            if (res == 0) {
                Log.i(TAG, "FacePPImpl: Initialized successfully");
                restoreFeature(); 
                mIsInit = true;
            }
        }
    }

    public void restoreFeature() {
        FaceUnlockVendorImpl.getInstance().prepare();
        int restoredCount = FaceUnlockVendorImpl.getInstance().restoreFeature();
        if (!isFeatureFilePresent()) {
            Log.w(TAG, "restoreFeature: recieved vendor code:" + restoredCount + " no face is restored.");
            mFaceCount = 0;
        } else {
            mFaceCount = restoredCount;
        }
        
        FaceUnlockVendorImpl.getInstance().reset();
    }
    public boolean hasEnrolledFaces() {
        return isFeatureFilePresent();
    }

    private boolean isFeatureFilePresent() {
        File f = new File(DATA_PATH, "feature");
        return f.exists() && f.length() > 0;
    }

    public void saveFeatureStart() {
        if (!mIsInit) init();
        FaceUnlockVendorImpl.getInstance().prepare();
    }

    public int saveFeature(byte[] img, int w, int h, int angle, boolean mirror, byte[] feature, byte[] faceData, int[] outFaceId) {
        int res = FaceUnlockVendorImpl.getInstance().saveFeature(img, w, h, angle, mirror, feature, faceData, outFaceId);
        if (res == 0) mFaceCount = 1; 
        return res;
    }

    public void saveFeatureStop() { FaceUnlockVendorImpl.getInstance().reset(); }
    
    public void compareStart() { if (!mIsInit) init(); FaceUnlockVendorImpl.getInstance().prepare(); }
    
    public int compare(byte[] img, int w, int h, int angle, boolean mirror, boolean live, int[] scores) {
        return FaceUnlockVendorImpl.getInstance().compare(img, w, h, angle, mirror, live, scores);
    }
    
    public void compareStop() { FaceUnlockVendorImpl.getInstance().reset(); }
    
    public void setDetectArea(int left, int top, int right, int bottom) {
        FaceUnlockVendorImpl.getInstance().setDetectArea(left, top, right, bottom);
    }
    
    public void deleteFeature(int id) {
        Log.w(TAG, "deleteFeature: " + id);
        FaceUnlockVendorImpl.getInstance().deleteFeature(id);
        mFaceCount = 0;
        try {
            File dir = new File(DATA_PATH);
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.getName().startsWith("restore_") || f.getName().startsWith("feature")) {
                            boolean deleted = f.delete();
                            Log.i(TAG, "Physically deleted: " + f.getName() + " Success=" + deleted);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to delete face", e);
        }
    }
}