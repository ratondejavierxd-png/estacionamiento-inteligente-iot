package com.example.estacionamiento

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Error devuelto por la API (por ejemplo "Credenciales inválidas"). */
class ApiError(msg: String) : IOException(msg)

/** El token expiró o no es válido: hay que volver al login. */
class SesionExpirada : IOException("Sesión expirada")

object Api {
    // IP de la Raspbian (VM). Debe coincidir con res/xml/network_security_config.xml
    const val BASE = "http://192.168.1.8:5000"

    // El token vive solo en memoria: no se guarda en el dispositivo.
    var token: String? = null

    private val http = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    private val JSON = "application/json".toMediaType()

    suspend fun request(path: String, body: JSONObject? = null): JSONObject =
        withContext(Dispatchers.IO) {
            val b = Request.Builder().url(BASE + path)
            token?.let { b.header("Authorization", "Bearer $it") }
            if (body != null) b.post(body.toString().toRequestBody(JSON))
            http.newCall(b.build()).execute().use { r ->
                val txt = r.body?.string().orEmpty()
                if (r.code == 401 && token != null) {
                    token = null
                    throw SesionExpirada()
                }
                if (!r.isSuccessful) {
                    val msg = try {
                        JSONObject(txt).optString("error", "Error ${r.code}")
                    } catch (e: Exception) {
                        "Error ${r.code}"
                    }
                    throw ApiError(msg)
                }
                JSONObject(txt)
            }
        }

    /** Texto amigable para mostrar al usuario. */
    fun mensaje(e: Exception): String = when (e) {
        is ApiError -> e.message ?: "Error"
        is IOException -> "No se pudo conectar con el servidor"
        else -> e.message ?: "Error inesperado"
    }

    fun volverAlLogin(ctx: Context) {
        token = null
        ctx.startActivity(
            Intent(ctx, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
