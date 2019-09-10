package software.wings.service.impl.instance.stats.collector;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.Instant;

public class StatsCollectorImplTest extends CategoryTest {
  @Test
  @Category(UnitTests.class)
  public void alignedWith10thMinute() {
    Instant instant = Instant.parse("2018-12-03T10:10:30.00Z");
    Instant aligned = StatsCollectorImpl.alignedWithMinute(instant, 10);
    assertThat(Instant.parse("2018-12-03T10:10:00.00Z")).isEqualTo(aligned);

    Instant instant2 = Instant.parse("2018-12-03T10:12:30.00Z");
    aligned = StatsCollectorImpl.alignedWithMinute(instant2, 10);
    assertThat(Instant.parse("2018-12-03T10:10:00.00Z")).isEqualTo(aligned);
  }
}
