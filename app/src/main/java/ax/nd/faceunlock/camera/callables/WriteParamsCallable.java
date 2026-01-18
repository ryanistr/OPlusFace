package ax.nd.faceunlock.camera.callables;

import android.hardware.Camera;
import ax.nd.faceunlock.camera.listeners.CameraListener;

public class WriteParamsCallable extends CameraCallable {

    public WriteParamsCallable(CameraListener cameraListener) {
        super(cameraListener);
    }

    @Override
    public void run() {
        Camera camera = getCameraData().mCamera;
        if (camera != null) {
            try {
                Camera.Parameters params = getCameraData().mParameters;
                if (params != null) {
                    camera.setParameters(params);
                }
                
                if (getCameraListener() != null) {
                    CameraCallable.runOnUiThread(() -> {
                        if (getCameraListener() != null)
                            getCameraListener().onComplete(null);
                    });
                }
            } catch (Exception e) {
                if (getCameraListener() != null) {
                    final Exception ex = e;
                    CameraCallable.runOnUiThread(() -> {
                        if (getCameraListener() != null)
                            getCameraListener().onError(ex);
                    });
                }
            }
        }
    }
}