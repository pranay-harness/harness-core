package io.harness.time;

import java.util.concurrent.TimeUnit;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Timestamp {
  public static long currentMinuteBoundary() {
    return minuteBoundary(System.currentTimeMillis());
  }

  public static long minuteBoundary(long timestampMs) {
    return (timestampMs / TimeUnit.MINUTES.toMillis(1)) * TimeUnit.MINUTES.toMillis(1);
  }

  public static long nextMinuteBoundary(long timestampMs) {
    return minuteBoundary(timestampMs) + TimeUnit.MINUTES.toMillis(1);
  }
}
