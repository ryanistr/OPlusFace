package ax.nd.faceunlock.camera;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.util.Log;

import ax.nd.faceunlock.FaceAuthBridge;
import ax.nd.faceunlock.camera.CameraRepository;
import ax.nd.faceunlock.camera.callables.*;
import ax.nd.faceunlock.camera.listeners.*;

public class CameraService {
    private static final int DEFAULT_MSG_TYPE = 1;
    private final Handler mServiceHandler;
    private HandlerThread mThread;

    private CameraService() {
        mThread = new HandlerThread("CameraServiceThread");
        mThread.start();
        this.mServiceHandler = new Handler(mThread.getLooper(), message -> {
            try {
                if (message.obj instanceof Runnable) {
                    ((Runnable) message.obj).run();
                }
            } catch (Throwable t) {
                Log.e("CameraService", "Error in CameraService thread", t);
            }
            return true;
        });
    }

    private static CameraService getInstance() {
        return LazyLoader.INSTANCE;
    }

    public static void openCamera(int i, ErrorCallbackListener errorCallbackListener, CameraListener cameraListener) {
        getInstance().addCallable(new OpenCameraCallable(i, errorCallbackListener, cameraListener));
    }

    public static void closeCamera(CameraListener cameraListener) {
        getInstance().mServiceHandler.removeMessages(DEFAULT_MSG_TYPE);
        getInstance().addCallable(new CloseCameraCallable(cameraListener));
    }

    public static void configureAndStartPreview(Surface surface, CameraListener cameraListener) {
        getInstance().addCallable(new ConfigureAndStartPreviewCallable(surface, cameraListener));
    }

    public static void startPreview(CameraListener cameraListener) {
        try {
            if (FaceAuthBridge.getInstance() != null) {
                SurfaceTexture dummy = FaceAuthBridge.getInstance().getDummySurface();
                if (dummy != null) {
                    getInstance().addCallable(new StartPreviewCallable(dummy, cameraListener));
                    return;
                }
            }
        } catch (Throwable e) {}
        getInstance().addCallable(new StartPreviewCallable(cameraListener));
    }

    public static void startPreview(SurfaceTexture surfaceTexture, CameraListener cameraListener) {
        getInstance().addCallable(new StartPreviewCallable(surfaceTexture, cameraListener));
    }

    public static void startPreview(SurfaceHolder surfaceHolder, CameraListener cameraListener) {
        getInstance().addCallable(new StartPreviewCallable(surfaceHolder, cameraListener));
    }

    public static void stopPreview(CameraListener cameraListener) {
        getInstance().addCallable(new StopPreviewCallable(cameraListener));
    }

    public static void addCallbackBuffer(byte[] bArr, CameraListener cameraListener) {
        getInstance().addCallable(new AddCallbackBufferCallable(bArr, cameraListener));
    }

    public static void setPreviewCallback(ByteBufferCallbackListener byteBufferCallbackListener, boolean z, CameraListener cameraListener) {
        getInstance().addCallable(new SetPreviewCallbackCallable(byteBufferCallbackListener, z, cameraListener));
    }

    public static void setFaceDetectionCallback(Camera.FaceDetectionListener faceDetectionListener, CameraListener cameraListener) {
        getInstance().addCallable(new SetFaceDetectionCallback(faceDetectionListener, cameraListener));
    }

    public static void setDisplayOrientationCallback(int i, CameraListener cameraListener) {
        getInstance().addCallable(new SetDisplayOrientationCallback(i, cameraListener));
    }

    public static void clearQueue() {
        getInstance().mServiceHandler.removeMessages(DEFAULT_MSG_TYPE);
    }

    private void addCallable(Runnable cameraCallable) {
        this.mServiceHandler.sendMessage(this.mServiceHandler.obtainMessage(DEFAULT_MSG_TYPE, cameraCallable));
    }

    public static void autoFocus(boolean z, FocusResultListener l, CameraListener c) { getInstance().addCallable(new AutoFocusCallable(z, l, c)); }
    public static void readParameters(ReadParametersListener r, CameraListener c) { getInstance().addCallable(new ReadParamsCallable(r, c)); }
    public static void writeParameters(CameraListener c) { getInstance().addCallable(new WriteParamsCallable(c)); }

    private static final class LazyLoader {
        private static final CameraService INSTANCE = new CameraService();
    }
}