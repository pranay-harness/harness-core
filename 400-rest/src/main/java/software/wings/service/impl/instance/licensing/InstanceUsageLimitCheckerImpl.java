/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.instance.licensing;

import software.wings.service.intfc.instance.licensing.InstanceLimitProvider;
import software.wings.service.intfc.instance.licensing.InstanceUsageLimitChecker;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

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

    log.info("[Instance Usage] Allowed: {}, Used: {}, percentLimit: {}, Within Limit: {}", allowedUsage, actualUsage,
        percentLimit, withinLimit);
    return withinLimit;
  }

  static boolean isWithinLimit(double actualUsage, double percentLimit, double allowedUsage) {
    double P = percentLimit / 100.0;
    return actualUsage <= P * allowedUsage;
  }
}
