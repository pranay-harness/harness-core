/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
