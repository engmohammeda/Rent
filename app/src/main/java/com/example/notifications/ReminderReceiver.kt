package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.SakanDatabase
import com.example.utils.Formatters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = SakanDatabase.getInstance(context)
                val dao = db.sakanDao()
                val config = dao.getConfigOnce() ?: return@launch
                if (!config.notificationsEnabled) return@launch

                val openPeriod = dao.getLatestOpenPeriod() ?: return@launch
                val ledgers = dao.getLedgerForPeriodOnce(openPeriod.id)
                val unpaidCount = ledgers.count { it.totalRemaining > 0.05 }
                val totalRemaining = ledgers.sumOf { it.totalRemaining }

                val isElectricityPending = openPeriod.electricityTotalAmount <= 0.01
                if (unpaidCount == 0 && !isElectricityPending) {
                    return@launch
                }

                val dayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                val isElecDueSoon = isDayApproachingOrPassed(dayOfMonth, config.electricityDueDayOfMonth)
                val isRentDueSoon = isDayApproachingOrPassed(dayOfMonth, config.rentDueDayOfMonth)

                val (title, body) = when {
                    isElectricityPending && isElecDueSoon -> {
                        "تنبيه فاتورة الكهرباء • ${config.apartmentName}" to
                            "اقترب موعد فاتورة شهر ${openPeriod.label}. سجّل القراءة والمبلغ لتقسيمه على السكان."
                    }
                    unpaidCount > 0 -> {
                        val remainingStr = Formatters.formatMoney(totalRemaining, config.currencySymbol)
                        val prefix = if (isRentDueSoon || isElecDueSoon) "موعد التحصيل • " else ""
                        "${prefix}${config.apartmentName} (${openPeriod.label})" to
                            "متبقي $remainingStr لدى $unpaidCount ساكنين. اضغط للمتابعة وإرسال تذكير واتساب."
                    }
                    else -> return@launch
                }

                ReminderScheduler.postNotification(context, title, body)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun isDayApproachingOrPassed(today: Int, dueDay: Int): Boolean {
        val diff = dueDay - today
        return diff in -10..4 || (dueDay <= 4 && today >= 26)
    }
}

object ReminderScheduler {
    const val CHANNEL_ID = "sakan_billing_reminders"
    private const val NOTIFICATION_ID = 1001
    private const val REQUEST_CODE = 2002

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تذكيرات العزبة والفواتير",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات دورية للمالك لمتابعة الإيجار وفواتير الكهرباء والماء حتى اكتمال السداد"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun scheduleRecurringReminders(context: Context, intervalHours: Int, enabled: Boolean) {
        ensureChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        if (!enabled) return

        val safeHours = intervalHours.coerceIn(6, 72)
        val intervalMs = safeHours * 60L * 60L * 1000L
        val firstTrigger = System.currentTimeMillis() + intervalMs

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            firstTrigger,
            intervalMs,
            pendingIntent
        )
    }

    fun postNotification(context: Context, title: String, message: String) {
        ensureChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
