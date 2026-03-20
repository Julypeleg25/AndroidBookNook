package com.booknook.app.util

import android.content.Context
import android.text.format.DateUtils
import com.booknook.app.R

fun Context.formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    if (now - timestamp < DateUtils.MINUTE_IN_MILLIS) {
        return getString(R.string.time_just_now)
    }

    return DateUtils.getRelativeTimeSpanString(
        timestamp,
        now,
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE
    ).toString()
}
