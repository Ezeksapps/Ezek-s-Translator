package com.ezeksapps.ezeksapp.camera

import android.content.Context
import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ViewModel responsible for managing camera operations using CameraX.
 * Handles camera initialization, photo capture, and file management.
 */
class CameraViewModel : ViewModel() {

    // Holds the ImageCapture use case for taking photos
    private var imageCapture: ImageCapture? = null

    /**
     * Initializes and starts the camera preview.
     *
     * @param context The Android context (typically from LocalContext.current)
     * @param previewView The PreviewView that will display the camera feed
     * @param lifecycleOwner The lifecycle owner (typically your Activity/Fragment)
     */
    fun startCamera(
        context: Context,
        previewView: PreviewView,
        lifecycleOwner: androidx.lifecycle.LifecycleOwner
    ) {
        // Get the CameraProvider future (async operation)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        // Add listener to be notified when the provider is ready
        cameraProviderFuture.addListener({
            try {
                // 1. Get the camera provider instance
                val cameraProvider = cameraProviderFuture.get()

                // 2. Build the preview use case (what user sees)
                val preview = Preview.Builder().build().apply {
                    // Connect the preview to our PreviewView surface
                    surfaceProvider = previewView.surfaceProvider
                }

                // 3. Init image capture use case (for taking photos)
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                // 4. Unbind any previous use cases first
                cameraProvider.unbindAll()

                // 5. Bind all use cases to the lifecycle
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,                // Tie to component lifecycle
                    CameraSelector.DEFAULT_BACK_CAMERA, // Use back camera
                    preview,                      // Preview use case
                    imageCapture                  // Image capture use case
                )

            } catch (e: Exception) {
                // Log camera initialization errors
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context)) // Run on main thread
    }

    /**
     * Takes a photo and returns its URI via callback.
     *
     * @param context The Android context for file operations
     * @param onCaptured Callback that receives the Uri of the captured image
     */
    fun takePhoto(
        context: Context,
        onCaptured: (Uri) -> Unit
    ) {
        // Safety check - don't proceed if imageCapture isn't ready
        val imageCapture = imageCapture ?: return

        // Launch in IO dispatcher to avoid blocking UI thread
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Create a timestamped image file
            val photoFile = createImageFile(context).also {
                // Ensure parent directory exists
                it.parentFile?.mkdirs()
            }

            // 2. Configure output options
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
                .build()

            // 3. Take the picture (async operation)
            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context), // Callback executor
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(e: ImageCaptureException) {
                        // Log capture errors
                        e.printStackTrace()
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        // Return the saved file's URI
                        onCaptured(Uri.fromFile(photoFile))
                    }
                }
            )
        }
    }

    /**
     * Creates a new image file in the app's Pictures directory.
     *
     * @param context The Android context for accessing storage
     * @return File object for the new image
     */
    private fun createImageFile(context: Context): File {
        // Generate timestamp for unique filename
        val timeStamp = SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.getDefault()
        ).format(Date())

        // Create file in app-specific Pictures directory
        return File(
            context.getExternalFilesDir("Pictures"), // Safe storage location
            "JPEG_${timeStamp}.jpg"                  // Filename format
        )
    }
}