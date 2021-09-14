/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
