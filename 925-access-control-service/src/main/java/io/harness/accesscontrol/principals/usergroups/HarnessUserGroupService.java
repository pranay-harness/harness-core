package io.harness.accesscontrol.principals.usergroups;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.annotations.dev.OwnedBy;

import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(PL)
public interface HarnessUserGroupService {
  void sync(@NotEmpty String identifier, @NotNull Scope scope);
  void deleteIfPresent(@NotEmpty String identifier, @NotNull Scope scope);
}
