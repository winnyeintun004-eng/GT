package com.galaxy.tunnel

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat

class GalaxyVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private lateinit var coreManager: VpnCoreManager

    companion object {
        private const val TAG = "GalaxyVpnService"
        const val ACTION_START = "com.galaxy.tunnel.START"
        const val ACTION_STOP = "com.galaxy.tunnel.STOP"
        const val EXTRA_CONFIG = "EXTRA_CONFIG"
        private const val CHANNEL_ID = "galaxy_tunnel_channel"
        private const val NOTIFICATION_ID = 101
    }

    override fun onCreate() {
        super.onCreate()
        // VpnCoreManager ကို လက်ရှိ Context နဲ့ ချိတ်ဆက်ပြီး ဆောက်လိုက်ခြင်း
        coreManager = VpnCoreManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val configJson = intent.getStringExtra(EXTRA_CONFIG) ?: ""
                startVpnService(configJson)
            }
            ACTION_STOP -> {
                stopVpnService()
            }
        }
        return START_NOT_STICKY
    }

    private fun startVpnService(configJson: String) {
        // ၁။ Notification ပြသခြင်း (Foreground Service အတွက် မဖြစ်မနေလိုသည်)
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GalaxyTunnel VPN")
            .setContentText("Status: Connected to the Cosmos")
            .setSmallIcon(android.R.drawable.ic_menu_share) // မိမိ app icon သို့ ပြောင်းနိုင်ပါသည်
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_VPN)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        try {
            // ၂။ TUN Interface ကို ဆောက်ပြီး ဖုန်းတစ်ခုလုံးက အင်တာနက်ဒေတာတွေကို လမ်းကြောင်းလွှဲခြင်း
            val builder = Builder()
                .setSession("GalaxyTunnel")
                .setMtu(1500)
                .addAddress("26.26.26.1", 24) // TUN Virtual IP Address
                .addRoute("0.0.0.0", 0)       // ဖုန်းတစ်ခုလုံးက ဒေတာအားလုံးကို ဖမ်းမည်
                .addDnsServer("8.8.8.8")      // Google DNS သုံးစွဲမည်
                .addDnsServer("1.1.1.1")

            vpnInterface = builder.establish()

            if (vpnInterface != null) {
                Log.i(TAG, "TUN Interface established successfully.")
                
                // ၃။ ဒေတာတွေကို ရလာပြီဆိုတာနဲ့ VpnCoreManager ကတစ်ဆင့် libv2ray + tun2socks ကို နှိုးလိုက်ခြင်း
                // သတိပြုရန် - မင်းရဲ့ .aar ပေါ်မူတည်ပြီး vpnInterface ရဲ့ file descriptor (fd) ကိုပါ Core ထဲ လှမ်းထည့်ပေးဖို့ လိုအပ်နိုင်ပါတယ်
                coreManager.startCoreEngine(configJson)
            } else {
                Log.e(TAG, "Failed to establish TUN interface.")
                stopSelf()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error starting VPN Service", e)
            stopSelf()
        }
    }

    private fun stopVpnService() {
        Log.i(TAG, "Stopping VPN Service...")
        
        // V2ray Core ကို ပိတ်ခိုင်းခြင်း
        coreManager.stopCoreEngine()

        // TUN Interface ကို ပိတ်သိမ်းခြင်း
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing TUN interface", e)
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GalaxyTunnel VPN Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stopVpnService()
        super.onDestroy()
    }
}
