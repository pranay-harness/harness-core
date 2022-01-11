/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.features;

import software.wings.features.api.AbstractRestrictedFeature;
import software.wings.features.api.FeatureRestrictions;
import software.wings.features.api.FeatureUsageComplianceReport;
import software.wings.service.intfc.AccountService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;

@Singleton
public class DeploymentHistoryFeature extends AbstractRestrictedFeature {
  public static final String FEATURE_NAME = "DEPLOYMENT_HISTORY";
  private static final String RETENTION_PERIOD_IN_DAYS_KEY = "retentionPeriodInDays";

  @Inject
  public DeploymentHistoryFeature(AccountService accountService, FeatureRestrictions featureRestrictions) {
    super(accountService, featureRestrictions);
  }

  @Override
  public String getFeatureName() {
    return FEATURE_NAME;
  }

  public Optional<Integer> getRetentionPeriodInDays(String accountId) {
    return Optional.ofNullable((Integer) getRestrictions(accountId).get(RETENTION_PERIOD_IN_DAYS_KEY));
  }

  @Override
  public boolean isUsageCompliantWithRestrictions(String accountId, String targetAccountType) {
    return true;
  }

  @Override
  public FeatureUsageComplianceReport getUsageComplianceReport(String accountId, String targetAccountType) {
    return FeatureUsageComplianceReport.builder().featureName(FEATURE_NAME).build();
  }
}
