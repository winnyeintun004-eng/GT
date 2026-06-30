package com.galaxy.tunnel

import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

data class VpnServer(
    val name: String,
    val configUrl: String,
    val flag: String = "🌍",
    val ping: Int = (20..150).random() // Mock ping for now
)

class MainViewModel : ViewModel() {
    private val client = OkHttpClient()

    private val _servers = MutableStateFlow<List<VpnServer>>(emptyList())
    val servers: StateFlow<List<VpnServer>> = _servers.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        // Fetch default servers on init
        fetchServers("https://raw.githubusercontent.com/Galaxy-Tunnel/ONE-AGENT/refs/heads/main/servers.txt")
    }

    fun fetchServers(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            try {
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response: Response ->
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    val body = response.body?.string() ?: ""
                    val parsedServers = parseConfigs(body)
                    _servers.value = parsedServers
                    Log.d("MainViewModel", "Fetched ${parsedServers.size} servers")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to fetch servers", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun parseConfigs(content: String): List<VpnServer> {
        val lines = try {
            // Check if base64 (common in subscription links)
            if (!content.contains("://") && !content.contains("\n")) {
                String(Base64.decode(content.trim(), Base64.DEFAULT)).lines()
            } else {
                content.lines()
            }
        } catch (e: Exception) {
            content.lines()
        }

        val serverList = mutableListOf<VpnServer>()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

            // Basic parsing for vless:// or vmess:// or trojan://
            if (trimmed.contains("://")) {
                val name = extractNameFromUrl(trimmed) ?: "Unknown Server"
                val flag = guessFlagFromName(name)
                serverList.add(VpnServer(name = name, configUrl = trimmed, flag = flag))
            }
        }
        return serverList
    }

    private fun extractNameFromUrl(url: String): String? {
        return try {
            val hashIndex = url.indexOf("#")
            if (hashIndex != -1) {
                java.net.URLDecoder.decode(url.substring(hashIndex + 1), "UTF-8")
            } else {
                "Server ${url.substring(0, url.indexOf("://")).uppercase()}"
            }
        } catch (e: Exception) {
            "Server"
        }
    }

    private fun guessFlagFromName(name: String): String {
        val lowerName = name.lowercase()
        return when {
            lowerName.contains("sg") || lowerName.contains("singapore") -> "🇸🇬"
            lowerName.contains("jp") || lowerName.contains("japan") -> "🇯🇵"
            lowerName.contains("us") || lowerName.contains("america") -> "🇺🇸"
            lowerName.contains("mm") || lowerName.contains("myanmar") -> "🇲🇲"
            lowerName.contains("th") || lowerName.contains("thai") -> "🇹🇭"
            lowerName.contains("hk") || lowerName.contains("hong") -> "🇭🇰"
            else -> "🌍"
        }
    }
}
