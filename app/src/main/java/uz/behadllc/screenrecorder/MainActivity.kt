package uz.behadllc.screenrecorder

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.DisplayMetrics
import android.view.View
import android.widget.ToggleButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import uz.behadllc.screenrecorder.databinding.ActivityMainBinding
import java.lang.Exception
import java.lang.StringBuilder
import java.security.spec.ECField
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private lateinit var mediaRecorder: MediaRecorder
    private lateinit var virtualDisplay: VirtualDisplay
    private lateinit var mediaProjectionCallBack: MediaProjectionCallBack

    private var screenDensity = 0
    private var videoUri = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        screenDensity = metrics.densityDpi

        mediaRecorder = MediaRecorder()
        mediaProjectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        binding.btnStartStopRecord.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this@MainActivity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                ) {
                    binding.btnStartStopRecord.isChecked = false
                    Snackbar.make(binding.root, "Permissions", Snackbar.LENGTH_INDEFINITE)
                        .setAction("ENABLE"
                        ) {
                            ActivityCompat.requestPermissions(this@MainActivity,
                                listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE).toTypedArray(),
                                Constants.REQUEST_PERMISSION)
                        }.show()
                } else {
                    ActivityCompat.requestPermissions(this@MainActivity,
                        listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE).toTypedArray(),
                        Constants.REQUEST_PERMISSION)
                }
            } else {
                screenShare(it)
            }
        }

    }

    private fun screenShare(view: View) {
        if ((view as ToggleButton).isChecked) {
            initRecorder()
            recordScreen()
        } else {
            mediaRecorder.stop()
            mediaRecorder.reset()
            stopRecordScreen()
        }
    }

    private fun recordScreen() {

        if (mediaProjection == null) {
            launcher.launch(mediaProjectionManager.createScreenCaptureIntent())
            return
        }

        virtualDisplay = createVirtualDisplay()

    }

    private fun createVirtualDisplay(): VirtualDisplay {
        return mediaProjection!!.createVirtualDisplay("MainActivity",
            Constants.DISPLAY_WIDTH,
            Constants.DISPLAY_HEIGHT,
            screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder.surface, null, null)
    }

    private fun initRecorder() {
        try {

            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

            videoUri =
                "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)}/${
                    StringBuilder().append(SimpleDateFormat("JAKHA_RECORD_dd-MM-yyyy-hh_mm_ss").format(
                        Date()))
                }.mp4"

            mediaRecorder.setOutputFile(videoUri)
            mediaRecorder.setVideoSize(Constants.DISPLAY_WIDTH, Constants.DISPLAY_HEIGHT)
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            mediaRecorder.setVideoEncodingBitRate(512 * 1000)
            mediaRecorder.setVideoFrameRate(30)

            val rotation = windowManager.defaultDisplay.rotation
            val orientation = Constants.ORIENTATIONS.get(rotation * 90)

            mediaRecorder.setOrientationHint(orientation)
            mediaRecorder.prepare()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                mediaProjectionCallBack = MediaProjectionCallBack()
                mediaProjection =
                    mediaProjectionManager.getMediaProjection(it.resultCode, it.data!!)
                mediaProjection!!.registerCallback(mediaProjectionCallBack, null)
                virtualDisplay = createVirtualDisplay()
            }
        }

    inner class MediaProjectionCallBack : MediaProjection.Callback() {
        override fun onStop() {
            super.onStop()
            if (binding.btnStartStopRecord.isChecked) {
                binding.btnStartStopRecord.isChecked = false
                mediaRecorder.stop()
                mediaRecorder.reset()
            }
            mediaProjection = null
            stopRecordScreen()
        }
    }

    fun stopRecordScreen() {
        if (virtualDisplay == null) return
        virtualDisplay.release()
        destroyMediaProjection()
    }

    fun destroyMediaProjection() {
        if (mediaProjection != null) {
            mediaProjection!!.unregisterCallback(mediaProjectionCallBack)
            mediaProjection!!.stop()
            mediaProjection = null
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            Constants.REQUEST_PERMISSION -> {
                if ((grantResults.isNotEmpty()) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    screenShare(binding.btnStartStopRecord)
                } else {
                    binding.btnStartStopRecord.isChecked = false
                    Snackbar.make(binding.root, "Permissions", Snackbar.LENGTH_INDEFINITE)
                        .setAction("ENABLE"
                        ) {
                            ActivityCompat.requestPermissions(this@MainActivity,
                                listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE).toTypedArray(),
                                Constants.REQUEST_PERMISSION)
                        }.show()
                }
                return
            }
        }
    }


}