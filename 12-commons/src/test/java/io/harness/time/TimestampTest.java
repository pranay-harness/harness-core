package io.harness.time;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimestampTest {
  private static final Logger logger = LoggerFactory.getLogger(TimestampTest.class);

  @Test
  public void testCurrentMinuteBoundary() {
    final long currentMinuteBoundary = Timestamp.currentMinuteBoundary();
    assertThat(currentMinuteBoundary).isBetween(currentMinuteBoundary, currentMinuteBoundary + 60 * 1000);
  }

  @Test
  public void testMinuteBoundary() {
    assertThat(Timestamp.minuteBoundary(1524335288123L)).isEqualTo(1524335280000L);
  }
}