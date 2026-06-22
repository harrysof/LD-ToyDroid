package com.ldtoypad.remote.data.remote

import com.ldtoypad.remote.core.result.AppError
import com.ldtoypad.remote.data.remote.dto.CharacterDto
import com.ldtoypad.remote.data.remote.dto.RemoteTokenDto
import com.ldtoypad.remote.data.remote.dto.VehicleDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class EmulatorHttpClient(
    private val client: OkHttpClient,
    private val json: Json,
    var baseUrl: String = ""
) {
    private val mediaType = "application/json".toMediaType()

    suspend fun getCharacterMap(): Result<List<CharacterDto>> = get("/json/charactermap.json")
    
    suspend fun getVehicleMap(): Result<List<VehicleDto>> = get("/json/tokenmap.json")
    
    suspend fun getTokens(): Result<List<RemoteTokenDto>> = get("/json/toytags.json")

    suspend fun createCharacter(id: Int): Result<Unit> = post("/character", """{"id": $id}""")

    suspend fun createVehicle(id: Int): Result<Unit> = post("/vehicle", """{"id": $id}""")

    suspend fun placeToken(uid: String, id: Int, position: Int, index: Int): Result<Unit> {
        val payload = """
            {
                "uid": "$uid",
                "id": $id,
                "position": $position,
                "index": $index
            }
        """.trimIndent()
        return post("/place", payload)
    }

    suspend fun removeToken(uid: String, index: Int): Result<Unit> {
        val payload = """
            {
                "uid": "$uid",
                "index": $index
            }
        """.trimIndent()
        return delete("/remove", payload)
    }

    private suspend inline fun <reified T> get(path: String): Result<T> = withContext(Dispatchers.IO) {
        if (baseUrl.isEmpty()) return@withContext Result.failure(AppError.State("Base URL is empty"))
        val request = Request.Builder().url("${baseUrl}$path").get().build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(AppError.Server(response.code, response.message))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(AppError.Parsing("Empty body"))
                Result.success(json.decodeFromString<T>(body))
            }
        } catch (e: IOException) {
            Result.failure(AppError.Network(e.message ?: "Network error"))
        } catch (e: Exception) {
            Result.failure(AppError.Parsing(e.message ?: "Parsing error"))
        }
    }

    private suspend fun post(path: String, jsonBody: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (baseUrl.isEmpty()) return@withContext Result.failure(AppError.State("Base URL is empty"))
        val request = Request.Builder()
            .url("${baseUrl}$path")
            .post(jsonBody.toRequestBody(mediaType))
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) Result.success(Unit)
                else Result.failure(AppError.Server(response.code, response.message))
            }
        } catch (e: IOException) {
            Result.failure(AppError.Network(e.message ?: "Network error"))
        }
    }

    private suspend fun delete(path: String, jsonBody: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (baseUrl.isEmpty()) return@withContext Result.failure(AppError.State("Base URL is empty"))
        val request = Request.Builder()
            .url("${baseUrl}$path")
            .delete(jsonBody.toRequestBody(mediaType))
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) Result.success(Unit)
                else Result.failure(AppError.Server(response.code, response.message))
            }
        } catch (e: IOException) {
            Result.failure(AppError.Network(e.message ?: "Network error"))
        }
    }
}
