package ax.nd.faceunlock.camera.callables;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.util.Log;
import java.lang.reflect.Method;
import ax.nd.faceunlock.camera.listeners.CameraListener;

public class StartPreviewCallable extends CameraCallable {
    private SurfaceTexture mTexture;
    private SurfaceHolder mHolder;
    private Surface mSurface;

    public StartPreviewCallable(CameraListener cameraListener) {
        super(cameraListener);
    }

    public StartPreviewCallable(SurfaceTexture texture, CameraListener cameraListener) {
        super(cameraListener);
        mTexture = texture;
    }

    public StartPreviewCallable(SurfaceHolder holder, CameraListener cameraListener) {
        super(cameraListener);
        mHolder = holder;
    }

    public StartPreviewCallable(Surface surface, CameraListener cameraListener) {
        super(cameraListener);
        mSurface = surface;
    }

    @Override
    public void run() {
        try {
            Camera camera = getCameraData().mCamera;
            if (camera != null) {
                if (mSurface != null) {
                    try {
                        Method setPreviewSurface = camera.getClass().getMethod("setPreviewSurface", Surface.class);
                        setPreviewSurface.invoke(camera, mSurface);
                        Log.d("StartPreviewCallable", "setPreviewSurface(Surface) called successfully");
                    } catch (Exception e) {
                        Log.e("StartPreviewCallable", "Failed to call setPreviewSurface", e);
                        throw e;
                    }
                } else if (mTexture != null) {
                    camera.setPreviewTexture(mTexture);
                } else if (mHolder != null) {
                    camera.setPreviewDisplay(mHolder);
                }
                
                camera.startPreview();
                
                if (getCameraListener() != null) {
                    getCameraListener().onComplete(null);
                }
            }
        } catch (Exception e) {
            if (getCameraListener() != null) {
                getCameraListener().onError(e);
            }
        }
    }
}