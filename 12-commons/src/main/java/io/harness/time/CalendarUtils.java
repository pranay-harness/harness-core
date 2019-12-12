package io.harness.time;

import lombok.extern.slf4j.Slf4j;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

@Slf4j
public class CalendarUtils {
  /**
   * Get calendar instance for a timezone
   * @param timeZone
   * @return calendar instance for a timezone
   */
  public static Calendar getCalendarForTimeZone(String timeZone) {
    SimpleDateFormat dateFormatTimeZone = new SimpleDateFormat("EEEE");
    TimeZone userTimeZone = TimeZone.getTimeZone(timeZone);
    dateFormatTimeZone.setTimeZone(userTimeZone);
    return Calendar.getInstance(userTimeZone);
  }

  /**
   * Get calendar instance for a timezone with time epoch time set
   * @param timeZone
   * @return calendar instance for a timezone
   */
  public static Calendar getCalendar(String timeZone, long epochTime) {
    Calendar calendar = getCalendarForTimeZone(timeZone);
    calendar.setTimeInMillis(epochTime);
    return calendar;
  }

  /**
   * Get Date instance for given time string and calendar instance
   * @param time for instance "7:00 AM"
   * @return hour of the day as a number
   */
  public static Date getDate(String time) {
    try {
      DateFormat timeFormat = new SimpleDateFormat("hh:mm aa");
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
    calendar.setTime(CalendarUtils.getDate(time));
    calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY));
    return calendar;
  }
}
