/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.accesscontrol.principals.usergroups;

import static io.harness.accesscontrol.principals.PrincipalType.USER_GROUP;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.common.validation.ValidationResult;
import io.harness.accesscontrol.principals.Principal;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.principals.PrincipalValidator;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import javax.validation.executable.ValidateOnExecution;

@OwnedBy(PL)
@Singleton
@ValidateOnExecution
public class UserGroupValidator implements PrincipalValidator {
  private final UserGroupService userGroupService;

  @Inject
  public UserGroupValidator(UserGroupService userGroupService) {
    this.userGroupService = userGroupService;
  }

  @Override
  public PrincipalType getPrincipalType() {
    return USER_GROUP;
  }

  @Override
  public ValidationResult validatePrincipal(Principal principal, String scopeIdentifier) {
    String identifier = principal.getPrincipalIdentifier();
    Optional<UserGroup> userGroupOptional = userGroupService.get(identifier, scopeIdentifier);
    if (userGroupOptional.isPresent()) {
      return ValidationResult.builder().valid(true).build();
    }
    return ValidationResult.builder()
        .valid(false)
        .errorMessage(String.format(
            "user group not found with the given identifier %s in the scope %s", identifier, scopeIdentifier))
        .build();
  }
}
