package io.harness.time;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static io.harness.time.DurationUtils.truncate;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.time.Duration;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class DurationUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testTruncate() throws Exception {
    Duration duration = Duration.ofDays(7).plusHours(3).plusSeconds(33).plusMillis(23);
    assertThat(truncate(duration, Duration.ofDays(1))).isEqualTo(Duration.ofDays(7));
    assertThat(truncate(duration, Duration.ofDays(2))).isEqualTo(Duration.ofDays(6));
    assertThat(truncate(duration, Duration.ofHours(2))).isEqualTo(Duration.ofDays(7).plusHours(2));
    assertThat(truncate(duration, Duration.ofHours(1))).isEqualTo(Duration.ofDays(7).plusHours(3));
    assertThat(truncate(duration, Duration.ofSeconds(10))).isEqualTo(Duration.ofDays(7).plusHours(3).plusSeconds(30));
  }
}
