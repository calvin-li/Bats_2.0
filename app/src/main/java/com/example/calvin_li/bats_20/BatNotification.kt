package com.example.calvin_li.bats_20

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.SystemClock
import android.app.NotificationChannel


class BatNotification : BroadcastReceiver() {
    companion object {
        const val BATS_STOP = "app.calvin.bats.Notification.BATS_STOP" // TODO: maybe remove
        const val notificationID = 23
        private const val channelId = "Battery status"

        internal var manager: AlarmManager? = null
        internal var alarmPendingIntent: PendingIntent? = null
        internal const val updateInterval = 1000 * 60

        private var batteryStatus: Intent? = null
        private var charging: Boolean? = null
        private var lastChange: Double = 0.toDouble()

        fun displayNotification(context: Context) {
            batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

            val level = getBatteryInfo(BatteryManager.EXTRA_LEVEL, context)
            val plugged = plugToMessage(getBatteryInfo(BatteryManager.EXTRA_PLUGGED, context))
            val status = statusToMessage(getBatteryInfo(BatteryManager.EXTRA_STATUS, context))
            val temperature = getBatteryInfo(BatteryManager.EXTRA_TEMPERATURE, context) / 10.0
            val voltage = getBatteryInfo(BatteryManager.EXTRA_VOLTAGE, context)

            var time = "Time formatting error"
            time = formatTime(time)

            val notificationTitle = "$status$plugged for $time"
            val notificationText = temperature.toString() + "\u00b0C / " + voltage + "mV"
            val icon = context.resources.getIdentifier("a$level", "mipmap", context.packageName)
            val resultIntent = Intent(context, MainActivity::class.java)
            resultIntent.putExtra("Action", "Stop")
            val resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, 0)

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(
                NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_LOW))

            val batteryInfoBuilder = Notification.Builder(context, channelId)
                .setOngoing(true)
                .setShowWhen(false)
                .setContentIntent(resultPendingIntent)
                .setSmallIcon(icon)
                .setContentTitle(notificationTitle)
                .setContentText(notificationText)

            notificationManager.notify(notificationID, batteryInfoBuilder.build())
        }

        private fun formatTime(time: String): String { // TODO: unfuck
            var time1 = time
            val timeInMilli = SystemClock.elapsedRealtime() - lastChange.toLong()
            val days = (timeInMilli / (1000 * 60 * 60 * 24)).toInt()
            val hours = (timeInMilli / (1000 * 60 * 60) % 24).toInt()
            val minutes = (timeInMilli / (1000 * 60) % 60).toInt()
            val secs = (timeInMilli / 1000 % 60).toInt()
            if (days > 0) {
                val formatString = "%d:%02d:%02d:%02d"
                time1 = String.format(formatString, days, hours, minutes, secs)
            } else if (days == 0) {
                val formatString = "%d:%02d:%02d"
                time1 = String.format(formatString, hours, minutes, secs)
            }
            return time1
        }

        private fun plugToMessage(state: Int): String {
            //TODO: put state into memory so it persists between alarms
            val ret = " "
            return when (state) {
                BatteryManager.BATTERY_PLUGGED_AC -> "$ret(AC)"
                BatteryManager.BATTERY_PLUGGED_USB -> "$ret(USB)"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "$ret(Wireless)"
                else -> ""
            }
        }

        private fun statusToMessage(state: Int): String {
            if (charging == null) {
                charging = !(state == BatteryManager.BATTERY_STATUS_NOT_CHARGING || state == BatteryManager.BATTERY_STATUS_DISCHARGING)
                lastChange = SystemClock.elapsedRealtime().toDouble()
            }

            when (state) {
                BatteryManager.BATTERY_STATUS_CHARGING -> {
                    if (!charging!!) {
                        changeChargeState()
                    }
                    return "Charging"
                }
                BatteryManager.BATTERY_STATUS_DISCHARGING -> {
                    if (charging!!) {
                        changeChargeState()
                    }
                    return "Discharging"
                }
                BatteryManager.BATTERY_STATUS_FULL -> {
                    if (!charging!!) {
                        changeChargeState()
                    }
                    return "Full"
                }
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> {
                    if (charging!!) {
                        changeChargeState()
                    }
                    return "Not charging"
                }
                else -> return if (state == BatteryManager.BATTERY_STATUS_UNKNOWN) {
                    "Status unknown"
                } else
                    "-1"
            }
        }

        private fun changeChargeState() {
            charging = !charging!!
            lastChange = SystemClock.elapsedRealtime().toDouble()
        }

        private fun getBatteryInfo(extra: String, context: Context): Int {
            val defaultValue = -1
            val ret = batteryStatus!!.getIntExtra(extra, defaultValue)
            val errorMessage = "Attribute $extra could not be fetched."
            if (ret == defaultValue) {
                MainActivity.makeToast(errorMessage, context.applicationContext)
            }
            return ret
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            val serviceIntent = Intent(context, MainActivity::class.java)
            context.startService(serviceIntent)
            return
        }

        displayNotification(context)
    }
}
