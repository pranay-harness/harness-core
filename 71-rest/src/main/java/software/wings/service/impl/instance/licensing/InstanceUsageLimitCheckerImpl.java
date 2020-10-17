package software.wings.service.impl.instance.licensing;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import software.wings.service.intfc.instance.licensing.InstanceLimitProvider;
import software.wings.service.intfc.instance.licensing.InstanceUsageLimitChecker;

@Slf4j
public class InstanceUsageLimitCheckerImpl implements InstanceUsageLimitChecker {
  private InstanceLimitProvider instanceLimitProvider;

  @Inject
  public InstanceUsageLimitCheckerImpl(InstanceLimitProvider instanceLimitProvider) {
    this.instanceLimitProvider = instanceLimitProvider;
  }

  @Override
  public boolean isWithinLimit(String accountId, long percentLimit, double actualUsage) {
    long allowedUsage = instanceLimitProvider.getAllowedInstances(accountId);
    boolean withinLimit = isWithinLimit(actualUsage, percentLimit, allowedUsage);

    logger.info("[Instance Usage] Allowed: {}, Used: {}, percentLimit: {}, Within Limit: {}", allowedUsage, actualUsage,
        percentLimit, withinLimit);
    return withinLimit;
  }

  static boolean isWithinLimit(double actualUsage, double percentLimit, double allowedUsage) {
    double P = percentLimit / 100.0;
    return actualUsage <= P * allowedUsage;
  }
}