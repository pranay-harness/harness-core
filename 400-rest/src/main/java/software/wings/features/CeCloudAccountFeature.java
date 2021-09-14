/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.features;

import io.harness.ccm.config.CCMSettingService;

import software.wings.features.api.AbstractUsageLimitedCeFeature;
import software.wings.features.api.ComplianceByLimitingUsage;
import software.wings.features.api.FeatureRestrictions;
import software.wings.service.intfc.AccountService;

import com.google.inject.Inject;
import java.util.Map;

public class CeCloudAccountFeature extends AbstractUsageLimitedCeFeature implements ComplianceByLimitingUsage {
  public static final String FEATURE_NAME = "CE_CLOUD_ACCOUNTS";
  private final CCMSettingService settingsService;

  @Inject
  public CeCloudAccountFeature(
      AccountService accountService, FeatureRestrictions featureRestrictions, CCMSettingService settingsService) {
    super(accountService, featureRestrictions);
    this.settingsService = settingsService;
  }

  @Override
  public boolean limitUsageForCompliance(
      String accountId, String targetAccountType, Map<String, Object> requiredInfoToLimitUsage) {
    return false;
  }

  @Override
  public int getMaxUsageAllowed(String accountType) {
    return (int) getRestrictions(accountType).getOrDefault("maxCloudAccountsAllowed", Integer.MAX_VALUE);
  }

  @Override
  public int getUsage(String accountId) {
    return settingsService.listCeCloudAccounts(accountId).size();
  }

  @Override
  public String getFeatureName() {
    return FEATURE_NAME;
  }
}
