package com.show.qrscanx

import android.content.Context
import android.util.DisplayMetrics
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 *  com.show.qrscanx
 *  2021/1/4
 *  22:05
 *  ShowMeThe
 */
class QrCodeDecode constructor(private val context: Context, private val type: DecodeType) {

    companion object {
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }

    private val cameraExecutor by lazy {
        Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors() * 2 + 1
        )
    }
    private var cameraProvider: ProcessCameraProvider? = null
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private lateinit var previewView: PreviewView
    private lateinit var lifecycleOwner: LifecycleOwner
    private var preview: Preview? = null
    private var camera: Camera? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private val channel = BroadcastChannel<Result>(Channel.BUFFERED)
    private val listeners = ArrayList<WeakReference<ScanningResultListener>>()
    private var debounce: Long? = null
    private val defaultBarcodeFormat = arrayListOf(BarcodeFormat.QR_CODE)


    fun debounce(debounce: Long): QrCodeDecode {
        this.debounce = debounce
        return this
    }

    fun barcodeFormat(vararg formats: BarcodeFormat): QrCodeDecode {
        defaultBarcodeFormat.addAll(formats)
        return this
    }

    fun bind(previewView: PreviewView, lifecycleOwner: LifecycleOwner): QrCodeDecode {
        this.previewView = previewView
        this.lifecycleOwner = lifecycleOwner
        previewView.post {
            setUpCamera()
        }
        return this
    }

    fun addListener(listener: ScanningResultListener): QrCodeDecode {
        listeners.add(WeakReference(listener))
        return this
    }

    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(Runnable {

            cameraProvider = cameraProviderFuture.get()


            lensFacing = when {
                hasBackCamera() -> CameraSelector.LENS_FACING_BACK
                else -> throw IllegalStateException("Back and front camera are unavailable")
            }

            // Build and bind the camera use cases
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(context))

    }

    private fun bindCameraUseCases() {

        val metrics = DisplayMetrics().also { previewView.display.getRealMetrics(it) }

        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)


        val rotation = previewView.display.rotation


        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()



        preview = Preview.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()


        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, QRCodeAnalyzer())
            }

        GlobalScope.launch(Dispatchers.Main) {
            channel
                .asFlow()
                .apply {
                    when (type) {
                        DecodeType.SINGLE() -> distinctUntilChanged { old, new -> old.text == new.text }
                        DecodeType.CONTINUES() -> debounce(debounce ?: type.debounce)
                    }
                }
                .collect { result ->
                    listeners.forEach {
                        it.get()?.invoke(result)
                    }
                }
        }

        cameraProvider.unbindAll()

        try {
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner, cameraSelector, preview, imageAnalyzer
            )
            preview?.setSurfaceProvider(previewView.surfaceProvider)
        } catch (exc: Exception) {
        }
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    private fun hasBackCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
    }

    private inner class QRCodeAnalyzer() : ImageAnalysis.Analyzer {

        private val reader: MultiFormatReader = MultiFormatReader()

        init {
            val map = mapOf<DecodeHintType, Collection<BarcodeFormat>>(
                Pair(DecodeHintType.POSSIBLE_FORMATS, defaultBarcodeFormat)
            )
            reader.setHints(map)
        }

        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()
            val data = ByteArray(remaining())
            get(data)
            return data
        }

        override fun analyze(image: ImageProxy) {
            val buffer = image.planes[0].buffer
            val data = buffer.toByteArray()
            val height = image.height
            val width = image.width
            val source = PlanarYUVLuminanceSource(data, width, height, 0, 0, width, height, false)
            val bitmap = BinaryBitmap(HybridBinarizer(source))
            try {
                val result = reader.decode(bitmap)
                runBlocking {
                    channel.send(result)
                    image.close()
                }
            } catch (e: Exception) {
                image.close()
            }

        }

    }

}