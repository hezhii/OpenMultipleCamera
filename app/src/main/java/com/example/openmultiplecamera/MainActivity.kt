package com.example.openmultiplecamera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private val cameraIds = arrayOf("1", "3", "5")
    private val textureViews = arrayOfNulls<TextureView>(cameraIds.size)
    private val cameraDevices = arrayOfNulls<CameraDevice>(cameraIds.size)
    private val captureSessions = arrayOfNulls<CameraCaptureSession>(cameraIds.size)

    private lateinit var cameraManager: CameraManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager

        textureViews[0] = findViewById(R.id.textureView1)
        textureViews[1] = findViewById(R.id.textureView2)
        textureViews[2] = findViewById(R.id.textureView3)

        for (i in cameraIds.indices) {
            textureViews[i]?.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(
                    surfaceTexture: SurfaceTexture, width: Int, height: Int
                ) {
                    openCamera(cameraIds[i], surfaceTexture, i)
                }

                override fun onSurfaceTextureSizeChanged(
                    surfaceTexture: SurfaceTexture, width: Int, height: Int
                ) {
                }

                override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean =
                    true

                override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {}
            }
        }
    }

    private fun openCamera(cameraId: String, surfaceTexture: SurfaceTexture, index: Int) {
        Log.i(TAG, "open camera:" + cameraId)
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 0)
            return
        }

        try {
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevices[index] = camera
                    createCameraPreviewSession(camera, surfaceTexture, index)
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cameraDevices[index] = null
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    cameraDevices[index] = null
                }
            }, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun createCameraPreviewSession(
        camera: CameraDevice, surfaceTexture: SurfaceTexture, index: Int
    ) {
        try {
            val surface = Surface(surfaceTexture)
            val captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder.addTarget(surface)

            camera.createCaptureSession(
                listOf(surface), object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        if (cameraDevices[index] == null) return
                        captureSessions[index] = session
                        try {
                            session.setRepeatingRequest(captureRequestBuilder.build(), null, null)
                        } catch (e: CameraAccessException) {
                            e.printStackTrace()
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {}
                }, null
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        for (camera in cameraDevices) {
            camera?.close()
        }
    }
}