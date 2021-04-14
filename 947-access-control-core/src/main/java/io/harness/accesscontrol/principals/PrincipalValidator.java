package io.harness.accesscontrol.principals;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.common.validation.ValidationResult;
import io.harness.annotations.dev.OwnedBy;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(PL)
public interface PrincipalValidator {
  PrincipalType getPrincipalType();
  ValidationResult validatePrincipal(@NotNull @Valid Principal principal, @NotEmpty String scopeIdentifier);
}
