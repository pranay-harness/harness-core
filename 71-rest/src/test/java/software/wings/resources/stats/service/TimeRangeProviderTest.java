package software.wings.resources.stats.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.resources.stats.model.TimeRange;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

public class TimeRangeProviderTest extends CategoryTest {
  @Test
  @Category(UnitTests.class)
  public void monthlyRanges() {
    LocalDateTime from = LocalDateTime.parse("2018-10-03T10:15:30");
    LocalDateTime to = LocalDateTime.parse("2019-01-03T10:15:30");

    TimeRangeProvider provider = new TimeRangeProvider(ZoneOffset.UTC);
    List<TimeRange> timeRanges = provider.monthlyRanges(from.toInstant(ZoneOffset.UTC), to.toInstant(ZoneOffset.UTC));
    assertThat(timeRanges).hasSize(4);
    assertEquals("October 2018", timeRanges.get(0).getLabel());
    assertEquals("January 2019", timeRanges.get(3).getLabel());
  }
}
