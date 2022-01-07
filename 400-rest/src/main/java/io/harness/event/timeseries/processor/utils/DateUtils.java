/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.event.timeseries.processor.utils;

import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class DateUtils {
  private DateUtils() {}

  // If current timestamp is a whole hour, it will return next whole hour
  // if input is 1st Feb, 2020 01:00:00 UTC or input is 1st Feb, 2020 01:27:00 UTC
  // o/p is 1st Feb, 2020 02:00:00 UTC in both cases
  public static Date getNextWholeHourUTC(long timestamp) {
    Calendar c = new GregorianCalendar(getUTCTimezone());
    Date prevHourUTC = getPrevWholeHourUTC(timestamp);
    c.setTime(prevHourUTC);

    c.add(Calendar.HOUR_OF_DAY, 1);

    return c.getTime();
  }

  public static Date getNextNearestWholeHourUTC(long timestamp) {
    Calendar c = new GregorianCalendar(getUTCTimezone());
    Date date = new Date(timestamp);
    c.setTime(date);

    if (isWholeHourUTC(c)) {
      return c.getTime();
    }

    return getNextWholeHourUTC(timestamp);
  }

  // if input is 1st Feb, 2020 01:02:00 UTC
  // o/p is 1st Feb, 2020 01:00:00 UTC
  public static Date getPrevWholeHourUTC(long timestamp) {
    Calendar c = new GregorianCalendar(getUTCTimezone());
    Date date = new Date(timestamp);
    c.setTime(date);

    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);

    return c.getTime();
  }

  // Returns starting midnight timestamp for next day for input timestamp
  // eg: if input is 2nd Feb, 2020 01:10:00 UTC
  // o/p: 3rd Feb, 2020 00:00:00 UTC
  public static Date getNextNearestWholeDayUTC(long timestamp) {
    Calendar c = new GregorianCalendar(getUTCTimezone());
    Date date = new Date(timestamp);
    c.setTime(date);

    // If its already whole day UTC (eg : 1st Feb, 2020 00:00:00)
    // then it is itself the next nearest whole day UTC
    if (isTimestampMidnight(c)) {
      return c.getTime();
    }

    c.add(Calendar.DATE, 1);
    c.set(Calendar.HOUR_OF_DAY, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);

    return c.getTime();
  }

  public static Date getPrevNearestWholeDayUTC(long timestamp) {
    Calendar c = new GregorianCalendar(getUTCTimezone());
    Date date = new Date(timestamp);
    c.setTime(date);

    // If its already whole day UTC (eg : 1st Feb, 2020 00:00:00)
    // then it is itself the prev nearest whole day UTC
    if (isTimestampMidnight(c)) {
      return c.getTime();
    }

    Date nextDayUTC = getNextNearestWholeDayUTC(timestamp);
    c.setTime(nextDayUTC);
    c.add(Calendar.DATE, -1);

    return c.getTime();
  }

  public static Date getBeginningTimestampOfMonth(long timestamp) {
    Calendar c = new GregorianCalendar(getUTCTimezone());
    Date date = new Date(timestamp);
    c.setTime(date);

    c.set(Calendar.DAY_OF_MONTH, 1);
    c.set(Calendar.HOUR_OF_DAY, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);

    return c.getTime();
  }

  public static Date getNextNearestMonthBeginningTimestamp(long timestamp) {
    Calendar c = new GregorianCalendar(getUTCTimezone());
    Date date = new Date(timestamp);
    c.setTime(date);

    if (isMonthStartTimestamp(timestamp)) {
      return c.getTime();
    }

    c.setTime(getBeginningTimestampOfMonth(timestamp));
    c.add(Calendar.MONTH, 1);

    return c.getTime();
  }

  public static Date getPrevNearestMonthBeginningTimestamp(long timestamp) {
    Calendar c = new GregorianCalendar(getUTCTimezone());
    Date date = new Date(timestamp);
    c.setTime(date);

    if (isMonthStartTimestamp(timestamp)) {
      return c.getTime();
    }

    c.add(Calendar.DAY_OF_MONTH, 1);

    return c.getTime();
  }

  public static Integer getDayOfWeek(long timestamp) {
    Calendar c = new GregorianCalendar(getUTCTimezone());
    Date date = new Date(timestamp);
    c.setTime(date);

    return c.get(Calendar.DAY_OF_WEEK);
  }

  public static Integer getDayOfMonth(long timestamp) {
    Calendar c = new GregorianCalendar(getUTCTimezone());
    Date date = new Date(timestamp);
    c.setTime(date);

    return c.get(Calendar.DAY_OF_MONTH);
  }

  public static Integer getCurrentMonth(long timestamp) {
    Calendar c = new GregorianCalendar(getUTCTimezone());
    Date date = new Date(timestamp);
    c.setTime(date);

    return c.get(Calendar.MONTH);
  }

  public static Integer getDaysInCurrentMonth(long timestamp) {
    Calendar c = new GregorianCalendar(getUTCTimezone());
    Date date = new Date(timestamp);
    c.setTime(date);

    return c.getActualMaximum(Calendar.DAY_OF_MONTH);
  }

  // Returns nearest starting timestamp of the week
  // If current timestamp is the starting timestamp of the week, return the same
  public static Date getNextNearestWeekBeginningTimestamp(long timestamp) {
    Calendar c = new GregorianCalendar(getUTCTimezone());
    Date date = new Date(timestamp);
    c.setTime(date);

    // If its already whole day UTC (eg : 1st Feb, 2020 00:00:00) and its Sunday
    // then it is itself the starting timestamp of the week
    Integer dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
    if (dayOfWeek.equals(Calendar.SUNDAY) && isTimestampMidnight(c)) {
      return c.getTime();
    }

    c.set(Calendar.HOUR_OF_DAY, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);

    return addDays(c.getTime().getTime(), Calendar.DAY_OF_WEEK - dayOfWeek + 1);
  }

  public static Date getPrevNearestWeekBeginningTimestamp(long timestamp) {
    Calendar c = new GregorianCalendar(getUTCTimezone());
    Date date = new Date(timestamp);
    c.setTime(date);

    // If its already whole day UTC (eg : 1st Feb, 2020 00:00:00) and its Sunday
    // then it is itself the starting timestamp of the week
    Integer dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
    if (dayOfWeek.equals(Calendar.SUNDAY) && isTimestampMidnight(c)) {
      return c.getTime();
    }

    c.set(Calendar.HOUR_OF_DAY, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);

    return addDays(c.getTime().getTime(), -(dayOfWeek - 1));
  }

  public static long getCurrentTime() {
    return new Date().getTime();
  }

  public static Calendar getDefaultCalendar() {
    return Calendar.getInstance(TimeZone.getTimeZone("UTC"));
  }

  private static Integer getMinutes(long timestamp) {
    Calendar c = new GregorianCalendar(getUTCTimezone());
    Date date = new Date(timestamp);
    c.setTime(date);

    return c.get(Calendar.MINUTE);
  }

  public static Boolean isMonthStartTimestamp(long timestamp) {
    Calendar c = new GregorianCalendar(getUTCTimezone());
    Date date = new Date(timestamp);
    c.setTime(date);

    if (c.get(Calendar.DAY_OF_MONTH) == 1 && isTimestampMidnight(c)) {
      return Boolean.TRUE;
    }

    return Boolean.FALSE;
  }

  public static Date addMinutes(long timestamp, Integer minutes) {
    Calendar c = new GregorianCalendar(getUTCTimezone());
    Date date = new Date(timestamp);
    c.setTime(date);
    c.add(Calendar.MINUTE, minutes);

    return c.getTime();
  }

  public static Date addHours(long timestamp, Integer hours) {
    Calendar c = new GregorianCalendar(getUTCTimezone());
    Date date = new Date(timestamp);
    c.setTime(date);
    c.add(Calendar.HOUR_OF_DAY, hours);

    return c.getTime();
  }

  public static Date addDays(long timestamp, Integer days) {
    Calendar c = new GregorianCalendar(getUTCTimezone());
    Date date = new Date(timestamp);
    c.setTime(date);
    c.add(Calendar.DATE, days);

    return c.getTime();
  }

  public static Date addMonths(long timestamp, Integer months) {
    Calendar c = new GregorianCalendar(getUTCTimezone());
    Date date = new Date(timestamp);
    c.setTime(date);
    c.add(Calendar.MONTH, months);

    return c.getTime();
  }

  public static Instant toInstant(Date date) {
    return Instant.ofEpochMilli(date.getTime());
  }

  private static Boolean isWholeHourUTC(final Calendar c) {
    if (c.get(Calendar.MINUTE) == 0 && c.get(Calendar.SECOND) == 0 && c.get(Calendar.MILLISECOND) == 0) {
      return Boolean.TRUE;
    }

    return Boolean.FALSE;
  }

  private static Boolean isTimestampMidnight(final Calendar c) {
    if (c.get(Calendar.HOUR_OF_DAY) == 0 && isWholeHourUTC(c)) {
      return Boolean.TRUE;
    }

    return Boolean.FALSE;
  }

  private static TimeZone getUTCTimezone() {
    return TimeZone.getTimeZone("UTC");
  }
}
