package org.schabi.newpipe.local.feed.notifications

import android.content.Context
import androidx.preference.PreferenceManager
import org.schabi.newpipe.R
import java.util.concurrent.TimeUnit

/**
 * Information for the Scheduler which checks for new streams.
 * See [NotificationWorker]
 */
data class ScheduleOptions(
    val interval: Long,
    val isRequireNonMeteredNetwork: Boolean
) {

    companion object {

        fun from(context: Context): ScheduleOptions {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            return ScheduleOptions(
                interval = TimeUnit.SECONDS.toMillis(
                    preferences.getString(
                        context.getString(R.string.streams_notifications_interval_key),
                        null
                    )?.toLongOrNull() ?: context.getString(
                        R.string.streams_notifications_interval_default
                    ).toLong()
                ),
                isRequireNonMeteredNetwork = preferences.getString(
                    context.getString(R.string.streams_notifications_network_key),
                    context.getString(R.string.streams_notifications_network_default)
                ) == context.getString(R.string.streams_notifications_network_wifi)
            )
        }
    }
}
