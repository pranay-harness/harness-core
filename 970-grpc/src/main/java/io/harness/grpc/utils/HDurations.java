/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.grpc.utils;

import com.google.protobuf.Duration;
import com.google.protobuf.util.Durations;
import lombok.experimental.UtilityClass;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

@UtilityClass
public class HDurations {
  private static final PeriodFormatter formatter =
      new PeriodFormatterBuilder().appendMinutes().appendSuffix("m").appendSeconds().appendSuffix("s").toFormatter();

  public Duration parse(String duration) {
    return Durations.fromSeconds(formatter.parsePeriod(duration).toStandardDuration().getStandardSeconds());
  }
}
