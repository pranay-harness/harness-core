package io.harness.steps.resourcerestraint.service;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.steps.resourcerestraint.beans.ResourceRestraint;
import io.harness.validation.Create;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;

import javax.validation.Valid;

@OwnedBy(CDC)
public interface ResourceRestraintService<T extends ResourceRestraint> {
  T get(String ownerId, String id);
  @ValidationGroups(Create.class) T save(@Valid T resourceConstraint);
}
