package io.harness.accesscontrol.resources.resourcegroups;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.annotations.dev.OwnedBy;

import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(PL)
public interface HarnessResourceGroupService {
  void sync(@NotEmpty String identifier, @NotNull Scope scope);
  void deleteIfPresent(@NotEmpty String identifier, @NotNull Scope scope);
}
