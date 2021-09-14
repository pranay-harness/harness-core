/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.time;

import com.google.common.base.Preconditions;
import java.time.Duration;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DurationUtils {
  private static final long DAY_MILLIS = Duration.ofDays(1).toMillis();
  /**
   * Round d towards 0, to a multiple of m (i.e. d%m)
   */
  public static Duration truncate(Duration d, Duration m) {
    Preconditions.checkArgument(m.toMillis() > 0);
    return d.minus(Duration.ofMillis(d.toMillis() % m.toMillis()));
  }

  public static Duration durationTillDayTime(long from, Duration time) {
    Preconditions.checkArgument(time.toMillis() >= 0 && time.toMillis() < DAY_MILLIS);
    return Duration.ofMillis((DAY_MILLIS - from % DAY_MILLIS + time.toMillis()) % DAY_MILLIS);
  }
}
