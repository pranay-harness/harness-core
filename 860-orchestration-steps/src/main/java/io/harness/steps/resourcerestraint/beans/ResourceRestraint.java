package io.harness.steps.resourcerestraint.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.distribution.constraint.Constraint;

@OwnedBy(CDC)
public interface ResourceRestraint {
  String getUuid();
  String getName();
  int getCapacity();
  String getClaimant();
  Constraint.Strategy getStrategy();
}
