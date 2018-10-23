package software.wings.resources.stats.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import software.wings.resources.stats.model.TimeRange;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

public class TimeRangeProviderTest {
  @Test
  public void monthlyRanges() {
    LocalDateTime from = LocalDateTime.parse("2018-10-03T10:15:30");
    LocalDateTime to = LocalDateTime.parse("2019-01-03T10:15:30");

    TimeRangeProvider provider = new TimeRangeProvider(ZoneOffset.UTC);
    List<TimeRange> timeRanges = provider.monthlyRanges(from.toInstant(ZoneOffset.UTC), to.toInstant(ZoneOffset.UTC));
    assertEquals("Size should be 4: Oct, Nov, Dec, Jan", 4, timeRanges.size());
    assertEquals("October 2018", timeRanges.get(0).getLabel());
    assertEquals("January 2019", timeRanges.get(3).getLabel());
  }
}
