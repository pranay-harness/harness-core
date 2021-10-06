package io.harness.pms.pipeline.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PIPELINE)
public interface PipelineServiceEnforcementService {
  boolean isFeatureRestricted(String accountId, String featureRestrictionName);
}
