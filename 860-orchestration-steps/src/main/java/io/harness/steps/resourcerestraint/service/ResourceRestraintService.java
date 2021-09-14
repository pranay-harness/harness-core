/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.steps.resourcerestraint.service;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.steps.resourcerestraint.beans.ResourceRestraint;

import java.util.List;
import java.util.Set;
import javax.validation.Valid;

@OwnedBy(PIPELINE)
public interface ResourceRestraintService {
  ResourceRestraint get(String ownerId, String id);
  ResourceRestraint getByNameAndAccountId(String name, String accountId);
  ResourceRestraint save(@Valid ResourceRestraint resourceConstraint);
  List<ResourceRestraint> getConstraintsIn(Set<String> constraintIds);
}
