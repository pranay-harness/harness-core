package io.harness.pms.pipeline.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.enforcement.constants.FeatureRestrictionName;
import io.harness.pms.contracts.steps.StepType;

import java.util.List;
import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
public interface PipelineEnforcementService {
  boolean isFeatureRestricted(String accountId, String featureRestrictionName);

  Map<FeatureRestrictionName, Boolean> getFeatureRestrictionMap(
      String accountId, List<String> featureRestrictionNameList);

  void checkFeatureRestrictionOrThrow(String accountId, List<String> featureRestrictionNameList);

  /**
   * Check and validate the feature restriction on the given stepTypes
   * Also, validates the deployments and builds feature restriction check if stepType contains module specific stage
   */
  void checkStepRestrictionAndThrow(String accountId, List<StepType> stepTypes);
}
