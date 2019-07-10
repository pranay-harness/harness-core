package software.wings.service.impl.marketplace.gcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

public class GCPUsageReportTimeProviderTest extends CategoryTest {
  @Test
  @Category(UnitTests.class)
  public void testProvider() {
    Instant currentTime = Instant.now();
    Instant startTime = currentTime.minus(2, ChronoUnit.DAYS);
    Instant endTime = currentTime.plus(1, ChronoUnit.MINUTES);

    GCPUsageReportTimeProvider gcpUsageReportTimeProvider =
        new GCPUsageReportTimeProvider(startTime, endTime, TimeUnit.DAYS.toDays(1), ChronoUnit.DAYS);

    Instant currentDayEndTime = startTime.plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);

    assertTrue(gcpUsageReportTimeProvider.hasNext());
    assertEquals(gcpUsageReportTimeProvider.next(), currentDayEndTime);

    Instant nextDayEndTime = startTime.plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);

    assertTrue(gcpUsageReportTimeProvider.hasNext());
    assertEquals(gcpUsageReportTimeProvider.next(), nextDayEndTime);

    assertFalse(gcpUsageReportTimeProvider.hasNext());
    assertNull(gcpUsageReportTimeProvider.next());
  }
}
