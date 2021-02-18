package io.harness.accesscontrol.scopes.core;

import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public interface ScopeService {
  Scope buildScopeFromParams(@NotNull @Valid ScopeParams scopeParams);

  Scope buildScopeFromScopeIdentifier(@NotNull String scopeIdentifier);

  boolean areScopeLevelsValid(@NotNull Set<String> scopeLevels);

  Set<String> getAllScopeLevels();
}
