package software.wings.service.impl.instance.licensing;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.service.intfc.instance.licensing.InstanceLimitProvider;
import software.wings.service.intfc.instance.licensing.InstanceUsageLimitChecker;

public class InstanceUsageLimitCheckerImpl implements InstanceUsageLimitChecker {
  private static final Logger log = LoggerFactory.getLogger(InstanceUsageLimitCheckerImpl.class);

  private InstanceLimitProvider instanceLimitProvider;

  @Inject
  public InstanceUsageLimitCheckerImpl(InstanceLimitProvider instanceLimitProvider) {
    this.instanceLimitProvider = instanceLimitProvider;
  }

  @Override
  public boolean isWithinLimit(String accountId, long percentLimit, double actualUsage) {
    long allowedUsage = instanceLimitProvider.getAllowedInstances(accountId);
    boolean withinLimit = isWithinLimit(actualUsage, percentLimit, allowedUsage);

    log.info("[Instance Usage] Allowed: {}, Used: {}, percentLimit: {}, Within Limit: {}, Account ID: {}", allowedUsage,
        actualUsage, percentLimit, withinLimit, accountId);
    return withinLimit;
  }

  static boolean isWithinLimit(double actualUsage, double percentLimit, double allowedUsage) {
    double P = percentLimit / 100.0;
    return actualUsage <= P * allowedUsage;
  }
}