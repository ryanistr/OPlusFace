package ax.nd.faceunlock.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import ax.nd.faceunlock.camera.listeners.CameraListener;
import ax.nd.faceunlock.camera.listeners.ErrorCallbackListener;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CameraFaceAuthController {
    private static final String TAG = "CameraFaceAuthController";
    private Context mContext;
    private Handler mHandler;
    private HandlerThread mAuthHandlerThread;
    private Handler mAuthHandler;
    private ServiceCallback mCallback;
    private boolean mIsAuthenticating = false;

    private int mWidth = 640;
    private int mHeight = 480;

    public interface ServiceCallback {
        int handlePreviewData(byte[] data, int width, int height);
        void setDetectArea(Camera.Size size);
        void onTimeout(boolean b);
        void onCameraError();
    }

    public CameraFaceAuthController(Context context, ServiceCallback callback) {
        mContext = context;
        mCallback = callback;
        mHandler = new Handler(Looper.getMainLooper());
    }

    public void start(int cameraId, SurfaceTexture dummySurface) {
        Log.d(TAG, "Starting Auth Camera...");
        if (mIsAuthenticating) stop();
        mIsAuthenticating = true;
        
        mAuthHandlerThread = new HandlerThread("face_auth_thread");
        mAuthHandlerThread.start();
        mAuthHandler = new Handler(mAuthHandlerThread.getLooper());

        CameraService.openCamera(cameraId, new ErrorCallbackListener() {
            @Override
            public void onEventCallback(int i, Object value) {
                Log.e(TAG, "Auth Camera Open Error: " + i);
                if (mCallback != null) mCallback.onCameraError();
            }
        }, new CameraListener() {
            @Override
            public void onComplete(Object value) {
                if (value instanceof Camera) {
                    Camera camera = (Camera) value;
                    
                    setupCameraParameters(camera);

                    CameraService.startPreview(dummySurface, new CameraListener() {
                        @Override
                        public void onComplete(Object value) {
                            Log.d(TAG, "Auth Preview Started (" + mWidth + "x" + mHeight + "). Attaching buffers...");
                            setupBufferedCallback(camera);
                        }
                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "Auth Preview Start Failed", e);
                            if (mCallback != null) mCallback.onCameraError();
                        }
                    });
                }
            }
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Camera open exception", e);
                if (mCallback != null) mCallback.onCameraError();
            }
        });
    }

    private void setupCameraParameters(Camera camera) {
        try {
            Camera.Parameters params = camera.getParameters();
            List<Camera.Size> supported = params.getSupportedPreviewSizes();
            
            Camera.Size bestSize = null;
            int minDiff = Integer.MAX_VALUE;
            
            if (supported != null) {
                for (Camera.Size size : supported) {
                    if (size.width == 640 && size.height == 480) {
                        bestSize = size;
                        break;
                    }
                    int diff = Math.abs((size.width * size.height) - (640 * 480));
                    if (diff < minDiff) {
                        minDiff = diff;
                        bestSize = size;
                    }
                }
            }

            if (bestSize != null) {
                mWidth = bestSize.width;
                mHeight = bestSize.height;
                params.setPreviewSize(mWidth, mHeight);
                Log.i(TAG, "Requested Camera Size: " + mWidth + "x" + mHeight);
            }
            
            params.setPreviewFormat(ImageFormat.NV21);
            
            camera.setParameters(params);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to set camera parameters", e);
        }
    }

    private void setupBufferedCallback(Camera camera) {
        try {
            Camera.Parameters params = camera.getParameters();
            Camera.Size size = params.getPreviewSize();
            mWidth = size.width;
            mHeight = size.height;

            int format = params.getPreviewFormat();
            int bitsPerPixel = ImageFormat.getBitsPerPixel(format);
            int bufferSize = (mWidth * mHeight * bitsPerPixel) / 8;
            camera.addCallbackBuffer(new byte[bufferSize]);
            camera.addCallbackBuffer(new byte[bufferSize]);
            camera.addCallbackBuffer(new byte[bufferSize]);

            CameraService.setPreviewCallback((i, obj) -> {
                if (!mIsAuthenticating || mCallback == null) return;
                
                if (obj instanceof byte[]) {
                    final byte[] data = (byte[]) obj;
                    
                    if (mAuthHandler != null) {
                        mAuthHandler.post(() -> {
                            try {
                                if (mCallback == null || !mIsAuthenticating) return;

                                mCallback.handlePreviewData(data, mWidth, mHeight);
                                
                                if (mIsAuthenticating && camera != null) {
                                    camera.addCallbackBuffer(data);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Auth loop error", e);
                            }
                        });
                    }
                }
            }, true, null);

        } catch (Exception e) {
            Log.e(TAG, "Buffer Setup Failed", e);
            if (mCallback != null) mCallback.onCameraError();
        }
    }

    public void stop() {
        Log.d(TAG, "Stopping Auth Camera");
        mIsAuthenticating = false;
        mCallback = null;
        CameraService.closeCamera(null);
        if (mAuthHandlerThread != null) {
            mAuthHandlerThread.quitSafely();
            mAuthHandlerThread = null;
        }
    }
}