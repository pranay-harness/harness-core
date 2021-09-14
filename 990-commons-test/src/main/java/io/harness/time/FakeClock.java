/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.time;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.TemporalUnit;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.Wither;

/**
 * To simulate passage of time in tests.
 */
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = false)
public class FakeClock extends Clock {
  @Getter @Setter @Accessors(fluent = true) private Instant instant = Instant.now();
  @Getter @Wither private ZoneId zone = ZoneId.of("UTC");

  public void advanceBy(long amountToAdd, TemporalUnit unit) {
    instant = instant.plus(amountToAdd, unit);
  }

  public Clock toFixed() {
    return Clock.fixed(instant, zone);
  }
}
