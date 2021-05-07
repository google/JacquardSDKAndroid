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

package com.google.android.jacquard.sample.utilities;

import android.content.Context;
import android.text.format.DateFormat;
import android.text.format.Time;
import com.google.android.jacquard.sample.R;
import java.util.Calendar;
import java.util.Date;

/**
 * Utility class for datetime.
 */
public class DateUtil {

  public static String getUserReadableString(Date timeStamp, Context context) {
    boolean yesterdayLabel = false;
    boolean todayLabel = false;
    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.DAY_OF_MONTH, -1);
    Date yesterDate = getZeroDateTime(cal.getTime());
    cal.add(Calendar.DAY_OF_MONTH, -1);
    Date earlierDate = getZeroDateTime(Calendar.getInstance().getTime());
    String sectionLabel = "";
    Date date = getZeroDateTime(timeStamp);
    if (!todayLabel && isToday(timeStamp.getTime())) {
      todayLabel = true;
      sectionLabel = context.getString(R.string.today);
    } else if (!yesterdayLabel && date.compareTo(yesterDate) == 0) {
      yesterdayLabel = true;
      sectionLabel = context.getString(R.string.yesterday);
    } else if (!isToday(timeStamp.getTime())
        && !(date.compareTo(yesterDate) == 0)
        && !(date.compareTo(earlierDate) == 0)) {
      earlierDate = date;
      sectionLabel =
          DateFormat.format("MMM", date).toString().toUpperCase()
              + " "
              + DateFormat.format("dd", date)
              + ", "
              + DateFormat.format("yyyy", date);
    }
    return sectionLabel;
  }

  public static Date getZeroDateTime(Date inputDate) {
    Date outputDate;
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(inputDate);
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    outputDate = calendar.getTime();
    return outputDate;
  }

  private static boolean isToday(long when) {
    Time time = new Time();
    time.set(when);
    int thenYear = time.year;
    int thenMonth = time.month;
    int thenMonthDay = time.monthDay;
    time.set(System.currentTimeMillis());
    return (thenYear == time.year)
        && (thenMonth == time.month)
        && (thenMonthDay == time.monthDay);
  }

  /**
   * Get next date from current selected date
   *
   * @param date date
   */
  public static Date incrementDateByOne(Date date) {
    Calendar c = Calendar.getInstance();
    c.setTime(date);
    c.add(Calendar.DATE, 1);
    return c.getTime();
  }
}
