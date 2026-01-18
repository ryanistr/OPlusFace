package ax.nd.faceunlock.camera.callables;

import android.hardware.Camera;
import android.util.Log;

import ax.nd.faceunlock.camera.CameraRepository;
import ax.nd.faceunlock.camera.listeners.CameraListener;
import ax.nd.faceunlock.camera.listeners.ErrorCallbackListener;

public class OpenCameraCallable extends CameraCallable {
    private int mCameraId;
    private ErrorCallbackListener mErrorCallbackListener;

    public OpenCameraCallable(int cameraId, ErrorCallbackListener errorCallbackListener, CameraListener cameraListener) {
        super(cameraListener);
        this.mCameraId = cameraId;
        this.mErrorCallbackListener = errorCallbackListener;
    }

    @Override
    public void run() {
        try {
            CameraRepository.CameraData cameraData = getCameraData();
            
            if (cameraData.mCamera != null) {
                cameraData.mCamera.release();
                cameraData.mCamera = null;
            }

            cameraData.mCamera = Camera.open(mCameraId);
            cameraData.mCameraId = mCameraId;
            
            if (cameraData.mCameraInfo == null) {
                cameraData.mCameraInfo = new Camera.CameraInfo();
            }
            Camera.getCameraInfo(mCameraId, cameraData.mCameraInfo);

            if (getCameraListener() != null) {
                getCameraListener().onComplete(cameraData.mCamera);
            }
        } catch (Exception e) {
            Log.e("OpenCameraCallable", "Failed to open camera", e);
            if (getCameraListener() != null) {
                getCameraListener().onError(e);
            }
            if (mErrorCallbackListener != null) {
                mErrorCallbackListener.onEventCallback(1, "Camera Open Failed");
            }
        }
    }
}