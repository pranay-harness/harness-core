package io.harness.time;

import java.util.concurrent.TimeUnit;

/**
 * Created by rsingh on 6/28/17.
 */
public class Timestamp {
  /**
   * Returns the current minute boundary in milliseconds
   *
   * @return
   */
  public static long currentMinuteBoundary() {
    return minuteBoundary(System.currentTimeMillis());
  }

  /**
   * Returns the minute boundary in milliseconds given a timestamp
   *
   * @param timestampMs
   * @return
   */
  public static long minuteBoundary(long timestampMs) {
    return (timestampMs / TimeUnit.MINUTES.toMillis(1)) * TimeUnit.MINUTES.toMillis(1);
  }

  public static long nextMinuteBoundary(long timestampMs) {
    return minuteBoundary(timestampMs) + TimeUnit.MINUTES.toMillis(1);
  }
}
