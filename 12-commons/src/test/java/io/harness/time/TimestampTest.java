package io.harness.time;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimestampTest {
  private static final Logger logger = LoggerFactory.getLogger(TimestampTest.class);

  @Test
  @Category(UnitTests.class)
  public void testCurrentMinuteBoundary() {
    final long currentMinuteBoundary = Timestamp.currentMinuteBoundary();
    assertThat(currentMinuteBoundary).isBetween(currentMinuteBoundary, currentMinuteBoundary + 60 * 1000);
  }

  @Test
  @Category(UnitTests.class)
  public void testMinuteBoundary() {
    assertThat(Timestamp.minuteBoundary(1524335288123L)).isEqualTo(1524335280000L);
  }
}