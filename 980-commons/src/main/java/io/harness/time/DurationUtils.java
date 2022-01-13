/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
