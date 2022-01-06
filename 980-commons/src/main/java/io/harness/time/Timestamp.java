/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
