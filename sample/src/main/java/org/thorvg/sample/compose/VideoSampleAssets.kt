package org.thorvg.sample.compose

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.thorvg.sample.R
import java.io.File

internal suspend fun copySampleVideo(context: Context): String = withContext(Dispatchers.IO) {
    val cacheDir = context.externalCacheDir ?: context.cacheDir
    cacheDir.mkdirs()
    val file = File(cacheDir, "thorvg_middle_audio.mp4")
    val expectedSize = runCatching {
        context.resources.openRawResourceFd(R.raw.middle_audio)?.use { it.length }
    }.getOrNull() ?: -1L
    if (expectedSize > 0L && file.isFile && file.length() == expectedSize) {
        return@withContext file.absolutePath
    }

    context.resources.openRawResource(R.raw.middle_audio).use { input ->
        file.outputStream().use { output ->
            input.copyTo(output)
        }
    }
    file.absolutePath
}

internal suspend fun readSampleVideo(context: Context): ByteArray = withContext(Dispatchers.IO) {
    context.resources.openRawResource(R.raw.middle_audio).use { it.readBytes() }
}
