package ax.nd.faceunlock.camera;

import android.content.Context;
import android.hardware.Camera;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CameraUtil {

    public static int getFrontFacingCameraId(Context context) {
        return 1; 
    }

    public static Camera.Size calBestPreviewSize(Camera.Parameters parameters, final int width, final int height) {
        List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
        if (supportedPreviewSizes == null) return null;

        for (Camera.Size size : supportedPreviewSizes) {
            if (size.width == width && size.height == height) return size;
        }

        if (width == height) {
            for (Camera.Size size : supportedPreviewSizes) {
                if (size.width == size.height && size.width >= 400) {
                    return size;
                }
            }
        }

        Collections.sort(supportedPreviewSizes, (lhs, rhs) -> 
            (lhs.width * lhs.height) - (rhs.width * rhs.height));

        for (Camera.Size size : supportedPreviewSizes) {
            if (size.width >= width && size.height >= height) {
                return size;
            }
        }
        return supportedPreviewSizes.get(supportedPreviewSizes.size() - 1);
    }
}