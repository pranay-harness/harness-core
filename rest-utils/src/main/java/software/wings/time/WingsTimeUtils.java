package software.wings.time;

import java.util.concurrent.TimeUnit;

/**
 * Created by rsingh on 6/28/17.
 */
public class WingsTimeUtils {
  /**
   * Returns the minute boundary in milliseconds given a time stamp
   *
   * @param timeStampMs
   * @return
   */
  public static long getMinuteBoundary(long timeStampMs) {
    return (timeStampMs / TimeUnit.MINUTES.toMillis(1)) * TimeUnit.MINUTES.toMillis(1);
  }
}
