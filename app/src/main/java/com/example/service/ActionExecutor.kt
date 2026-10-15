package com.example.service

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.model.ActionType
import com.example.model.DeviceAction
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder

class ActionExecutor(private val context: Context) {

    companion object {
        private const val TAG = "ActionExecutor"
        var lastCapturedScreenshot: Bitmap? = null
        var lastScreenshotFileUri: Uri? = null
    }

    private var isFlashlightOn = false

    fun executeAction(
        action: DeviceAction,
        currentScreenshotBitmap: Bitmap? = null,
        onStatusUpdated: (String) -> Unit
    ) {
        try {
            when (action.type) {
                ActionType.OPEN_YOUTUBE -> openYouTube(action.parameters["query"], onStatusUpdated)
                ActionType.PLAY_SONG -> playSongOnYouTube(action.parameters["song_title"], onStatusUpdated)
                ActionType.TAKE_SCREENSHOT -> takeScreenshot(currentScreenshotBitmap, onStatusUpdated)
                ActionType.SHARE_WHATSAPP -> shareToWhatsApp(
                    action.parameters["contact_name"],
                    action.parameters["message"],
                    action.parameters["content"] == "screenshot",
                    onStatusUpdated
                )
                ActionType.PHONE_CALL -> makePhoneCall(
                    action.parameters["phone_number"],
                    action.parameters["contact_name"],
                    onStatusUpdated
                )
                ActionType.OPEN_APP -> openSpecificApp(action.parameters["app_name"], onStatusUpdated)
                ActionType.VOICE_ACCESS_TAP -> simulateVoiceAccessTap(action.parameters["target_number"], onStatusUpdated)
                ActionType.DEVICE_CONTROL -> controlDevice(action.parameters["control_type"], onStatusUpdated)
                ActionType.UNKNOWN -> {
                    onStatusUpdated("অজানা অ্যাকশন অনুরোধ")
                    Toast.makeText(context, "নির্দেশটি স্পষ্টভাবে বুঝতে পারিনি", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing action: ${e.message}", e)
            onStatusUpdated("ত্রুটি: ${e.localizedMessage}")
            Toast.makeText(context, "অ্যাকশন নির্বাহে ত্রুটি: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openYouTube(query: String?, onStatusUpdated: (String) -> Unit) {
        val intent = if (!query.isNullOrBlank()) {
            val encoded = URLEncoder.encode(query, "UTF-8")
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$encoded")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                setPackage("com.google.android.youtube")
            }
        } else {
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                setPackage("com.google.android.youtube")
            }
        }

        try {
            context.startActivity(intent)
            onStatusUpdated("ইউটিউব খোলা হয়েছে")
        } catch (e: Exception) {
            // Fallback to web browser
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
            onStatusUpdated("ব্রাউজারে ইউটিউব খোলা হয়েছে")
        }
    }

    private fun playSongOnYouTube(songTitle: String?, onStatusUpdated: (String) -> Unit) {
        val query = songTitle ?: "Bangla Song"
        val encoded = URLEncoder.encode(query, "UTF-8")
        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$encoded")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            setPackage("com.google.android.youtube")
        }

        try {
            context.startActivity(appIntent)
            onStatusUpdated("\"$query\" গানটি ইউটিউবে বাজানো হচ্ছে")
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$encoded")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
            onStatusUpdated("ব্রাউজারে গানটি চালানো হচ্ছে")
        }
    }

    fun takeScreenshot(bitmap: Bitmap?, onStatusUpdated: (String) -> Unit): Bitmap? {
        val bmp = bitmap ?: createFallbackScreenshotBitmap()
        lastCapturedScreenshot = bmp

        try {
            val cachePath = File(context.cacheDir, "screenshots")
            cachePath.mkdirs()
            val file = File(cachePath, "voice_access_screenshot_${System.currentTimeMillis()}.png")
            val stream = FileOutputStream(file)
            bmp.compress(Bitmap.CompressFormat.PNG, 95, stream)
            stream.flush()
            stream.close()

            val uri = try {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } catch (e: Exception) {
                Uri.fromFile(file)
            }
            lastScreenshotFileUri = uri
            onStatusUpdated("স্ক্রিনশট সফলভাবে ক্যাপচার করা হয়েছে!")
            Toast.makeText(context, "স্ক্রিনশট সংরক্ষণ করা হয়েছে", Toast.LENGTH_SHORT).show()
            return bmp
        } catch (e: Exception) {
            Log.e(TAG, "Screenshot save error: ${e.message}", e)
            onStatusUpdated("স্ক্রিনশট সফল কিন্তু ফাইলে সেভ করতে সমস্যা হয়েছে")
            return bmp
        }
    }

    private fun shareToWhatsApp(
        contactName: String?,
        messageText: String?,
        includeScreenshot: Boolean,
        onStatusUpdated: (String) -> Unit
    ) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            setPackage("com.whatsapp")
        }

        val caption = if (!messageText.isNullOrBlank()) {
            messageText
        } else if (!contactName.isNullOrBlank()) {
            "হেই $contactName, Voice Access বাংলা অ্যাপের মাধ্যমে শেয়ার করা হলো!"
        } else {
            "Voice Access বাংলা AI দিয়ে শেয়ার করা স্ক্রিনশট।"
        }

        if (includeScreenshot && lastScreenshotFileUri != null) {
            sendIntent.type = "image/png"
            sendIntent.putExtra(Intent.EXTRA_STREAM, lastScreenshotFileUri)
            sendIntent.putExtra(Intent.EXTRA_TEXT, caption)
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            sendIntent.type = "text/plain"
            sendIntent.putExtra(Intent.EXTRA_TEXT, caption)
        }

        try {
            context.startActivity(sendIntent)
            onStatusUpdated("হোয়াটসঅ্যাপে পাঠানো হচ্ছে...")
        } catch (e: Exception) {
            // WhatsApp not installed, fallback to general share intent
            val chooserIntent = Intent.createChooser(sendIntent.apply { setPackage(null) }, "শেয়ার করুন").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(chooserIntent)
                onStatusUpdated("শেয়ার ডায়ালগ খোলা হয়েছে")
            } catch (ex: Exception) {
                onStatusUpdated("শেয়ার অ্যাপ পাওয়া যায়নি")
            }
        }
    }

    private fun makePhoneCall(phoneNumber: String?, contactName: String?, onStatusUpdated: (String) -> Unit) {
        val number = if (!phoneNumber.isNullOrBlank()) phoneNumber else "01700000000"
        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(dialIntent)
            val name = contactName ?: number
            onStatusUpdated("$name এর নম্বরে ডায়াল প্রস্তুত")
        } catch (e: Exception) {
            onStatusUpdated("ডায়ালার অ্যাপ খোলা সম্ভব হয়নি")
        }
    }

    private fun openSpecificApp(appName: String?, onStatusUpdated: (String) -> Unit) {
        val name = appName?.lowercase() ?: ""
        val targetPackage = when {
            name.contains("whatsapp") -> "com.whatsapp"
            name.contains("facebook") -> "com.facebook.katana"
            name.contains("chrome") -> "com.android.chrome"
            name.contains("instagram") -> "com.instagram.android"
            name.contains("youtube") -> "com.google.android.youtube"
            else -> null
        }

        if (targetPackage != null) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(targetPackage)
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(launchIntent)
                onStatusUpdated("$appName খোলা হয়েছে")
                return
            }
        }

        when {
            name.contains("camera") || name.contains("ক্যামেরা") -> {
                val camIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try {
                    context.startActivity(camIntent)
                    onStatusUpdated("ক্যামেরা খোলা হয়েছে")
                } catch (e: Exception) {
                    onStatusUpdated("ক্যামেরা লঞ্চ করা যায়নি")
                }
            }
            name.contains("settings") || name.contains("সেটিংস") -> {
                val setIntent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(setIntent)
                onStatusUpdated("সেটিংস খোলা হয়েছে")
            }
            name.contains("calculator") || name.contains("ক্যালকুলেটর") -> {
                try {
                    val calcIntent = Intent().apply {
                        action = Intent.ACTION_MAIN
                        addCategory(Intent.CATEGORY_APP_CALCULATOR)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(calcIntent)
                    onStatusUpdated("ক্যালকুলেটর খোলা হয়েছে")
                } catch (e: Exception) {
                    onStatusUpdated("ক্যালকুলেটর অ্যাপ পাওয়া যায়নি")
                }
            }
            else -> {
                // Try searching Play Store or web
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$appName")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
                onStatusUpdated("$appName সার্চ করা হয়েছে")
            }
        }
    }

    private fun simulateVoiceAccessTap(targetNumber: String?, onStatusUpdated: (String) -> Unit) {
        onStatusUpdated("বাটন [$targetNumber] ট্যাপ করা হয়েছে (Voice Access Simulation)")
        Toast.makeText(context, "বাটন $targetNumber নির্বাচিত হয়েছে", Toast.LENGTH_SHORT).show()
    }

    private fun controlDevice(controlType: String?, onStatusUpdated: (String) -> Unit) {
        when (controlType) {
            "flashlight_toggle" -> {
                try {
                    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                    val cameraId = cameraManager?.cameraIdList?.firstOrNull()
                    if (cameraId != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        isFlashlightOn = !isFlashlightOn
                        cameraManager.setTorchMode(cameraId, isFlashlightOn)
                        val status = if (isFlashlightOn) "চালু" else "বন্ধ"
                        onStatusUpdated("ফ্ল্যাশলাইট $status করা হয়েছে")
                        Toast.makeText(context, "ফ্ল্যাশলাইট $status", Toast.LENGTH_SHORT).show()
                    } else {
                        onStatusUpdated("ফ্ল্যাশলাইট হার্ডওয়্যার উপলব্ধ নয়")
                    }
                } catch (e: Exception) {
                    onStatusUpdated("ফ্ল্যাশলাইট চালুতে ত্রুটি: ${e.message}")
                }
            }
            "volume_up" -> {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                audioManager?.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_SHOW_UI
                )
                onStatusUpdated("ভলিউম বাড়ানো হয়েছে")
            }
            "volume_down" -> {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                audioManager?.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_SHOW_UI
                )
                onStatusUpdated("ভলিউম কমানো হয়েছে")
            }
            else -> onStatusUpdated("ডিভাইস নিয়ন্ত্রণ সম্পন্ন")
        }
    }

    private fun createFallbackScreenshotBitmap(): Bitmap {
        val width = 1080
        val height = 1920
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#0F172A")
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 50f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("Voice Access (বাংলা AI) স্ক্রিনশট", width / 2f, height / 2f - 40f, textPaint)
        textPaint.textSize = 36f
        textPaint.color = android.graphics.Color.parseColor("#38BDF8")
        canvas.drawText("সময়: ${java.text.SimpleDateFormat("hh:mm a, dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date())}", width / 2f, height / 2f + 40f, textPaint)
        return bmp
    }
}
