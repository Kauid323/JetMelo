package com.rcmiku.ncmapi.utils

import android.util.Base64
import android.os.Build
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URLEncoder
import java.util.UUID

val json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    prettyPrint = true
}

fun base64Encode(data: ByteArray): String {
    return Base64.encodeToString(data, Base64.NO_WRAP)
}

object FileProvider {
    private lateinit var cacheDir: File

    fun init(dir: File) {
        cacheDir = dir
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }
    
    fun getCacheDir(): File = cacheDir
}

object UserAgentProvider {
    private var userAgent: String = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"

    fun init(ua: String) {
        userAgent = ua
    }
    
    fun get(): String = userAgent
}

object CookieProvider {
    private var cookie: Map<String, String> = emptyMap()

    fun init(cookies: Map<String, String>) {
        val mutable = cookies.toMutableMap()

        if (!mutable.containsKey("deviceId")) {
            val uuid = UUID.randomUUID().toString().replace("-", "").take(16)
            val raw = "null 02:00:00:00:00:00 $uuid unknown"
            mutable["deviceId"] = URLEncoder.encode(raw, Charsets.UTF_8.name())
        }
        mutable.putIfAbsent("osver", Build.VERSION.RELEASE ?: "")
        mutable.putIfAbsent("mobilename", Build.MODEL ?: "")

        cookie = mutable
    }
    
    fun get(): Map<String, String> = cookie
}
