package ax.nd.faceunlock.camera;

import android.content.Context;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;

import ax.nd.faceunlock.camera.listeners.CameraListener;
import ax.nd.faceunlock.camera.listeners.ErrorCallbackListener;

public class CameraFaceEnrollController {
    private static final String TAG = "CameraFaceEnrollController";
    private static CameraFaceEnrollController sInstance;
    private Context mContext;
    private Handler mHandler;
    private HandlerThread mEnrollHandlerThread;
    private Handler mEnrollHandler;
    private volatile CameraCallback mCallback;
    private volatile boolean mIsEnrolling = false;

    private int mSrcWidth = 0;
    private int mSrcHeight = 0;

    private int mTargetWidth = 640;
    private int mTargetHeight = 480;
    private boolean mUseDownscale = false;
    
    private byte[] mProcessedBuffer;

    public interface CameraCallback {
        int handleSaveFeature(byte[] data, int width, int height, int angle);
        void handleSaveFeatureResult(int res);
        void onFaceDetected();
        void onTimeout();
        void onCameraError();
        void setDetectArea(Camera.Size size);
    }

    public static CameraFaceEnrollController getInstance(Context context) {
        if (sInstance == null) sInstance = new CameraFaceEnrollController(context);
        return sInstance;
    }

    public static CameraFaceEnrollController getInstance() {
        return sInstance;
    }

    private CameraFaceEnrollController(Context context) {
        mContext = context;
        mHandler = new Handler(Looper.getMainLooper());
    }

    public void start(CameraCallback callback, int cameraId, Surface previewSurface) {
        Log.d(TAG, "start() called with cameraId: " + cameraId);
        if (mIsEnrolling) stop(null);
        mIsEnrolling = true;
        mCallback = callback;
        
        mSrcWidth = 0; 
        mSrcHeight = 0;

        mEnrollHandlerThread = new HandlerThread("face_enroll_thread");
        mEnrollHandlerThread.start();
        mEnrollHandler = new Handler(mEnrollHandlerThread.getLooper());

        CameraService.openCamera(cameraId, new ErrorCallbackListener() {
            @Override
            public void onEventCallback(int i, Object value) {
                Log.e(TAG, "Camera open error: " + i);
                if (mCallback != null) mCallback.onCameraError();
            }
        }, new CameraListener() {
            @Override
            public void onComplete(Object value) {
                startConfiguredPreview(previewSurface);
            }
            @Override
            public void onError(Exception e) {
                if (mCallback != null) mCallback.onCameraError();
            }
        });
    }

    private void startConfiguredPreview(Surface surface) {
        CameraService.configureAndStartPreview(surface, new CameraListener() {
            @Override
            public void onComplete(Object value) {
                Log.d(TAG, "Preview Started. Attaching Callback...");
                attachPreviewCallback();
            }

            @Override
            public void onError(Exception e) {
                if (mCallback != null) mCallback.onCameraError();
            }
        });
    }

    private void attachPreviewCallback() {
        CameraService.setPreviewCallback((i, obj) -> {
            final CameraCallback callback = mCallback;
            if (!mIsEnrolling || callback == null) return;

            if (obj instanceof byte[]) {
                final byte[] srcData = (byte[]) obj;

                if (mSrcWidth == 0) {
                     detectSourceResolution(srcData.length);
                }
                
                if (mSrcWidth == 0) return;

                if (mEnrollHandler != null) {
                    mEnrollHandler.post(() -> {
                        try {
                            byte[] destBuffer = mProcessedBuffer;
                            
                            if (callback == null || mSrcWidth == 0 || destBuffer == null) return;
                            
                            if (mUseDownscale) {
                                downscaleNV21(srcData, mSrcWidth, mSrcHeight, destBuffer, mTargetWidth, mTargetHeight);
                            } else {
                                cropNV21(srcData, mSrcWidth, mSrcHeight, destBuffer, mTargetWidth, mTargetHeight);
                            }
                            
                            int res = callback.handleSaveFeature(destBuffer, mTargetWidth, mTargetHeight, 90);
                            callback.handleSaveFeatureResult(res);
                            
                        } catch (Exception e) {
                            Log.e(TAG, "Enroll processing error", e);
                        }
                    });
                }
            }
        }, false, null);
    }

    private void detectSourceResolution(int dataLength) {
        int pixels = (int)(dataLength / 1.5);
        int sqrt = (int) Math.sqrt(pixels);
        
        if (sqrt * sqrt == pixels) {
            mSrcWidth = sqrt;
            mSrcHeight = sqrt;
            
            if (mSrcWidth >= 800) {
                mUseDownscale = true;
                mTargetWidth = mSrcWidth / 2;
                mTargetHeight = mSrcHeight / 2;
                Log.i(TAG, "High-Res Square (" + mSrcWidth + "x" + mSrcHeight + ") -> Downscaling to " + mTargetWidth + "x" + mTargetHeight);
            } else {
                mUseDownscale = false;
                mTargetWidth = 640;
                mTargetHeight = 480;
                Log.i(TAG, "Standard Square (" + mSrcWidth + "x" + mSrcHeight + ") -> Cropping to " + mTargetWidth + "x" + mTargetHeight);
            }
        } 
        else if (pixels == 307200) { mSrcWidth = 640; mSrcHeight = 480; mUseDownscale = false; } // VGA
        else if (pixels == 921600) { mSrcWidth = 1280; mSrcHeight = 720; mUseDownscale = false; } // 720p
        else if (pixels == 2073600) { mSrcWidth = 1920; mSrcHeight = 1080; mUseDownscale = false; } // 1080p
        else {
             Log.e(TAG, "Unknown buffer size: " + dataLength);
             return;
        }
        
        mProcessedBuffer = new byte[mTargetWidth * mTargetHeight * 3 / 2];
    }

    private void downscaleNV21(byte[] src, int srcWidth, int srcHeight, byte[] dest, int dstWidth, int dstHeight) {
        for (int y = 0; y < dstHeight; y++) {
            for (int x = 0; x < dstWidth; x++) {
                dest[y * dstWidth + x] = src[(y * 2) * srcWidth + (x * 2)];
            }
        }
        
        int uvSrcStart = srcWidth * srcHeight;
        int uvDstStart = dstWidth * dstHeight;
        for (int y = 0; y < dstHeight / 2; y++) {
            for (int x = 0; x < dstWidth; x += 2) {
                int srcIndex = uvSrcStart + (y * 2) * srcWidth + x * 2;
                int dstIndex = uvDstStart + y * dstWidth + x;
                
                dest[dstIndex] = src[srcIndex];     // V
                dest[dstIndex + 1] = src[srcIndex + 1]; // U
            }
        }
    }

    private void cropNV21(byte[] src, int srcWidth, int srcHeight, byte[] dest, int dstWidth, int dstHeight) {
        if (src.length < srcWidth * srcHeight * 3 / 2) return;
        int xOffset = (srcWidth - dstWidth) / 2;
        int yOffset = (srcHeight - dstHeight) / 2;
        if (xOffset % 2 != 0) xOffset--;
        if (yOffset % 2 != 0) yOffset--;

        for (int i = 0; i < dstHeight; i++) {
            System.arraycopy(src, (yOffset + i) * srcWidth + xOffset, dest, i * dstWidth, dstWidth);
        }
        int uvSrcStart = srcWidth * srcHeight;
        int uvDstStart = dstWidth * dstHeight;
        for (int i = 0; i < dstHeight / 2; i++) {
             System.arraycopy(src, uvSrcStart + (yOffset / 2 + i) * srcWidth + xOffset, dest, uvDstStart + i * dstWidth, dstWidth);
        }
    }

    public void stop(CameraCallback callback) {
        mIsEnrolling = false;
        mCallback = null;
        CameraService.closeCamera(null);
        if (mEnrollHandlerThread != null) {
            mEnrollHandlerThread.quitSafely();
            mEnrollHandlerThread = null;
        }
        mProcessedBuffer = null;
    }
}