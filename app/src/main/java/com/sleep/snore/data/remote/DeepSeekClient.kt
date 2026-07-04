package com.sleep.snore.data.remote

import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class DeepSeekConfig(
    val apiKey: String,
    val baseUrl: String,
    val modelName: String
) {
    val isConfigured: Boolean
        get() = apiKey.isNotBlank() && baseUrl.isNotBlank() && modelName.isNotBlank()
}

@Singleton
class DeepSeekClient @Inject constructor() {

    suspend fun analyze(prompt: String, config: DeepSeekConfig): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            require(config.isConfigured) { "DeepSeek 配置不完整" }
            val connection = (URL(config.baseUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                doOutput = true
                setRequestProperty("Authorization", "Bearer ${config.apiKey}")
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
            }

            val body = JSONObject()
                .put("model", config.modelName)
                .put("temperature", 0.3)
                .put("messages", JSONArray().apply {
                    put(
                        JSONObject()
                            .put("role", "system")
                            .put("content", "你是谨慎的睡眠健康分析助手，只做健康建议，不做医学诊断。")
                    )
                    put(JSONObject().put("role", "user").put("content", prompt))
                })

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(body.toString())
            }

            val responseText = if (connection.responseCode in 200..299) {
                connection.inputStream.bufferedReader(Charsets.UTF_8).use(BufferedReader::readText)
            } else {
                val error = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use(BufferedReader::readText).orEmpty()
                error("DeepSeek 请求失败 HTTP ${connection.responseCode}: $error")
            }

            JSONObject(responseText)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 45_000
    }
}
