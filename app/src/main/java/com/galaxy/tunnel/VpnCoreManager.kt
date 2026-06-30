package com.galaxy.tunnel

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
// .aar ဖိုင်ထည့်ပြီးပြီဖြစ်လို့ ဒီကောင်ကို Import လုပ်လို့ရပါပြီ
import libv2ray.Libv2ray

class VpnCoreManager(private val context: Context) {

    companion object {
        private const val TAG = "VpnCoreManager"
        private const val GEOIP_FILENAME = "geoip.dat"
        private const val GEOSITE_FILENAME = "geosite.dat"
    }

    /**
     * Initializes the core assets required by the V2Ray/Xray engine.
     * Copies geoip.dat and geosite.dat from the assets folder to the app's internal filesDir.
     */
    fun initializeCoreAssets() {
        copyAssetToFileDir(GEOIP_FILENAME)
        copyAssetToFileDir(GEOSITE_FILENAME)
    }

    private fun copyAssetToFileDir(filename: String) {
        val targetFile = File(context.filesDir, filename)
        if (targetFile.exists()) {
            Log.d(TAG, "$filename already exists at ${targetFile.absolutePath}")
            return
        }

        try {
            context.assets.open(filename).use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    val buffer = ByteArray(1024)
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                    }
                    outputStream.flush()
                }
            }
            Log.d(TAG, "Successfully copied $filename to ${targetFile.absolutePath}")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to copy $filename from assets to filesDir", e)
        }
    }

    /**
     * Functional structure to bridge with the libv2ray .aar external library.
     * Starts the VPN core engine with a given configuration.
     * * @param configJson The actual V2Ray JSON configuration string.
     * @return true if the core started successfully, false otherwise.
     */
    fun startCoreEngine(configJson: String): Boolean {
        Log.i(TAG, "Starting VPN Core Engine...")
        val configPath = context.filesDir.absolutePath
        
        return try {
            // တကယ့် .aar ထဲက ပတ်ဝန်းကျင်နဲ့ Core စတင်တဲ့ ဖန်ရှင်တွေကို လှမ်းခေါ်လိုက်ပါပြီ
            Libv2ray.initV2Env(configPath)
            Libv2ray.startCore(configJson)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start core engine", e)
            false
        }
    }

    /**
     * Functional structure to stop the libv2ray engine.
     */
    fun stopCoreEngine() {
        Log.i(TAG, "Stopping VPN Core Engine")
        try {
            // .aar ထဲက ရပ်တန့်တဲ့ ဖန်ရှင်ကို လှမ်းခေါ်ခြင်း
            Libv2ray.stopCore()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop core engine", e)
        }
    }
    
    /**
     * Functional structure to check the status of the libv2ray engine.
     */
    fun isCoreRunning(): Boolean {
        return try {
            // Core တကယ် လည်ပတ်နေသလား စစ်ဆေးခြင်း
            Libv2ray.isRunning()
        } catch (e: Exception) {
            false
        }
    }
}
