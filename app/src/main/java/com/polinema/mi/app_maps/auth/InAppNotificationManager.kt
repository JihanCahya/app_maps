package com.polinema.mi.app_maps.auth
import android.animation.Animator
import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.polinema.mi.app_maps.R
import kotlinx.coroutines.*
data class Notification(
    val title: String? = null,
    val message: String? = null,
    val type: String? = null
)
class InAppNotificationManager(private val context: Context) {

    enum class NotificationType {
        SUCCESS, WARNING, ERROR, INFO
    }

    private val notificationQueue = ArrayDeque<NotificationData>()
    private var isShowingNotification = false
    private var notificationContainer: FrameLayout? = null
    private var rootView: ViewGroup? = null

    data class NotificationData(
        val title: String,
        val message: String,
        val type: NotificationType,
        val duration: Long = 3000
    )

    fun initialize(activity: Activity) {
        // Gunakan Activity untuk mendapatkan root view dengan metode yang lebih aman
        rootView = activity.window.decorView.findViewById(android.R.id.content)

        // Inflate notification layout
        val inflater = LayoutInflater.from(context)
        notificationContainer = inflater.inflate(
            R.layout.custom_notification,
            rootView,
            false
        ) as FrameLayout

        rootView?.addView(notificationContainer)
        notificationContainer?.visibility = View.GONE
    }

    fun showInAppNotification(
        title: String,
        message: String,
        type: NotificationType,
        duration: Long = 3000
    ) {
        // Tambahkan pengecekan inisialisasi
        if (notificationContainer == null) {
            Log.e("InAppNotificationManager", "Notification container not initialized")
            return
        }

        val notification = NotificationData(title, message, type, duration)
        notificationQueue.add(notification)
        processNotificationQueue()
    }

    private fun processNotificationQueue() {
        if (isShowingNotification || notificationQueue.isEmpty() || notificationContainer == null) return

        val notification = notificationQueue.removeFirst()
        displayNotification(notification)
    }

    private fun displayNotification(notification: NotificationData) {
        val container = notificationContainer ?: return
        val rootView = rootView ?: return

        isShowingNotification = true

        val card = container.findViewById<MaterialCardView>(R.id.notificationCard)
        val titleView = container.findViewById<TextView>(R.id.notificationTitle)
        val messageView = container.findViewById<TextView>(R.id.notificationMessage)
        val iconView = container.findViewById<ImageView>(R.id.notificationIcon)
        val closeButton = container.findViewById<View>(R.id.closeButton)

        // Set colors and icon based on type
        val (bgColor, textColor, iconRes) = when (notification.type) {
            NotificationType.SUCCESS -> Triple(
                ContextCompat.getColor(context, R.color.notification_success_bg),
                ContextCompat.getColor(context, R.color.notification_success_text),
                R.drawable.baseline_home_24
            )
            NotificationType.WARNING -> Triple(
                ContextCompat.getColor(context, R.color.notification_warning_bg),
                ContextCompat.getColor(context, R.color.notification_warning_text),
                R.drawable.baseline_home_24
            )
            NotificationType.ERROR -> Triple(
                ContextCompat.getColor(context, R.color.notification_error_bg),
                ContextCompat.getColor(context, R.color.notification_error_text),
                R.drawable.baseline_home_24
            )
            NotificationType.INFO -> Triple(
                ContextCompat.getColor(context, R.color.notification_info_bg),
                ContextCompat.getColor(context, R.color.notification_info_text),
                R.drawable.baseline_home_24
            )
        }

        card.setCardBackgroundColor(bgColor)
        titleView.setTextColor(textColor)
        messageView.setTextColor(textColor)
        iconView.setImageResource(iconRes)

        titleView.text = notification.title
        messageView.text = notification.message

        // Slide-in animation
        notificationContainer!!.visibility = View.VISIBLE
        notificationContainer!!.translationY = -notificationContainer!!.height.toFloat()
        notificationContainer!!.animate()
            .translationY(0f)
            .setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Auto dismiss
        GlobalScope.launch(Dispatchers.Main) {
            delay(notification.duration)
            dismissNotification()
        }

        // Manual close
        closeButton.setOnClickListener {
            dismissNotification()
        }
    }

    private fun dismissNotification() {
        val container = notificationContainer ?: return

        container.animate()
            .translationY(-container.height.toFloat())
            .setDuration(300)
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationEnd(animation: Animator) {
                    container.visibility = View.GONE
                    isShowingNotification = false
                    processNotificationQueue()
                }
                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
            .start()
    }
}