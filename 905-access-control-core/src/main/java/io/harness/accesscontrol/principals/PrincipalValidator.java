package io.harness.accesscontrol.principals;

import io.harness.accesscontrol.common.validation.ValidationResult;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public interface PrincipalValidator {
  PrincipalType getPrincipalType();
  ValidationResult validatePrincipal(@NotNull @Valid Principal principal);
}
