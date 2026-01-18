package ax.nd.faceunlock.camera.callables;

import android.hardware.Camera;
import android.view.Surface;
import android.util.Log;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Comparator;
import ax.nd.faceunlock.camera.CameraRepository;
import ax.nd.faceunlock.camera.listeners.CameraListener;

public class ConfigureAndStartPreviewCallable extends CameraCallable {
    private static final String TAG = "ConfigStartCallable";
    private final Surface mSurface;

    public ConfigureAndStartPreviewCallable(Surface surface, CameraListener cameraListener) {
        super(cameraListener);
        mSurface = surface;
    }

    @Override
    public void run() {
        try {
            Camera camera = getCameraData().mCamera;
            if (camera == null) {
                Log.e(TAG, "Camera is null, cannot start preview");
                if (getCameraListener() != null) getCameraListener().onError(new Exception("Camera null"));
                return;
            }

            Log.d(TAG, "Configuring Camera...");

            try {
                camera.setDisplayOrientation(90);
                Log.d(TAG, "Set Display Orientation: 90");
            } catch (Exception e) {
                Log.w(TAG, "Failed to set orientation", e);
            }

            try {
                Camera.Parameters params = camera.getParameters();
                Camera.Size bestSize = findBestSize(params);
                if (bestSize != null) {
                    Log.d(TAG, "Setting Preview Size: " + bestSize.width + "x" + bestSize.height);
                    params.setPreviewSize(bestSize.width, bestSize.height);
                    camera.setParameters(params);
                }
                getCameraData().mParameters = camera.getParameters();
            } catch (Exception e) {
                Log.e(TAG, "Failed to configure parameters", e);
            }

            if (mSurface != null) {
                try {
                    Method setPreviewSurface = camera.getClass().getMethod("setPreviewSurface", Surface.class);
                    setPreviewSurface.invoke(camera, mSurface);
                    Log.d(TAG, "setPreviewSurface(Surface) called successfully");
                } catch (Exception e) {
                    Log.e(TAG, "Failed to call setPreviewSurface (Reflection)", e);
                }
            }

            camera.startPreview();
            Log.d(TAG, "startPreview() called");

            if (getCameraListener() != null) {
                CameraCallable.runOnUiThread(() -> getCameraListener().onComplete(null));
            }

        } catch (Exception e) {
            Log.e(TAG, "Critical failure in ConfigureAndStartPreview", e);
            if (getCameraListener() != null) {
                final Exception err = e;
                CameraCallable.runOnUiThread(() -> getCameraListener().onError(err));
            }
        }
    }

    private Camera.Size findBestSize(Camera.Parameters params) {
        List<Camera.Size> sizes = params.getSupportedPreviewSizes();
        if (sizes == null) return null;

        Collections.sort(sizes, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size a, Camera.Size b) {
                return (b.width * b.height) - (a.width * a.height);
            }
        });

        for (Camera.Size s : sizes) {
            if (s.width == s.height && s.width >= 480) {
                return s;
            }
        }

        for (Camera.Size s : sizes) {
            if (s.width == 640 && s.height == 480) return s;
        }

        return sizes.get(0);
    }
}