package com.rcmiku.ncmapi.utils

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readBytes
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import android.util.Log
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.util.zip.GZIPInputStream
import kotlin.random.Random

object HttpManager {
    private const val BASE_URL = "https://interface.163.com"
    private const val API_BASE_URL = "https://music.163.com"
    private const val EAPI_BASE_URL = "https://interface.music.163.com"

    private const val DEFAULT_NETEASE_UA = "NeteaseMusic/3.7.01.250103035128(3007001);Dalvik/2.1.0 (Linux; U; Android 11; Redmi 6A Build/RQ3A.211001.001)"

    private const val TAG = "NcmApi"
    private const val TAG_REQ = "NcmApiReq"
    private const val TAG_RESP = "NcmApiResp"
    private const val TAG_ERR = "NcmApiErr"
    private const val TAG_HDR = "NcmApiHdr"
    private const val TAG_BODY = "NcmApiBody"
    private const val TAG_PLAIN = "NcmApiPlain"

    private const val LOG_CHUNK_SIZE = 800

    private var warnedDebugDisabled: Boolean = false

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        prettyPrint = true
    }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
        followRedirects = true
    }

    suspend fun request(
        url: String,
        method: String = "POST",
        data: Map<String, Any> = emptyMap(),
        cookies: Map<String, String> = emptyMap(),
        crypto: CryptoType = CryptoType.WEAPI,
        ua: String? = null,
        fullDump: Boolean = false,
    ): String {
        val debugEnabled = runCatching {
            val prop = System.getProperty("ncmapi.debug")
            val env = System.getenv("NCMAPI_DEBUG")
            when {
                prop != null -> prop == "true"
                env != null -> env == "true"
                else -> isHostDebugBuild()
            }
        }.getOrDefault(false)

        val fullDump = runCatching {
            val prop = System.getProperty("ncmapi.dumpFull")
            val env = System.getenv("NCMAPI_DUMP_FULL")
            prop == "true" || env == "true"
        }.getOrDefault(false)

        val eapiMinimalCookie = runCatching {
            val prop = System.getProperty("ncmapi.eapiMinimalCookie")
            val env = System.getenv("NCMAPI_EAPI_MINIMAL_COOKIE")
            when {
                prop != null -> prop != "false"
                env != null -> env != "false"
                else -> true
            }
        }.getOrDefault(true)

        val logMaxLen = if (fullDump) Int.MAX_VALUE else 50000

        val headers = mutableMapOf<String, String>()
        val defaultUa = UserAgentProvider.get().let { current ->
            if (current.startsWith("Mozilla/")) DEFAULT_NETEASE_UA else current
        }
        headers["User-Agent"] = ua ?: defaultUa
        if (crypto != CryptoType.EAPI) {
            headers["Referer"] = when (crypto) {
                CryptoType.WEAPI, CryptoType.LINUXAPI -> BASE_URL
                else -> BASE_URL
            }
        }

        if (crypto == CryptoType.EAPI) {
            headers["Accept"] = "application/json"
            headers["Accept-Charset"] = "UTF-8"
            headers["Accept-Encoding"] = "gzip"
            headers["Content-Type"] = "application/x-www-form-urlencoded; charset=UTF-8"
        }
        
        // Merge cookies
        val currentCookies = CookieProvider.get().toMutableMap()
        currentCookies.putAll(cookies)

        if (crypto == CryptoType.EAPI) {
            // Try to match official client cookie flags observed in captures.
            currentCookies.putIfAbsent("EVNSM", "1.0.0")
            currentCookies.putIfAbsent("versioncode", "3007001")
            currentCookies.putIfAbsent("buildver", "250103035128")
            currentCookies.putIfAbsent("resolution", "2269x1080")
            currentCookies.putIfAbsent("channel", "netease")
            currentCookies.putIfAbsent("os", "andrcar")
            currentCookies.putIfAbsent("modelCode", "netease")
            currentCookies.putIfAbsent("distributeChannel", "andrcar%24%7B%22channel%22%3A%22netease%22%7D")
            currentCookies.putIfAbsent("screenType", "other")
            currentCookies.putIfAbsent("appver", "3.7.01")
            currentCookies.putIfAbsent("packageType", "release")
        }
        
        // Build cookie string
        val cookieHeader = StringBuilder()
        if (crypto != CryptoType.EAPI) {
            currentCookies.getOrPut("os") { "pc" }
            currentCookies.getOrPut("appver") { "3.0.18.203152" }
            currentCookies.getOrPut("osver") { "Microsoft-Windows-10" }
            currentCookies.getOrPut("channel") { "netease" }

            currentCookies.getOrPut("versioncode") { "3007001" }
            currentCookies.getOrPut("buildver") { "250103035128" }
            currentCookies.getOrPut("resolution") { "2269x1080" }
            currentCookies.getOrPut("packageType") { "release" }
        }

        fun randomHex(len: Int): String {
            val chars = "0123456789abcdef"
            return buildString(len) {
                repeat(len) { append(chars[Random.nextInt(chars.length)]) }
            }
        }

        fun randomLower(len: Int): String {
            val chars = "abcdefghijklmnopqrstuvwxyz"
            return buildString(len) {
                repeat(len) { append(chars[Random.nextInt(chars.length)]) }
            }
        }

        currentCookies.getOrPut("_ntes_nuid") { randomHex(32) }
        currentCookies.getOrPut("_ntes_nnid") {
            val nuid = currentCookies["_ntes_nuid"] ?: randomHex(32)
            "$nuid,${System.currentTimeMillis()}"
        }
        if (crypto == CryptoType.EAPI) {
            // Match official client format observed in captures: vsfmic.1740907910255.01.4
            currentCookies.getOrPut("WNMCID") { "${randomLower(6)}.${System.currentTimeMillis()}.01.4" }
            currentCookies.getOrPut("NMCID") { currentCookies["WNMCID"] ?: "${randomLower(6)}.${System.currentTimeMillis()}.01.4" }
        } else {
            currentCookies.getOrPut("WNMCID") { "${randomHex(6)}.${System.currentTimeMillis()}.01.0" }
            currentCookies.getOrPut("NMCID") { currentCookies["WNMCID"] ?: "${randomHex(6)}.${System.currentTimeMillis()}.01.0" }
        }
        currentCookies.getOrPut("WEVNSM") { "1.0.0" }
        currentCookies.getOrPut("__remember_me") { "true" }
        currentCookies.getOrPut("ntes_kaola_ad") { "1" }
        currentCookies.getOrPut("NMTID") { randomHex(16) }

        if (!currentCookies.containsKey("MUSIC_U") && !currentCookies.containsKey("MUSIC_A")) {
            currentCookies["MUSIC_A"] = "guest_${randomHex(24)}"
        }
        
        val cookieToSend: Map<String, String> = if (crypto == CryptoType.EAPI && eapiMinimalCookie) {
            val allow = linkedSetOf(
                "EVNSM",
                "versioncode",
                "buildver",
                "resolution",
                "MUSIC_U",
                "MUSIC_A",
                "__csrf",
                "JSESSIONID-WYYY",
                "NTES_YD_SESS",
                "P_INFO",
                "S_INFO",
                "NMCID",
                "channel",
                "os",
                "modelCode",
                "distributeChannel",
                "screenType",
                "appver",
                "packageType"
            )
            currentCookies.filterKeys { allow.contains(it) }
        } else {
            currentCookies
        }

        cookieToSend.forEach { (k, v) ->
            cookieHeader.append("$k=$v; ")
        }
        headers["Cookie"] = cookieHeader.toString()

        // Match api-enhanced-main request.js behavior:
        // - WEAPI: /api/xxx is transported as /weapi/xxx
        // - EAPI:  /api/xxx is transported as /eapi/xxx (signature still uses original /api/xxx)
        val transportPath = when {
            crypto == CryptoType.WEAPI && url.startsWith("/api/") -> "/weapi/" + url.removePrefix("/api/")
            crypto == CryptoType.EAPI && url.startsWith("/api/") -> "/eapi/" + url.removePrefix("/api/")
            else -> url
        }

        val finalUrl = if (url.startsWith("http")) {
            url
        } else {
            val base = when {
                crypto == CryptoType.EAPI -> EAPI_BASE_URL
                transportPath.startsWith("/eapi") -> EAPI_BASE_URL
                transportPath.startsWith("/weapi") || transportPath.startsWith("/api") -> API_BASE_URL
                else -> BASE_URL
            }
            "$base$transportPath"
        }

        if (crypto != CryptoType.EAPI) {
            headers["Referer"] = when {
                finalUrl.startsWith(API_BASE_URL) -> API_BASE_URL
                finalUrl.startsWith(EAPI_BASE_URL) -> EAPI_BASE_URL
                else -> BASE_URL
            }
        }

        val urlHost = runCatching { io.ktor.http.Url(finalUrl).host }.getOrDefault("<unknown>")

        if (debugEnabled) {
            val cookieKeys = currentCookies.keys.sorted().joinToString(",")
            Log.d(
                TAG,
                "request method=${method.uppercase()} crypto=$crypto url=$finalUrl host=$urlHost referer=${headers["Referer"]} ua=${headers["User-Agent"]} cookieKeys=[$cookieKeys] dataKeys=${data.keys.sorted()}"
            )
        } else {
            if (!warnedDebugDisabled) {
                warnedDebugDisabled = true
                Log.w(TAG, "debug disabled (set -Dncmapi.debug=true or env NCMAPI_DEBUG=true)")
            }
        }

        // Always-on lightweight logs to make sure requests are visible in Logcat.
        Log.w(TAG_REQ, "${method.uppercase()} crypto=$crypto debug=$debugEnabled url=$finalUrl")

        val cookieKeys = currentCookies.keys.sorted().joinToString(",")
        logChunked(TAG_PLAIN, "cookieKeys=[$cookieKeys] cookieLen=${headers["Cookie"]?.length ?: 0}")

        // Plain request data (masked): helps debug EAPI/WEAPI payload mismatch.
        val plainDump = data.entries
            .sortedBy { it.key }
            .joinToString(" | ") { (k, v) ->
                val vv = maskIfSensitive(k, v.toString())
                "$k=$vv"
            }
        logChunked(TAG_PLAIN, "reqData $plainDump")

        val headerDump = headers.entries
            .sortedBy { it.key.lowercase() }
            .joinToString(" | ") { (k, v) ->
                if (k.equals("Cookie", ignoreCase = true)) {
                    if (fullDump) {
                        "$k=$v"
                    } else {
                        val prefix = if (v.length > 80) v.take(80) + "..." else v
                        "$k(len=${v.length},prefix=$prefix)"
                    }
                } else {
                    val vv = if (v.length > 200) v.take(200) + "..." else v
                    "$k=$vv"
                }
            }
        logChunked(TAG_HDR, "reqHeaders $headerDump")

        val response: HttpResponse = try {
            if (method.uppercase() == "POST") {
                val encryptedData = when (crypto) {
                    CryptoType.WEAPI -> {
                        val dataWithCsrf = data.toMutableMap()
                        val csrfToken = currentCookies["__csrf"] ?: ""
                        dataWithCsrf["csrf_token"] = csrfToken
                        CryptoUtils.weapi(toJsonString(dataWithCsrf))
                    }
                    CryptoType.LINUXAPI -> {
                        CryptoUtils.linuxapi(toJsonString(data))
                    }
                    CryptoType.EAPI -> {
                        // Important: use the original request path for EAPI signature.
                        // Using encodedPath from finalUrl may differ if host/base changes.
                        CryptoUtils.eapi(url, toJsonString(data))
                    }
                }

                val bodyDump = encryptedData.entries.joinToString(" | ") { (k, v) ->
                    if (fullDump) {
                        "$k=$v"
                    } else {
                        val prefix = if (v.length > 120) v.take(120) + "..." else v
                        "$k(len=${v.length},prefix=$prefix)"
                    }
                }
                logChunked(TAG_BODY, "reqBodyForm $bodyDump")

                if (debugEnabled) {
                    val summary = encryptedData.entries.joinToString(", ") { (k, v) ->
                        val prefix = if (v.length > 24) v.take(24) + "..." else v
                        "$k(len=${v.length},prefix=$prefix)"
                    }
                    Log.d(TAG, "cryptoPayload {$summary}")
                }

                client.post(finalUrl) {
                    headers.forEach { (k, v) -> header(k, v) }
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody(FormDataContent(Parameters.build {
                        encryptedData.forEach { (k, v) ->
                            append(k, v)
                        }
                    }))
                }
            } else {
                val qs = data.entries.joinToString("&") { (k, v) -> "$k=$v" }
                if (qs.isNotBlank()) {
                    Log.w(TAG_BODY, "reqQuery ${if (qs.length > 500) qs.take(500) + "..." else qs}")
                }
                client.get(finalUrl) {
                    headers.forEach { (k, v) -> header(k, v) }
                    data.forEach { (k, v) ->
                        url { parameters.append(k, v.toString()) }
                    }
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG_ERR, "request failed method=${method.uppercase()} crypto=$crypto url=$finalUrl", t)
            throw t
        }

        val contentEncoding = response.headers["Content-Encoding"]
        val rawBytes = runCatching { response.readBytes() }.getOrNull()
        if (rawBytes != null) {
            Log.w(TAG_RESP, "rawByteLen=${rawBytes.size} contentEncoding=$contentEncoding")
        }
        val body: String = if (rawBytes != null) {
            decodeResponseBytes(rawBytes, contentEncoding)
        } else {
            response.body()
        }

        val respHeaderDump = response.headers.entries()
            .sortedBy { it.key.lowercase() }
            .joinToString(" | ") { (k, vs) ->
                val joined = vs.joinToString(";")
                val vv = if (joined.length > 200) joined.take(200) + "..." else joined
                "$k=$vv"
            }
        logChunked(TAG_HDR, "respHeaders $respHeaderDump")

        val contentType = response.headers["Content-Type"]
        Log.w(TAG_RESP, "status=${response.status.value} url=$finalUrl contentType=$contentType len=${body.length}")
        val bodyForLog = decodeBodyForLog(body, contentEncoding, maxLen = logMaxLen)
        logChunked(TAG_BODY, "respBody $bodyForLog")

        if (debugEnabled) {
            val truncated = decodeBodyForLog(body, contentEncoding, maxLen = logMaxLen)
            Log.d(
                TAG,
                "response status=${response.status.value} url=$finalUrl contentType=$contentType len=${body.length} body=$truncated"
            )
        }
        return body
    }

    private fun logChunked(tag: String, message: String) {
        if (message.length <= LOG_CHUNK_SIZE) {
            Log.w(tag, message)
            return
        }
        var idx = 0
        var part = 0
        while (idx < message.length) {
            val end = (idx + LOG_CHUNK_SIZE).coerceAtMost(message.length)
            val chunk = message.substring(idx, end)
            Log.w(tag, "part=$part $chunk")
            idx = end
            part++
        }
    }

    private fun decodeResponseBytes(bytes: ByteArray, contentEncoding: String?): String {
        val isGzipHeader = bytes.size >= 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()
        val shouldTryGunzip = isGzipHeader || contentEncoding?.contains("gzip", ignoreCase = true) == true
        if (shouldTryGunzip) {
            return runCatching { gunzipToString(bytes) }.getOrElse {
                // fallback to ISO-8859-1 so we don't crash logging/decoding
                bytes.toString(Charsets.ISO_8859_1)
            }
        }
        return bytes.toString(Charsets.UTF_8)
    }

    private fun maskIfSensitive(key: String, value: String): String {
        val k = key.lowercase()
        val sensitive = listOf(
            "password",
            "passwd",
            "token",
            "csrf",
            "music_u",
            "music_a",
            "cookie",
            "encseckey",
            "params",
            "eparams"
        )
        if (sensitive.any { k.contains(it) }) {
            val prefix = value.take(6)
            val suffix = value.takeLast(4)
            return if (value.length <= 12) "<masked>" else "${prefix}...${suffix}(len=${value.length})"
        }
        return value
    }

    private fun decodeBodyForLog(raw: String, contentEncoding: String?, maxLen: Int): String {
        // Sometimes Ktor returns a string containing raw gzip bytes (not transparently decompressed).
        // We try to detect and decompress for readable Logcat output.
        return if (raw.length > maxLen) raw.take(maxLen) + "...<truncated>" else raw
    }

    private fun gunzipToString(bytes: ByteArray, charset: Charset = Charsets.UTF_8): String {
        GZIPInputStream(ByteArrayInputStream(bytes)).use { gis ->
            val bos = ByteArrayOutputStream()
            val buffer = ByteArray(8 * 1024)
            while (true) {
                val read = gis.read(buffer)
                if (read <= 0) break
                bos.write(buffer, 0, read)
            }
            return bos.toByteArray().toString(charset)
        }
    }

    private fun isHostDebugBuild(): Boolean {
        return runCatching {
            val candidates = listOf(
                "com.rcmiku.music.BuildConfig",
                "com.rcmiku.music.debug.BuildConfig"
            )
            for (className in candidates) {
                runCatching {
                    val clazz = Class.forName(className)
                    val field = clazz.getDeclaredField("DEBUG")
                    field.isAccessible = true
                    return@runCatching field.getBoolean(null)
                }.getOrNull()?.let { return@runCatching it }
            }
            false
        }.getOrDefault(false)
    }
    
    enum class CryptoType {
        WEAPI, LINUXAPI, EAPI
    }

    private fun toJsonString(map: Map<String, Any>): String {
        val obj = map.mapValues { (_, v) -> v.toJsonElement() }
        return json.encodeToString(JsonElement.serializer(), kotlinx.serialization.json.JsonObject(obj))
    }

    private fun Any.toJsonElement(): JsonElement = when (this) {
        is JsonElement -> this
        is String -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        else -> JsonPrimitive(this.toString())
    }
}
