/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.features;

import software.wings.beans.governance.GovernanceConfig;
import software.wings.features.api.AbstractPremiumFeature;
import software.wings.features.api.ComplianceByRemovingUsage;
import software.wings.features.api.FeatureRestrictions;
import software.wings.features.api.Usage;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.compliance.GovernanceConfigService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collection;
import java.util.Collections;

@Singleton
public class GovernanceFeature extends AbstractPremiumFeature implements ComplianceByRemovingUsage {
  public static final String FEATURE_NAME = "GOVERNANCE";

  private final GovernanceConfigService governanceConfigService;

  @Inject
  public GovernanceFeature(AccountService accountService, FeatureRestrictions featureRestrictions,
      GovernanceConfigService governanceConfigService) {
    super(accountService, featureRestrictions);
    this.governanceConfigService = governanceConfigService;
  }

  @Override
  public boolean removeUsageForCompliance(String accountId, String targetAccountType) {
    if (isUsageCompliantWithRestrictions(accountId, targetAccountType)) {
      return true;
    }

    GovernanceConfig governanceConfig = GovernanceConfig.builder().accountId(accountId).deploymentFreeze(false).build();
    governanceConfigService.upsert(accountId, governanceConfig);

    return isUsageCompliantWithRestrictions(accountId, targetAccountType);
  }

  @Override
  public boolean isBeingUsed(String accountId) {
    GovernanceConfig config = getGovernanceConfig(accountId);
    return config != null && config.isDeploymentFreeze();
  }

  private GovernanceConfig getGovernanceConfig(String accountId) {
    return governanceConfigService.get(accountId);
  }

  @Override
  public Collection<Usage> getDisallowedUsages(String accountId, String targetAccountType) {
    if (isAvailable(targetAccountType)) {
      return Collections.emptyList();
    }
    if (!isBeingUsed(accountId)) {
      return Collections.emptyList();
    }

    return Collections.singletonList(Usage.builder()
                                         .entityId(getGovernanceConfig(accountId).getUuid())
                                         .entityType("GOVERNANCE_CONFIG")
                                         .entityName("GOVERNANCE_CONFIG")
                                         .build());
  }

  @Override
  public String getFeatureName() {
    return FEATURE_NAME;
  }
}
