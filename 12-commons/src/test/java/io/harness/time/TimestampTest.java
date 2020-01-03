package io.harness.time;

import static io.harness.rule.OwnerRule.GEORGE;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class TimestampTest extends CategoryTest {
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testCurrentMinuteBoundary() {
    final long currentMinuteBoundary = Timestamp.currentMinuteBoundary();
    assertThat(currentMinuteBoundary).isBetween(currentMinuteBoundary, currentMinuteBoundary + 60 * 1000);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testMinuteBoundary() {
    assertThat(Timestamp.minuteBoundary(1524335288123L)).isEqualTo(1524335280000L);
  }
}