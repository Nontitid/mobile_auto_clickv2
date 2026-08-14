package com.example.autoclicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Floating auto-clicker.
 *
 * The user drags two dots ("Point A" ~ middle-left, "Point B" ~ top-left) to the
 * exact spots that should be tapped. Pressing GREEN runs [A tap, B tap] x 170.
 * Pressing RED runs [A tap, B tap] x 230. STOP cancels an in-progress run.
 */
class ClickAccessibilityService : AccessibilityService() {

    private lateinit var windowManager: WindowManager
    private lateinit var prefs: SharedPreferences
    private val handler = Handler(Looper.getMainLooper())

    // Views
    private var pointA: View? = null
    private var pointB: View? = null
    private var controlPanel: LinearLayout? = null
    private var greenButton: Button? = null
    private var redButton: Button? = null
    private var stopButton: Button? = null
    private var statusText: TextView? = null

    // Layout params kept around so we can update positions on drag
    private lateinit var paramsA: WindowManager.LayoutParams
    private lateinit var paramsB: WindowManager.LayoutParams
    private lateinit var paramsPanel: WindowManager.LayoutParams

    private var running = false
    private var stopRequested = false

    companion object {
        private const val PREFS_NAME = "auto_clicker_prefs"
        private const val KEY_AX = "point_a_x"
        private const val KEY_AY = "point_a_y"
        private const val KEY_BX = "point_b_x"
        private const val KEY_BY = "point_b_y"
        private const val TAP_DURATION_MS = 60L
        private const val DELAY_BETWEEN_TAPS_MS = 250L
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        addOverlay()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
    }

    // ---------- Overlay construction ----------

    private fun addOverlay() {
        val metrics = resources.displayMetrics
        val screenW = metrics.widthPixels
        val screenH = metrics.heightPixels

        val defaultAx = prefs.getInt(KEY_AX, (screenW * 0.12f).toInt())
        val defaultAy = prefs.getInt(KEY_AY, (screenH * 0.50f).toInt())
        val defaultBx = prefs.getInt(KEY_BX, (screenW * 0.12f).toInt())
        val defaultBy = prefs.getInt(KEY_BY, (screenH * 0.12f).toInt())

        val overlayType = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY

        // Point A dot
        pointA = makeDot("A", Color.parseColor("#2196F3"))
        paramsA = WindowManager.LayoutParams(
            80, 80, overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = defaultAx
            y = defaultAy
        }
        makeDraggable(pointA!!, paramsA) { x, y ->
            prefs.edit().putInt(KEY_AX, x).putInt(KEY_AY, y).apply()
        }
        windowManager.addView(pointA, paramsA)

        // Point B dot
        pointB = makeDot("B", Color.parseColor("#9C27B0"))
        paramsB = WindowManager.LayoutParams(
            80, 80, overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = defaultBx
            y = defaultBy
        }
        makeDraggable(pointB!!, paramsB) { x, y ->
            prefs.edit().putInt(KEY_BX, x).putInt(KEY_BY, y).apply()
        }
        windowManager.addView(pointB, paramsB)

        // Control panel (Green / Red / Stop + status text)
        controlPanel = buildControlPanel()
        paramsPanel = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 60
        }
        // Let the user drag the panel itself by its title bar area too
        makeDraggable(controlPanel!!, paramsPanel, dragHandleOnly = true) { _, _ -> }
        windowManager.addView(controlPanel, paramsPanel)
    }

    private fun removeOverlay() {
        listOf(pointA, pointB, controlPanel).forEach { v ->
            v?.let {
                try { windowManager.removeView(it) } catch (_: Exception) {}
            }
        }
    }

    private fun makeDot(label: String, color: Int): View {
        val tv = TextView(this)
        tv.text = label
        tv.setTextColor(Color.WHITE)
        tv.setBackgroundColor(color)
        tv.gravity = Gravity.CENTER
        tv.alpha = 0.85f
        return tv
    }

    private fun buildControlPanel(): LinearLayout {
        val outer = LinearLayout(this)
        outer.orientation = LinearLayout.VERTICAL
        outer.setBackgroundColor(Color.parseColor("#DD222222"))
        outer.setPadding(24, 16, 24, 24)

        val dragHandle = TextView(this)
        dragHandle.text = "≡ Auto Clicker (drag to move)"
        dragHandle.setTextColor(Color.WHITE)
        dragHandle.gravity = Gravity.CENTER
        dragHandle.tag = "drag_handle"
        dragHandle.setPadding(0, 0, 0, 12)
        outer.addView(dragHandle)

        statusText = TextView(this)
        statusText?.text = "Ready"
        statusText?.setTextColor(Color.WHITE)
        statusText?.gravity = Gravity.CENTER
        statusText?.setPadding(0, 0, 0, 12)
        outer.addView(statusText)

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL

        greenButton = Button(this).apply {
            text = "GREEN (170x)"
            setBackgroundColor(Color.parseColor("#4CAF50"))
            setTextColor(Color.WHITE)
            setOnClickListener { startRun(170, "GREEN") }
        }
        redButton = Button(this).apply {
            text = "RED (230x)"
            setBackgroundColor(Color.parseColor("#F44336"))
            setTextColor(Color.WHITE)
            setOnClickListener { startRun(230, "RED") }
        }
        stopButton = Button(this).apply {
            text = "STOP"
            setBackgroundColor(Color.parseColor("#757575"))
            setTextColor(Color.WHITE)
            visibility = View.GONE
            setOnClickListener {
                stopRequested = true
            }
        }

        row.addView(greenButton)
        row.addView(redButton)
        row.addView(stopButton)
        outer.addView(row)

        return outer
    }

    // ---------- Dragging ----------

    private fun makeDraggable(
        view: View,
        params: WindowManager.LayoutParams,
        dragHandleOnly: Boolean = false,
        onDropped: (x: Int, y: Int) -> Unit
    ) {
        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f

        val target: View = if (dragHandleOnly && view is LinearLayout) {
            view.findViewWithTag("drag_handle") ?: view
        } else view

        target.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - touchX).toInt()
                    params.y = initialY + (event.rawY - touchY).toInt()
                    windowManager.updateViewLayout(view, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    onDropped(params.x, params.y)
                    true
                }
                else -> false
            }
        }
    }

    // ---------- Tap loop ----------

    private fun startRun(times: Int, label: String) {
        if (running) return
        running = true
        stopRequested = false
        greenButton?.isEnabled = false
        redButton?.isEnabled = false
        stopButton?.visibility = View.VISIBLE

        val ax = paramsA.x + 40
        val ay = paramsA.y + 40
        val bx = paramsB.x + 40
        val by = paramsB.y + 40

        runCycle(1, times, label, ax, ay, bx, by)
    }

    private fun runCycle(cycleNum: Int, totalCycles: Int, label: String, ax: Int, ay: Int, bx: Int, by: Int) {
        if (stopRequested || cycleNum > totalCycles) {
            finishRun(if (stopRequested) "Stopped at $cycleNum/$totalCycles" else "Done: $label x$totalCycles")
            return
        }
        statusText?.text = "$label  ${cycleNum}/${totalCycles}"

        tap(ax, ay) {
            handler.postDelayed({
                tap(bx, by) {
                    handler.postDelayed({
                        runCycle(cycleNum + 1, totalCycles, label, ax, ay, bx, by)
                    }, DELAY_BETWEEN_TAPS_MS)
                }
            }, DELAY_BETWEEN_TAPS_MS)
        }
    }

    private fun finishRun(message: String) {
        running = false
        stopRequested = false
        statusText?.text = message
        greenButton?.isEnabled = true
        redButton?.isEnabled = true
        stopButton?.visibility = View.GONE
    }

    private fun tap(x: Int, y: Int, onDone: () -> Unit) {
        val path = Path()
        path.moveTo(x.toFloat(), y.toFloat())
        val stroke = GestureDescription.StrokeDescription(path, 0, TAP_DURATION_MS)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                onDone()
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                onDone()
            }
        }, null)
    }
}
