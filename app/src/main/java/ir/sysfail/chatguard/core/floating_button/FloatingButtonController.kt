package ir.sysfail.chatguard.core.floating_button

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.*
import android.view.animation.DecelerateInterpolator
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

class FloatingButtonController(
    private val context: Context
) {

    private var clickListener: (() -> Unit)? = null

    private val windowManager by lazy {
        context.getSystemService(WindowManager::class.java)
    }

    private val handler = Handler(Looper.getMainLooper())
    private var idleRunnable: Runnable? = null

    private var buttonView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var isShowAnimationInProgress = false
    private var isCloseAnimationInProgress = false

    private var isIdle = false
    private var buttonColor = IDLE_COLOR

    fun setOnClickListener(listener: () -> Unit) {
        clickListener = listener
    }

    fun show() {
        if (buttonView != null) return

        buttonView = createButtonView()
        layoutParams = createLayoutParams()

        windowManager.addView(buttonView, layoutParams)
        animateShow(buttonView!!)
        scheduleIdle()
    }

    fun hide() {
        val view = buttonView ?: return
        cancelIdle()
        animateHide(view) {
            windowManager.removeView(view)
            buttonView = null
            layoutParams = null
        }
    }

    fun setButtonColor(color: Int) {
        buttonColor = color
        buttonView?.background = createBackgroundDrawable(color)
    }

    private fun createButtonView(): View {
        return View(context).apply {
            val size = dp(BUTTON_SIZE)
            layoutParams = ViewGroup.LayoutParams(size, size)
            background = createBackgroundDrawable(buttonColor)

            scaleX = 0.7f
            scaleY = 0.7f
            alpha = 0f

//            setOnClickListener {
//                exitIdle()
//                scheduleIdle()
//            }

            setOnTouchListener(createDragTouchListener())
        }
    }

    private fun createLayoutParams(): WindowManager.LayoutParams {
        val size = dp(BUTTON_SIZE)

        return WindowManager.LayoutParams(
            size,
            size,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_TOAST,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = getScreenWidth() - size
            y = dp(200)
        }
    }

    private fun createDragTouchListener(): View.OnTouchListener {
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f

        return View.OnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    exitIdle()
                    animatePress(true)
                    startX = layoutParams!!.x
                    startY = layoutParams!!.y
                    touchX = event.rawX
                    touchY = event.rawY
                    clickListener?.invoke()
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    layoutParams!!.x = startX + (event.rawX - touchX).toInt()
                    layoutParams!!.y = startY + (event.rawY - touchY).toInt()
                    windowManager.updateViewLayout(buttonView, layoutParams)
                    true
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    animatePress(false)
                    snapToEdge()
                    scheduleIdle()
                    true
                }

                else -> false
            }
        }
    }

    private fun snapToEdge() {
        val params = layoutParams ?: return
        val screenWidth = getScreenWidth()
        val size = dp(BUTTON_SIZE)

        val centerX = params.x + size / 2
        val targetX = if (centerX < screenWidth / 2) {
            -size / 2
        } else {
            screenWidth - size / 2
        }

        animateMoveX(params.x, targetX)
    }

    private fun animateMoveX(from: Int, to: Int) {
        val params = layoutParams ?: return
        val view = buttonView ?: return

        ObjectAnimator.ofInt(from, to).apply {
            duration = 420
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener {
                params.x = it.animatedValue as Int
                windowManager.updateViewLayout(view, params)
            }
            start()
        }
    }

    private fun animateShow(view: View) {
        if (isShowAnimationInProgress) return
        isShowAnimationInProgress = true

        view.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(320)
            .setInterpolator(FastOutSlowInInterpolator())
            .withEndAction {
                isShowAnimationInProgress = false
            }
            .start()

    }

    private fun animateHide(view: View, end: () -> Unit) {
        if (isCloseAnimationInProgress) return
        isCloseAnimationInProgress = true

        view.animate()
            .alpha(0f)
            .scaleX(0.6f)
            .scaleY(0.6f)
            .setDuration(220)
            .withEndAction {
                end.invoke()
                isCloseAnimationInProgress = false
            }
            .start()
    }

    private fun animatePress(pressed: Boolean) {
        val view = buttonView ?: return

        view.animate()
            .scaleX(if (pressed) 0.9f else 1f)
            .scaleY(if (pressed) 0.9f else 1f)
            .setDuration(120)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun scheduleIdle() {
        cancelIdle()
        idleRunnable = Runnable {
            enterIdle()
        }.also {
            handler.postDelayed(it, 2500)
        }
    }

    private fun cancelIdle() {
        idleRunnable?.let { handler.removeCallbacks(it) }
        idleRunnable = null
    }

    private fun enterIdle() {
        val view = buttonView ?: return
        if (isIdle) return
        isIdle = true

        val size = dp(BUTTON_SIZE)
        val params = layoutParams ?: return

        val isLeft = params.x + size / 2 < getScreenWidth() / 2
        val offset = if (isLeft) -size * 0.4f else size * 0.4f

        view.animate()
            .alpha(0.45f)
            .translationX(offset)
            .setDuration(320)
            .setInterpolator(FastOutSlowInInterpolator())
            .start()
    }

    private fun exitIdle() {
        val view = buttonView ?: return
        if (!isIdle) return
        isIdle = false

        view.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(160)
            .setInterpolator(FastOutSlowInInterpolator())
            .start()
    }

    private fun createBackgroundDrawable(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(android.graphics.Color.TRANSPARENT)
            setStroke(dp(4), color)
        }
    }

    private fun getScreenWidth(): Int {
        return context.resources.displayMetrics.widthPixels
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    companion object {
        val IDLE_COLOR = android.graphics.Color.parseColor("#969696")
        val GREEN_SUCCESS_COLOR = android.graphics.Color.parseColor("#32a852")
        val RED_FAIL_COLOR = android.graphics.Color.parseColor("#a83232")

        private const val BUTTON_SIZE = 36
    }
}
