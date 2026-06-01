package com.yanivrw.lessscreen.blocking

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

// CRC: crc-BlockOverlay.md | Seq: seq-block-detection.md | R13, R14, R15
class BlockOverlay(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var view: View? = null
    private var currentLabel: String? = null

    // Seq: seq-block-detection.md#1.7.1
    fun show(appLabel: String, onGoBack: () -> Unit) {
        if (currentLabel == appLabel) return
        hide()

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.argb(230, 0, 0, 0))
            setPadding(64, 64, 64, 64)

            addView(TextView(context).apply {
                text = "📵"
                textSize = 64f
                gravity = Gravity.CENTER
            })
            addView(TextView(context).apply {
                text = "$appLabel is blocked right now."
                textSize = 20f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(0, 32, 0, 16)
            })
            addView(TextView(context).apply {
                text = "You're doing great — stay off $appLabel!"
                textSize = 16f
                setTextColor(Color.argb(170, 255, 255, 255))
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 48)
            })
            addView(Button(context).apply {
                text = "← Go Home"
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.rgb(44, 44, 46))
                setOnClickListener { onGoBack() }
            })

            // OS back button should also go home
            isFocusableInTouchMode = true
            requestFocus()
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    onGoBack(); true
                } else false
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.START }

        windowManager.addView(root, params)
        view = root
        currentLabel = appLabel
    }

    // Seq: seq-block-detection.md#1.8.1
    fun hide() {
        view?.let { runCatching { windowManager.removeView(it) } }
        view = null
        currentLabel = null
    }
}
