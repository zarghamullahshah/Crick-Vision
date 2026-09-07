package com.example.vision

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Extracts real video frames from any local video file or HTTP/HTTPS stream
 * using standard Android MediaMetadataRetriever.
 */
class VideoFrameExtractor(private val context: Context) {

    suspend fun analyzeVideo(
        videoUri: Uri,
        analyzer: RealTimeVisionAnalyzer,
        onFrameResult: (RealTimeVisionAnalyzer.FrameResult, Long, Long) -> Unit,
        sampleIntervalMs: Long = 60L // ~16-20 FPS sampling for vision analysis
    ): Boolean = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            if (videoUri.scheme == "http" || videoUri.scheme == "https") {
                retriever.setDataSource(videoUri.toString(), HashMap())
            } else {
                retriever.setDataSource(context, videoUri)
            }

            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 5000L

            analyzer.reset()

            var currentTimeMs = 0L
            val maxAnalyzeDurationMs = durationMs.coerceAtMost(30000L) // Scan up to 30 seconds

            while (currentTimeMs < maxAnalyzeDurationMs) {
                val frameUs = currentTimeMs * 1000L
                val frameBitmap: Bitmap? = try {
                    retriever.getFrameAtTime(frameUs, MediaMetadataRetriever.OPTION_CLOSEST)
                } catch (e: Exception) {
                    null
                }

                if (frameBitmap != null) {
                    val frameResult = analyzer.analyzeVideoFrame(frameBitmap, currentTimeMs)
                    frameBitmap.recycle()
                    withContext(Dispatchers.Main) {
                        onFrameResult(frameResult, currentTimeMs, durationMs)
                    }
                }

                currentTimeMs += sampleIntervalMs
            }
            true
        } catch (e: Exception) {
            Log.e("VideoFrameExtractor", "Failed to extract video frames for vision tracking", e)
            false
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
