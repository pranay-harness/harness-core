package software.wings.service.impl.marketplace.gcp;

import static io.harness.rule.OwnerRule.HITESH;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

public class GCPUsageReportTimeProviderTest extends CategoryTest {
  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testProvider() {
    Instant currentTime = Instant.now().truncatedTo(ChronoUnit.DAYS);
    Instant startTime = currentTime.minus(3, ChronoUnit.DAYS);
    Instant endTime = currentTime;

    GCPUsageReportTimeProvider gcpUsageReportTimeProvider =
        new GCPUsageReportTimeProvider(startTime, endTime, TimeUnit.DAYS.toDays(1), ChronoUnit.DAYS);

    Instant currentDayEndTime = startTime.plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);

    assertThat(gcpUsageReportTimeProvider.hasNext()).isTrue();
    assertThat(currentDayEndTime).isEqualTo(gcpUsageReportTimeProvider.next());

    Instant nextDayEndTime = startTime.plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);

    assertThat(gcpUsageReportTimeProvider.hasNext()).isTrue();
    assertThat(nextDayEndTime).isEqualTo(gcpUsageReportTimeProvider.next());

    assertThat(gcpUsageReportTimeProvider.hasNext()).isFalse();
    assertThat(gcpUsageReportTimeProvider.next()).isNull();
  }
}
