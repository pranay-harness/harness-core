package io.harness.accesscontrol.principals.users;

import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(HarnessTeam.PL)
public interface HarnessUserService {
  void sync(@NotEmpty String identifier, @NotNull Scope scope);
}
