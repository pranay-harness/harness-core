package software.wings.service.impl.instance.licensing;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.resources.DashboardStatisticsResource;
import software.wings.service.intfc.instance.licensing.InstanceLimitProvider;
import software.wings.service.intfc.instance.licensing.InstanceUsageLimitChecker;
import software.wings.service.intfc.instance.stats.InstanceStatService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class InstanceUsageLimitCheckerImpl implements InstanceUsageLimitChecker {
  private static final Logger log = LoggerFactory.getLogger(InstanceUsageLimitCheckerImpl.class);

  private InstanceLimitProvider instanceLimitProvider;
  private InstanceStatService instanceStatService;

  @Inject
  public InstanceUsageLimitCheckerImpl(
      InstanceLimitProvider instanceLimitProvider, InstanceStatService instanceStatService) {
    this.instanceLimitProvider = instanceLimitProvider;
    this.instanceStatService = instanceStatService;
  }

  @Override
  public boolean isWithinLimit(String accountId, long percentLimit) {
    long allowedUsage = instanceLimitProvider.getAllowedInstances(accountId);
    double actualUsage = actualUsage(accountId);
    boolean withinLimit = isWithinLimit(actualUsage, percentLimit, allowedUsage);

    log.info("[Instance Usage] Allowed: {}, Used: {}, percentLimit: {}, Within Limit: {}, Account ID: {}", allowedUsage,
        actualUsage, percentLimit, withinLimit, accountId);
    return withinLimit;
  }

  static boolean isWithinLimit(double actualUsage, double percentLimit, double allowedUsage) {
    double P = percentLimit / 100.0;
    return actualUsage <= P * allowedUsage;
  }

  // Find 95th percentile of usage in last 30 days.
  // This will serve as 'average' usage in last 30 days (the word 'average' is not used mathematically here)
  private double actualUsage(String accountId) {
    Instant now = Instant.now();
    Instant from = now.minus(30, ChronoUnit.DAYS);
    return instanceStatService.percentile(accountId, from, now, DashboardStatisticsResource.DEFAULT_PERCENTILE);
  }
}