package io.harness.accesscontrol.resources.resourcegroups;

import io.harness.accesscontrol.scopes.core.Scope;

import org.hibernate.validator.constraints.NotEmpty;

public interface HarnessResourceGroupService {
  void sync(@NotEmpty String identifier, @NotEmpty Scope scope);
}
