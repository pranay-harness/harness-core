package software.wings.service.impl.instance.stats.collector;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.Instant;

public class StatsCollectorImplTest {
  @Test
  public void alignedWith10thMinute() {
    Instant instant = Instant.parse("2018-12-03T10:10:30.00Z");
    Instant aligned = StatsCollectorImpl.alignedWithMinute(instant, 10);
    assertEquals("the value should be truncated to minute", aligned, Instant.parse("2018-12-03T10:10:00.00Z"));

    Instant instant2 = Instant.parse("2018-12-03T10:12:30.00Z");
    aligned = StatsCollectorImpl.alignedWithMinute(instant2, 10);
    assertEquals(
        "the value should be truncated to previous 10th minute", aligned, Instant.parse("2018-12-03T10:10:00.00Z"));
  }
}
