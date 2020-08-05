package io.harness.steps.resourcerestraint;

import io.harness.steps.resourcerestraint.beans.ResourceRestraint;
import io.harness.steps.resourcerestraint.service.RestraintService;

import java.util.List;
import java.util.Set;

/**
 * The type Restraint Service.
 * This is only to provide a Restraint Service Binding to Guice else it complains while running tests
 */
public class RestraintTestService implements RestraintService<ResourceRestraint> {
  @Override
  public ResourceRestraint get(String ownerId, String id) {
    return null;
  }

  @Override
  public ResourceRestraint save(ResourceRestraint resourceConstraint) {
    return null;
  }

  @Override
  public List<ResourceRestraint> getConstraintsIn(Set<String> constraintIds) {
    return null;
  }
}
