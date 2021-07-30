package io.harness.accesscontrol.scopes.harness;

import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@OwnedBy(HarnessTeam.PL)
public interface HarnessScopeService {
  void sync(@NotNull @Valid Scope scope);
  void deleteIfPresent(@NotNull @Valid Scope scope);
}
