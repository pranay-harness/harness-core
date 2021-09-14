/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.features;

import io.harness.ccm.cluster.ClusterRecordService;

import software.wings.features.api.AbstractUsageLimitedCeFeature;
import software.wings.features.api.ComplianceByLimitingUsage;
import software.wings.features.api.FeatureRestrictions;
import software.wings.service.intfc.AccountService;

import com.google.inject.Inject;
import java.util.Map;

public class CeClusterFeature extends AbstractUsageLimitedCeFeature implements ComplianceByLimitingUsage {
  public static final String FEATURE_NAME = "CE_CLUSTERS";
  private final ClusterRecordService clusterRecordService;

  @Inject
  public CeClusterFeature(AccountService accountService, FeatureRestrictions featureRestrictions,
      ClusterRecordService clusterRecordService) {
    super(accountService, featureRestrictions);
    this.clusterRecordService = clusterRecordService;
  }

  @Override
  public boolean limitUsageForCompliance(
      String accountId, String targetAccountType, Map<String, Object> requiredInfoToLimitUsage) {
    return false;
  }

  @Override
  public int getMaxUsageAllowed(String accountType) {
    return (int) getRestrictions(accountType).getOrDefault("maxClustersAllowed", Integer.MAX_VALUE);
  }

  @Override
  public int getUsage(String accountId) {
    return clusterRecordService.listCeEnabledClusters(accountId).size();
  }

  @Override
  public String getFeatureName() {
    return FEATURE_NAME;
  }
}
