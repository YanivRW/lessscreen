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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// CRC: crc-BlockOverlay.md | Seq: seq-block-detection.md, seq-friend-lock-overlay.md
// R13, R14, R15, R30, R31, R42
class BlockOverlay(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var view: View? = null
    private var currentLabel: String? = null

    // Seq: seq-block-detection.md#1.7.1 | R13, R14, R15
    fun show(appLabel: String, onGoHome: () -> Unit) {
        if (currentLabel == appLabel && view != null) return
        hide()
        val root = buildRoot(appLabel) {
            addGoHomeButton(it, onGoHome)
        }
        attach(root)
        currentLabel = appLabel
    }

    // Seq: seq-friend-lock-overlay.md#1.5 | R30, R31, R42
    // onVerifyPin: check-only RPC (no state change); onUnlock: called with (pin, durationMinutes)
    fun showLocked(
        appLabel: String,
        lockPartnerName: String,
        scope: CoroutineScope,
        onGoHome: () -> Unit,
        onVerifyPin: suspend (String) -> Boolean,
        onUnlock: suspend (pin: String, durationMinutes: Int) -> Unit,
    ) {
        if (currentLabel == appLabel && view != null) return
        hide()

        val root = buildRoot(appLabel) { container ->
            // "Locked by [Friend]" label
            container.addView(TextView(context).apply {
                text = "🔒 Locked by $lockPartnerName"
                textSize = 16f
                setTextColor(Color.argb(200, 255, 255, 255))
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 24)
            })

            // 6-digit PIN display
            val pinDigits = Array(6) { "" }
            val pinDisplay = TextView(context).apply {
                text = "_ _ _ _ _ _"
                textSize = 24f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                letterSpacing = 0.3f
            }
            container.addView(pinDisplay)

            // Error label (hidden by default)
            val errorLabel = TextView(context).apply {
                text = "Incorrect code"
                textSize = 14f
                setTextColor(Color.rgb(255, 80, 80))
                gravity = Gravity.CENTER
                setPadding(0, 8, 0, 0)
                visibility = View.INVISIBLE
            }
            container.addView(errorLabel)

            // Number pad (1-9, backspace, 0)
            val numPad = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 16, 0, 0)
            }

            fun refreshDisplay() {
                val filled = pinDigits.joinToString(" ") { if (it.isEmpty()) "_" else "•" }
                pinDisplay.text = filled
                errorLabel.visibility = View.INVISIBLE
            }

            var verifiedPin: String? = null

            fun onDigit(d: String) {
                val idx = pinDigits.indexOfFirst { it.isEmpty() }
                if (idx < 0) return
                pinDigits[idx] = d
                refreshDisplay()
                if (pinDigits.none { it.isEmpty() }) {
                    val pin = pinDigits.joinToString("")
                    scope.launch {
                        val ok = onVerifyPin(pin)
                        if (ok) {
                            verifiedPin = pin
                            showDurationPicker(container, numPad, pinDisplay, errorLabel) { mins ->
                                scope.launch { onUnlock(pin, mins) }
                            }
                        } else {
                            pinDigits.fill("")
                            refreshDisplay()
                            errorLabel.visibility = View.VISIBLE
                        }
                    }
                }
            }

            fun onBack() {
                val idx = (pinDigits.indices.lastOrNull { pinDigits[it].isNotEmpty() }) ?: return
                pinDigits[idx] = ""
                refreshDisplay()
            }

            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("⌫", "0", ""),
            )
            rows.forEach { row ->
                val rowLayout = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                }
                row.forEach { label ->
                    if (label.isEmpty()) {
                        rowLayout.addView(View(context).apply {
                            layoutParams = LinearLayout.LayoutParams(0, 80).apply { weight = 1f }
                        })
                    } else {
                        rowLayout.addView(Button(context).apply {
                            text = label
                            textSize = 20f
                            setTextColor(Color.WHITE)
                            setBackgroundColor(Color.argb(80, 80, 80, 80))
                            layoutParams = LinearLayout.LayoutParams(0, 120).apply {
                                weight = 1f
                                setMargins(4, 4, 4, 4)
                            }
                            setOnClickListener {
                                if (label == "⌫") onBack() else onDigit(label)
                            }
                        })
                    }
                }
                numPad.addView(rowLayout)
            }
            container.addView(numPad)

            // Go Home button always available
            container.addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 24
                )
            })
            addGoHomeButton(container, onGoHome)
        }

        // Handle OS back key
        root.isFocusableInTouchMode = true
        root.requestFocus()
        root.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                onGoHome(); true
            } else false
        }

        attach(root)
        currentLabel = appLabel
    }

    // Seq: seq-block-detection.md#1.8.1
    fun hide() {
        view?.let { runCatching { windowManager.removeView(it) } }
        view = null
        currentLabel = null
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private fun buildRoot(appLabel: String, addChildren: (LinearLayout) -> Unit): LinearLayout {
        return LinearLayout(context).apply {
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
                setPadding(0, 0, 0, 32)
            })
            addChildren(this)
        }
    }

    private fun addGoHomeButton(container: LinearLayout, onGoHome: () -> Unit) {
        container.addView(Button(context).apply {
            text = "← Go Home"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(44, 44, 46))
            setOnClickListener { onGoHome() }
        })
    }

    // Seq: seq-friend-lock-overlay.md#1.16 | R31
    private fun showDurationPicker(
        container: LinearLayout,
        numPad: LinearLayout,
        pinDisplay: TextView,
        errorLabel: TextView,
        onDurationSelected: (Int) -> Unit,
    ) {
        // Replace pin pad with duration picker
        container.removeView(numPad)
        pinDisplay.text = "✓ Code accepted"
        errorLabel.visibility = View.INVISIBLE

        val pickerContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 24, 0, 0)
        }

        val prompt = TextView(context).apply {
            text = "Unlock for how long?"
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }
        pickerContainer.addView(prompt)

        val durations = listOf("30 minutes" to 30, "1 hour" to 60, "2 hours" to 120, "Until re-locked" to -1)
        durations.forEach { (label, mins) ->
            pickerContainer.addView(Button(context).apply {
                text = label
                textSize = 16f
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.rgb(44, 80, 44))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { setMargins(0, 8, 0, 0) }
                setOnClickListener { onDurationSelected(mins) }
            })
        }

        // Insert before the Go Home button (which is the last child)
        container.addView(pickerContainer, container.childCount - 1)
    }

    private fun attach(root: LinearLayout) {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.START }
        windowManager.addView(root, params)
        view = root
    }
}
