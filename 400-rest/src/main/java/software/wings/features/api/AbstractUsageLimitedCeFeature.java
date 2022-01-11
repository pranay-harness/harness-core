/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
