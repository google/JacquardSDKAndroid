/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.google.android.jacquard.sample.utilities

import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import com.google.android.jacquard.sample.R
import java.util.*

/**
 * Utility class for datetime.
 */
object DateUtil {

    @JvmStatic
    fun getUserReadableString(timeStamp: Date, context: Context): String {
        var yesterdayLabel = false
        var todayLabel = false
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, -1)
        val yesterDate = getZeroDateTime(cal.time)
        cal.add(Calendar.DAY_OF_MONTH, -1)
        var earlierDate = getZeroDateTime(Calendar.getInstance().time)
        var sectionLabel = ""
        val date = getZeroDateTime(timeStamp)
        if (!todayLabel && isToday(timeStamp.time)) {
            sectionLabel = context.getString(R.string.today)
        } else if (!yesterdayLabel && date.compareTo(yesterDate) == 0) {
            sectionLabel = context.getString(R.string.yesterday)
        } else if (!isToday(timeStamp.time)
            && date.compareTo(yesterDate) != 0
            && date.compareTo(earlierDate) != 0
        ) {
            sectionLabel = (DateFormat.format("MMM", date).toString().uppercase(Locale.getDefault())
                    + " "
                    + DateFormat.format("dd", date)
                    + ", "
                    + DateFormat.format("yyyy", date))
        }
        return sectionLabel
    }

    @JvmStatic
    fun getZeroDateTime(inputDate: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = inputDate
        calendar[Calendar.HOUR_OF_DAY] = 0
        calendar[Calendar.MINUTE] = 0
        calendar[Calendar.SECOND] = 0
        calendar[Calendar.MILLISECOND] = 0
        return calendar.time
    }

    /**
     * Get next date from current selected date
     *
     * @param date date
     */
    @JvmStatic
    fun incrementDateByOne(date: Date): Date {
        val c = Calendar.getInstance()
        c.time = date
        c.add(Calendar.DATE, 1)
        return c.time
    }

    private fun isToday(timeStamp: Long): Boolean = DateUtils.isToday(timeStamp)

}