/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
