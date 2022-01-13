/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.features;

import static io.harness.beans.SearchFilter.Operator.EQ;

import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.delegate.beans.Delegate.DelegateKeys;

import software.wings.features.api.AbstractUsageLimitedFeature;
import software.wings.features.api.ComplianceByLimitingUsage;
import software.wings.features.api.FeatureRestrictions;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.DelegateService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Singleton
public class DelegatesFeature extends AbstractUsageLimitedFeature implements ComplianceByLimitingUsage {
  public static final String FEATURE_NAME = "DELEGATES";

  private final DelegateService delegateService;

  @Inject
  public DelegatesFeature(
      AccountService accountService, FeatureRestrictions featureRestrictions, DelegateService delegateService) {
    super(accountService, featureRestrictions);
    this.delegateService = delegateService;
  }

  @Override
  public int getMaxUsageAllowed(String accountType) {
    return (int) getRestrictions(accountType).getOrDefault("maxDelegatesAllowed", Integer.MAX_VALUE);
  }

  @Override
  public int getUsage(String accountId) {
    return getCurrentDelegateCount(accountId);
  }

  @Override
  public String getFeatureName() {
    return FEATURE_NAME;
  }

  @Override
  public boolean limitUsageForCompliance(
      String accountId, String targetAccountType, Map<String, Object> requiredInfoToLimitUsage) {
    if (isUsageCompliantWithRestrictions(accountId, targetAccountType)) {
      return true;
    }

    if (!getAccountType(accountId).equals(targetAccountType)) {
      return true;
    }

    delegateService.deleteAllDelegatesExceptOne(accountId, TimeUnit.MINUTES.toMillis(2));

    return true;
  }

  private int getCurrentDelegateCount(String accountId) {
    return delegateService
        .list(PageRequestBuilder.aPageRequest().addFilter(DelegateKeys.accountId, EQ, accountId).build())
        .size();
  }
}
