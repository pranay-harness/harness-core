/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.features;

import io.harness.governance.pipeline.service.PipelineGovernanceService;
import io.harness.governance.pipeline.service.model.PipelineGovernanceConfig;

import software.wings.beans.EntityType;
import software.wings.features.api.AbstractPremiumFeature;
import software.wings.features.api.ComplianceByRemovingUsage;
import software.wings.features.api.FeatureRestrictions;
import software.wings.features.api.Usage;
import software.wings.service.intfc.AccountService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class PipelineGovernanceFeature extends AbstractPremiumFeature implements ComplianceByRemovingUsage {
  public static final String FEATURE_NAME = "PIPELINE_GOVERNANCE";

  private PipelineGovernanceService pipelineGovernanceService;

  @Inject
  public PipelineGovernanceFeature(AccountService accountService, FeatureRestrictions featureRestrictions,
      PipelineGovernanceService pipelineGovernanceService) {
    super(accountService, featureRestrictions);
    this.pipelineGovernanceService = pipelineGovernanceService;
  }

  @Override
  public boolean isBeingUsed(String accountId) {
    return !getUsages(accountId).isEmpty();
  }

  @Override
  public Collection<Usage> getDisallowedUsages(String accountId, String targetAccountType) {
    if (isAvailable(targetAccountType)) {
      return Collections.emptyList();
    }

    return getUsages(accountId);
  }

  private Collection<Usage> getUsages(String accountId) {
    List<PipelineGovernanceConfig> pipelineGovernanceStandards = pipelineGovernanceService.list(accountId);
    return pipelineGovernanceStandards.stream().map(PipelineGovernanceFeature::toUsage).collect(Collectors.toList());
  }

  private static Usage toUsage(PipelineGovernanceConfig standard) {
    return Usage.builder()
        .entityId(standard.getUuid())
        .entityName(standard.getName())
        .entityType(EntityType.PIPELINE_GOVERNANCE_STANDARD.name())
        .build();
  }

  @Override
  public String getFeatureName() {
    return FEATURE_NAME;
  }

  @Override
  public boolean removeUsageForCompliance(String accountId, String targetAccountType) {
    if (isUsageCompliantWithRestrictions(accountId, targetAccountType)) {
      return true;
    }
    List<PipelineGovernanceConfig> pipelineGovernanceStandards = pipelineGovernanceService.list(accountId);

    for (PipelineGovernanceConfig governanceConfig : pipelineGovernanceStandards) {
      pipelineGovernanceService.delete(accountId, governanceConfig.getUuid());
    }

    return isUsageCompliantWithRestrictions(accountId, targetAccountType);
  }
}
