package io.harness.steps.resourcerestraint.service;

import io.harness.steps.resourcerestraint.beans.ResourceConstraint;

import java.util.List;
import java.util.Set;

/**
 * The type Restraint Service.
 * This is only to provide a Restraint Service Binding to Guice else it complains while running tests
 */
public class RestraintTestService implements RestraintService {
  @Override
  public ResourceConstraint get(String ownerId, String id) {
    return null;
  }

  @Override
  public ResourceConstraint save(ResourceConstraint resourceConstraint) {
    return null;
  }

  @Override
  public List<ResourceConstraint> getConstraintsIn(Set<String> constraintIds) {
    return null;
  }
}
