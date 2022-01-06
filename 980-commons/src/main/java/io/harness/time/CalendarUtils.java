/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.time;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CalendarUtils {
  /**
   * Get calendar instance for a timezone
   * @param timeZone
   * @return calendar instance for a timezone
   */
  public static Calendar getCalendarForTimeZone(String timeZone) {
    TimeZone userTimeZone = TimeZone.getTimeZone(timeZone);
    return Calendar.getInstance(userTimeZone);
  }

  /**
   * Get calendar instance for a timezone with time epoch time set
   * @param timeZone
   * @return calendar instance for a timezone
   */
  public static Calendar getCalendar(String timeZone, long epochTime) {
    TimeZone userTimeZone = TimeZone.getTimeZone(timeZone != null ? timeZone : "UTC");
    long timezoneAlteredTime = epochTime - userTimeZone.getRawOffset();
    Calendar calendar = Calendar.getInstance(userTimeZone);
    calendar.setTimeInMillis(timezoneAlteredTime);

    return calendar;
  }

  /**
   * Get Date instance for given time string and calendar instance
   * @param time for instance "7:00 AM"
   * @return hour of the day as a number
   */
  public static Date getDate(String time, TimeZone timeZone) {
    try {
      DateFormat timeFormat = new SimpleDateFormat("hh:mm aa");
      timeFormat.setTimeZone(timeZone);
      return timeFormat.parse(time);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Get calendar instance
   * @param dayofWeek
   * @param time
   * @param timeZone
   * @return Calendar instance with day of week and day of time set in context for given timeZone
   */
  public static Calendar getCalendar(int dayofWeek, String time, String timeZone) {
    Calendar calendar = getCalendarForTimeZone(timeZone);
    calendar.set(Calendar.DAY_OF_WEEK, dayofWeek);
    calendar.setTime(CalendarUtils.getDate(time, TimeZone.getTimeZone(timeZone)));
    calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY));
    return calendar;
  }
}
