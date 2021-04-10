package io.harness.accesscontrol.scopes.core;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@OwnedBy(HarnessTeam.PL)
public interface ScopeService {
  Scope buildScopeFromParams(@NotNull @Valid ScopeParams scopeParams);

  Scope buildScopeFromScopeIdentifier(@NotNull String scopeIdentifier);

  boolean areScopeLevelsValid(@NotNull Set<String> scopeLevels);

  Set<String> getAllScopeLevels();
}
