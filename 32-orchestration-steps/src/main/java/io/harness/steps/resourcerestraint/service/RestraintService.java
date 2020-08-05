package io.harness.steps.resourcerestraint.service;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.steps.resourcerestraint.beans.ResourceRestraint;

import java.util.List;
import java.util.Set;
import javax.validation.Valid;

@OwnedBy(CDC)
public interface RestraintService<T extends ResourceRestraint> {
  T get(String ownerId, String id);
  T save(@Valid T resourceConstraint);
  List<T> getConstraintsIn(Set<String> constraintIds);
}
