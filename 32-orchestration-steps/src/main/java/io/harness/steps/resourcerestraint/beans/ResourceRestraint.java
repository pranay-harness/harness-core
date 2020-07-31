package io.harness.steps.resourcerestraint.beans;

import io.harness.distribution.constraint.Constraint;

public interface ResourceRestraint {
  String getUuid();
  String getName();
  int getCapacity();
  String getClaimant();
  Constraint.Strategy getStrategy();
}
