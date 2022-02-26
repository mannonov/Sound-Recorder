package uz.behadllc.screenrecorder

import android.util.SparseIntArray
import android.view.Surface

object Constants {

    const val DISPLAY_WIDTH = 720
    const val DISPLAY_HEIGHT = 1280
    const val REQUEST_CODE = 712
    const val REQUEST_PERMISSION = 701
    val ORIENTATIONS = SparseIntArray().apply {
        append(Surface.ROTATION_0, 90)
        append(Surface.ROTATION_90, 0)
        append(Surface.ROTATION_180, 270)
        append(Surface.ROTATION_270, 180)
    }

}