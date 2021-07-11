package io.harness.steps.resourcerestraint.service;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.distribution.constraint.Constraint;
import io.harness.distribution.constraint.Consumer;
import io.harness.steps.resourcerestraint.beans.ResourceRestraint;
import io.harness.steps.resourcerestraint.beans.ResourceRestraintInstance;

import java.util.List;
import java.util.Set;

@OwnedBy(PIPELINE)
public interface ResourceRestraintInstanceService {
  ResourceRestraintInstance save(ResourceRestraintInstance resourceRestraintInstance);
  ResourceRestraintInstance activateBlockedInstance(String uuid, String resourceUnit);
  ResourceRestraintInstance finishInstance(String uuid, String resourceUnit);
  boolean updateActiveConstraintsForInstance(ResourceRestraintInstance instance);
  void updateBlockedConstraints(Set<String> constraints);
  List<ResourceRestraintInstance> getAllByRestraintIdAndResourceUnitAndStates(
      String resourceRestraintId, String resourceUnit, List<Consumer.State> states);
  Constraint createAbstraction(ResourceRestraint resourceRestraint);
  int getMaxOrder(String resourceRestraintId);
  int getAllCurrentlyAcquiredPermits(String scope, String releaseEntityId);

  static String getReleaseEntityId(String planExecutionId) {
    return planExecutionId;
  }

  static String getReleaseEntityId(String planExecutionId, String setupNodeId) {
    return planExecutionId + '|' + setupNodeId;
  }

  static String getPlanExecutionIdFromReleaseEntityId(String releaseEntityId) {
    return releaseEntityId.split("[|]")[0];
  }

  static String getSetupNodeIdFromReleaseEntityId(String releaseEntityId) {
    return releaseEntityId.split("[|]")[1];
  }
}
