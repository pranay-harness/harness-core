package io.harness.steps.resourcerestraint.service;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.distribution.constraint.Constraint;
import io.harness.distribution.constraint.Consumer;
import io.harness.steps.resourcerestraint.beans.ResourceRestraint;
import io.harness.steps.resourcerestraint.beans.ResourceRestraintInstance;

import java.util.List;
import java.util.Set;

@OwnedBy(CDC)
public interface ResourceRestraintService {
  ResourceRestraintInstance save(ResourceRestraintInstance resourceRestraintInstance);
  ResourceRestraintInstance activateBlockedInstance(String uuid, String resourceUnit);
  ResourceRestraintInstance finishActiveInstance(String uuid, String resourceUnit);
  Set<String> updateRunningConstraints(String releaseEntityType, String releaseEntityId);
  boolean updateActiveConstraintsForInstance(ResourceRestraintInstance instance);
  void updateBlockedConstraints(Set<String> constraints);
  List<ResourceRestraintInstance> getAllByRestraintIdAndResourceUnitAndStates(
      String resourceRestraintId, String resourceUnit, List<Consumer.State> states);
  Constraint createAbstraction(ResourceRestraint resourceRestraint);
  int getMaxOrder(String resourceRestraintId);
  int getAllCurrentlyAcquiredPermits(String scope, String releaseEntityId);

  static String getReleaseEntityId(String releaseTypeId) {
    return releaseTypeId;
  }

  static String getReleaseEntityId(String planId, String releaseTypeId) {
    return planId + '|' + releaseTypeId;
  }

  static String planExecutionIdFromReleaseEntityId(String releaseEntityId) {
    return releaseEntityId.split("[|]")[0];
  }

  static String planNodeIdFromReleaseEntityId(String releaseEntityId) {
    return releaseEntityId.split("[|]")[1];
  }
}
