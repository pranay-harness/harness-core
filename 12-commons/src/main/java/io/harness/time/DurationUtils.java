package io.harness.time;

import com.google.common.base.Preconditions;

import lombok.experimental.UtilityClass;

import java.time.Duration;

@UtilityClass
public class DurationUtils {
  /**
   * Round d towards 0, to a multiple of m (i.e. d%m)
   */
  public static Duration truncate(Duration d, Duration m) {
    Preconditions.checkArgument(m.toMillis() > 0);
    return d.minus(Duration.ofMillis(d.toMillis() % m.toMillis()));
  }
}
