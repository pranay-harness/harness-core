package software.wings.beans.instance.dashboard;

import software.wings.resources.DashboardStatisticsResource;
import software.wings.service.intfc.instance.stats.InstanceStatService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class InstanceStatsUtil {
  // Find 95th percentile of usage in last 30 days.
  // This will serve as 'average' usage in last 30 days (the word 'average' is not used mathematically here)
  public static double actualUsage(String accountId, InstanceStatService instanceStatService) {
    Instant now = Instant.now();
    Instant from = now.minus(30, ChronoUnit.DAYS);
    return instanceStatService.percentile(accountId, from, now, DashboardStatisticsResource.DEFAULT_PERCENTILE);
  }
}
