package io.harness.pms.pipeline.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Singleton;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class PipelineServiceEnforcementServiceImpl implements PipelineServiceEnforcementService {
  @Override
  public boolean isFeatureRestricted(String accountId, String featureRestrictionName) {
    // Todo: Call EnforcementSdk here
    return false;
  }
}
