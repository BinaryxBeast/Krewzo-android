package com.binaryxbeast.krewzo.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Standard production-grade image compression utility used in modern apps.
 * - Subsamples to avoid OutOfMemory on 12MP-108MP mobile camera captures.
 * - Preserves upright orientation by honoring EXIF metadata.
 * - Downscales to a standard max dimension (default 1024px) keeping aspect ratio.
 * - Compresses to JPEG with optimal visual quality (default 80%) to shrink 5-10MB captures to ~100-200KB.
 * - Handles camera write-flush delays and direct file access seamlessly.
 */
object ImageCompressor {

    /**
     * Direct file compression. Avoids ContentResolver stream locks on freshly captured camera files.
     */
    suspend fun compressFile(
        file: File,
        maxDimension: Int = 1024,
        quality: Int = 80
    ): ByteArray = withContext(Dispatchers.IO) {
        // Wait briefly if camera app is still writing/flushing to disk
        var attempts = 0
        while ((!file.exists() || file.length() == 0L) && attempts < 10) {
            delay(120)
            attempts++
        }

        if (!file.exists() || file.length() == 0L) {
            throw IllegalArgumentException("Captured image file is empty or missing: ${file.name}")
        }

        // 1. Read bounds without loading full pixels into RAM
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        FileInputStream(file).use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        }

        val rawWidth = boundsOptions.outWidth
        val rawHeight = boundsOptions.outHeight
        if (rawWidth <= 0 || rawHeight <= 0) {
            throw IllegalArgumentException("Invalid image dimensions in captured file")
        }

        // 2. Compute inSampleSize power of 2
        var inSampleSize = 1
        val maxRaw = max(rawWidth, rawHeight)
        while ((maxRaw / (inSampleSize * 2)) >= maxDimension) {
            inSampleSize *= 2
        }

        // 3. Decode sampled bitmap
        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val sampledBitmap = FileInputStream(file).use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: throw IllegalStateException("Failed to decode sampled bitmap from file")

        // 4. Correct EXIF Orientation
        val orientedBitmap = correctOrientation(file, sampledBitmap)

        // 5. Final smooth resize if still exceeds max dimension
        val currentMax = max(orientedBitmap.width, orientedBitmap.height)
        val finalBitmap = if (currentMax > maxDimension) {
            val scale = maxDimension.toFloat() / currentMax
            val targetWidth = (orientedBitmap.width * scale).roundToInt()
            val targetHeight = (orientedBitmap.height * scale).roundToInt()
            Bitmap.createScaledBitmap(orientedBitmap, targetWidth, targetHeight, true).also {
                if (it != orientedBitmap) orientedBitmap.recycle()
            }
        } else {
            orientedBitmap
        }

        // 6. Compress to JPEG ByteArray
        val outputStream = ByteArrayOutputStream()
        finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        finalBitmap.recycle()

        outputStream.toByteArray()
    }

    private fun correctOrientation(file: File, bitmap: Bitmap): Bitmap {
        var orientation = ExifInterface.ORIENTATION_NORMAL
        try {
            val exif = ExifInterface(file.absolutePath)
            orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } catch (_: Exception) {
            // Ignore exif read failure and retain original orientation
        }

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }

        val rotated = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )
        if (rotated != bitmap) {
            bitmap.recycle()
        }
        return rotated
    }

    /**
     * URI-based compression with automatic FileProvider resolution and retry backoff.
     */
    suspend fun compressImage(
        context: Context,
        uri: Uri,
        maxDimension: Int = 1024,
        quality: Int = 80
    ): ByteArray = withContext(Dispatchers.IO) {
        // If this URI points to our own cacheDir or FileProvider, resolve directly to File
        if (uri.scheme == "content" && uri.authority == "${context.packageName}.fileprovider") {
            val fileName = uri.lastPathSegment ?: ""
            val localFile = File(File(context.cacheDir, "camera_photos"), fileName)
            if (localFile.exists()) {
                return@withContext compressFile(localFile, maxDimension, quality)
            }
        } else if (uri.scheme == "file" && uri.path != null) {
            val localFile = File(uri.path!!)
            if (localFile.exists()) {
                return@withContext compressFile(localFile, maxDimension, quality)
            }
        }

        // Read bytes safely with retry to avoid multiple simultaneous stream opens
        var bytes: ByteArray? = null
        for (i in 0 until 5) {
            try {
                bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null && bytes.isNotEmpty()) break
            } catch (_: Exception) {}
            delay(150)
        }
        val rawBytes = bytes ?: throw IllegalArgumentException("Cannot open image stream for $uri")

        // 1. Read bounds
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, boundsOptions)

        val rawWidth = boundsOptions.outWidth
        val rawHeight = boundsOptions.outHeight
        if (rawWidth <= 0 || rawHeight <= 0) {
            throw IllegalArgumentException("Invalid image dimensions")
        }

        // 2. Compute inSampleSize power of 2
        var inSampleSize = 1
        val maxRaw = max(rawWidth, rawHeight)
        while ((maxRaw / (inSampleSize * 2)) >= maxDimension) {
            inSampleSize *= 2
        }

        // 3. Decode sampled bitmap
        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val sampledBitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOptions)
            ?: throw IllegalStateException("Failed to decode sampled bitmap")

        // 4. Correct EXIF Orientation
        var orientation = ExifInterface.ORIENTATION_NORMAL
        try {
            ByteArrayInputStream(rawBytes).use { stream ->
                val exif = ExifInterface(stream)
                orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }
        } catch (_: Exception) {}

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> {}
        }

        val orientedBitmap = if (orientation != ExifInterface.ORIENTATION_NORMAL && orientation != ExifInterface.ORIENTATION_UNDEFINED) {
            Bitmap.createBitmap(sampledBitmap, 0, 0, sampledBitmap.width, sampledBitmap.height, matrix, true).also {
                if (it != sampledBitmap) sampledBitmap.recycle()
            }
        } else {
            sampledBitmap
        }

        // 5. Final resize
        val currentMax = max(orientedBitmap.width, orientedBitmap.height)
        val finalBitmap = if (currentMax > maxDimension) {
            val scale = maxDimension.toFloat() / currentMax
            val targetWidth = (orientedBitmap.width * scale).roundToInt()
            val targetHeight = (orientedBitmap.height * scale).roundToInt()
            Bitmap.createScaledBitmap(orientedBitmap, targetWidth, targetHeight, true).also {
                if (it != orientedBitmap) orientedBitmap.recycle()
            }
        } else {
            orientedBitmap
        }

        val outputStream = ByteArrayOutputStream()
        finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        finalBitmap.recycle()

        outputStream.toByteArray()
    }
}
