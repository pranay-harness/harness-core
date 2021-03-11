package io.harness.accesscontrol.principals;

import io.harness.accesscontrol.scopes.core.Scope;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public interface PrincipalValidator {
  PrincipalType getPrincipalType();
  void validatePrincipal(@NotNull @Valid Principal principal, @NotNull @Valid Scope scope);
}
