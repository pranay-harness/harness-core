package io.harness.accesscontrol.scopes.core;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public interface ScopeService {
  Scope buildScopeFromParams(@NotNull @Valid ScopeParams scopeParams);

  Scope buildScopeFromScopeIdentifier(@NotNull String scopeIdentifier);
}
