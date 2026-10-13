package com.unlimsync.backupengine

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

class BackupEngine(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    companion object {
        const val NAME = "BackupEngine"
        // Backend URL (use emulator host IP for Android emulator)
        const val BACKEND_BASE_URL = "http://10.0.2.2:8080/"
        // Replace with your Telegram chat ID (private channel or user)
        const val TELEGRAM_CHAT_ID = "YOUR_TELEGRAM_CHAT_ID"
    }

    private val backendService: BackendService by lazy { createBackendService() }
    private val contentResolver: ContentResolver = reactContext.contentResolver

    override fun getName(): String = NAME

    @ReactMethod
    fun startBackup(promise: Promise) {
        Log.d(NAME, "BackupEngine.startBackup called")
        // Perform work off the UI thread
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val mediaUris = queryMediaUris()
                Log.d(NAME, "Found ${mediaUris.size} media items to upload")
                for (uri in mediaUris) {
                    uploadMedia(uri)
                }
                promise.resolve("Backup completed (${mediaUris.size} items uploaded)")
            } catch (e: Exception) {
                Log.e(NAME, "Backup failed", e)
                promise.reject("BACKUP_ERROR", e)
            }
        }
    }

    /** Scan MediaStore for image and video URIs */
    private fun queryMediaUris(): List<Uri> {
        val uris = mutableListOf<Uri>()
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        // Images
        contentResolver.query(imageUri, projection, null, null, null)?.use { cursor ->
            addUrisFromCursor(cursor, imageUri, uris)
        }
        // Videos
        contentResolver.query(videoUri, projection, null, null, null)?.use { cursor ->
            addUrisFromCursor(cursor, videoUri, uris)
        }
        return uris
    }

    private fun addUrisFromCursor(cursor: Cursor, baseUri: Uri, list: MutableList<Uri>) {
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val contentUri = Uri.withAppendedPath(baseUri, id.toString())
            list.add(contentUri)
        }
    }

    /** Upload a single media file to the Spring backend */
    private fun uploadMedia(uri: Uri) {
        val inputStream = contentResolver.openInputStream(uri) ?: return
        val tempFile = java.io.File.createTempFile("media", ".tmp")
        tempFile.outputStream().use { out ->
            inputStream.copyTo(out)
        }
        val requestFile = RequestBody.create("application/octet-stream".toMediaTypeOrNull(), tempFile)
        val body = MultipartBody.Part.createFormData("file", tempFile.name, requestFile)
        val chatIdBody = RequestBody.create("text/plain".toMediaTypeOrNull(), TELEGRAM_CHAT_ID)
        val response = backendService.upload(body, chatIdBody).execute()
        if (!response.isSuccessful) {
            Log.e(NAME, "Upload failed for ${uri}: ${response.errorBody()?.string()}")
        } else {
            Log.d(NAME, "Uploaded ${uri}, telegram file_id=${response.body()}")
        }
        tempFile.delete()
    }

    private fun createBackendService(): BackendService {
        val retrofit = Retrofit.Builder()
            .baseUrl(BACKEND_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(BackendService::class.java)
    }

    interface BackendService {
        @Multipart
        @POST("api/v1/telegram/upload")
        fun upload(
            @Part file: MultipartBody.Part,
            @Part("chatId") chatId: RequestBody
        ): retrofit2.Call<String>
    }
}
