package project.unibo.tankyou.data.database.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class FileDownloader {
    private val client = OkHttpClient()

    suspend fun downloadFile(context: Context, url: String, fileName: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("Failed to download file: ${response.code}"))
            }

            val body = response.body ?: return@withContext Result.failure(IOException("Response body is null"))

            val file = File(context.filesDir, fileName)
            FileOutputStream(file).use { output ->
                body.byteStream().use { input ->
                    input.copyTo(output)
                }
            }

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}