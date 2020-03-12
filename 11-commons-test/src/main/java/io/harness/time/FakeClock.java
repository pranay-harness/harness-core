package io.harness.time;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.Wither;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.TemporalUnit;

/**
 * To simulate passage of time in tests.
 */
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = false)
public class FakeClock extends Clock {
  @Getter @Setter @Accessors(fluent = true) private Instant instant = Instant.now();
  @Getter @Wither private ZoneId zone = ZoneId.systemDefault();

  public void advanceBy(long amountToAdd, TemporalUnit unit) {
    instant = instant.plus(amountToAdd, unit);
  }

  public Clock toFixed() {
    return Clock.fixed(instant, zone);
  }
}
