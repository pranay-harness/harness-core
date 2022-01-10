/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

/**
 *
 */

package software.wings.api;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * The Class WingsTimestamp.
 *
 * @author Rishi
 */
public class WingsTimestamp {
  private long timestamp;

  /**
   * Instantiates a new wings timestamp.
   *
   * @param timestamp the timestamp
   */
  public WingsTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  /**
   * Format.
   *
   * @param format the format
   * @return the string
   */
  public String format(String format) {
    try {
      DateFormat df = new SimpleDateFormat(format);
      return df.format(new Date(timestamp));
    } catch (Exception e) {
      return defaultFormat();
    }
  }

  /**
   * Format.
   *
   * @param format   the format
   * @param timezone the timezone
   * @return the string
   */
  public String format(String format, String timezone) {
    try {
      TimeZone timeZone = TimeZone.getTimeZone(timezone);
      Calendar cal = Calendar.getInstance(timeZone);
      cal.setTimeInMillis(timestamp);
      DateFormat df = new SimpleDateFormat(format);
      return df.format(cal.getTime());
    } catch (Exception e) {
      return defaultFormat();
    }
  }

  /**
   * Utc.
   *
   * @return the int
   */
  public int utc() {
    return (int) (timestamp / 1000);
  }

  private String defaultFormat() {
    try {
      return new Date(timestamp).toString();
    } catch (Exception e) {
      return "";
    }
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return String.valueOf(timestamp);
  }
}
