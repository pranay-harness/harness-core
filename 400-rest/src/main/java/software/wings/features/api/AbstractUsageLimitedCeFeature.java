/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.features.api;

import software.wings.service.intfc.AccountService;

import com.google.inject.Inject;

public abstract class AbstractUsageLimitedCeFeature extends AbstractRestrictedCeFeature implements UsageLimitedFeature {
  @Inject
  public AbstractUsageLimitedCeFeature(AccountService accountService, FeatureRestrictions featureRestrictions) {
    super(accountService, featureRestrictions);
  }

  @Override
  public boolean isUsageCompliantWithRestrictions(String accountId, String targetAccountType) {
    return getUsage(accountId) <= getMaxUsageAllowed(targetAccountType);
  }

  @Override
  public FeatureUsageComplianceReport getUsageComplianceReport(String accountId, String targetAccountType) {
    return FeatureUsageComplianceReport.builder()
        .featureName(getFeatureName())
        .property("isUsageCompliantWithRestrictions", isUsageCompliantWithRestrictions(accountId, targetAccountType))
        .property("isUsageLimited", true)
        .property("maxUsageAllowed", getMaxUsageAllowed(targetAccountType))
        .property("currentUsage", getUsage(accountId))
        .build();
  }

  @Override
  public int getMaxUsageAllowedForAccount(String accountId) {
    return getMaxUsageAllowed(getAccountType(accountId));
  }
}
