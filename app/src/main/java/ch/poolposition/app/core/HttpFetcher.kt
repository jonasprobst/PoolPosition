package ch.poolposition.app.core

import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream

/**
 * Plain HTTP GET using java.net (no OkHttp). Returns the response body as text
 * or an error message; never throws. HttpURLConnection handles redirects and,
 * with the Accept-Encoding below, gzip is decoded here.
 */
object HttpFetcher {

    private const val USER_AGENT =
        "Mozilla/5.0 (Android; PoolPosition) AppleWebKit/537.36 (KHTML, like Gecko)"

    data class FetchResult(val ok: Boolean, val body: String?, val error: String?)

    fun get(url: String, timeoutMs: Int = 15_000): FetchResult {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "text/html,application/xhtml+xml,*/*")
                setRequestProperty("Accept-Encoding", "gzip")
            }
            val code = connection.responseCode
            if (code !in 200..299) {
                return FetchResult(ok = false, body = null, error = "HTTP $code")
            }
            val charset = charsetFrom(connection.contentType)
            val stream = if (connection.contentEncoding.equals("gzip", ignoreCase = true)) {
                GZIPInputStream(connection.inputStream)
            } else {
                connection.inputStream
            }
            val body = stream.bufferedReader(charset).use(BufferedReader::readText)
            FetchResult(ok = true, body = body, error = null)
        } catch (e: Exception) {
            FetchResult(ok = false, body = null, error = e.message ?: e.javaClass.simpleName)
        } finally {
            connection?.disconnect()
        }
    }

    private fun charsetFrom(contentType: String?): java.nio.charset.Charset {
        val name = contentType
            ?.substringAfter("charset=", "")
            ?.substringBefore(';')
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        return runCatching { java.nio.charset.Charset.forName(name) }
            .getOrDefault(Charsets.UTF_8)
    }
}
