/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.roleassignments.validator;

import static io.harness.accesscontrol.common.filter.ManagedFilter.NO_FILTER;
import static io.harness.accesscontrol.common.validation.ValidationResult.VALID;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.common.validation.ValidationResult;
import io.harness.accesscontrol.principals.Principal;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.principals.PrincipalValidator;
import io.harness.accesscontrol.resources.resourcegroups.ResourceGroup;
import io.harness.accesscontrol.resources.resourcegroups.ResourceGroupService;
import io.harness.accesscontrol.roleassignments.RoleAssignment;
import io.harness.accesscontrol.roleassignments.validator.RoleAssignmentValidationResult.RoleAssignmentValidationResultBuilder;
import io.harness.accesscontrol.roles.Role;
import io.harness.accesscontrol.roles.RoleService;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
@Singleton
public class RoleAssignmentValidatorImpl implements RoleAssignmentValidator {
  private final Map<PrincipalType, PrincipalValidator> principalValidatorByType;
  private final RoleService roleService;
  private final ResourceGroupService resourceGroupService;
  private final ScopeService scopeService;

  @Inject
  public RoleAssignmentValidatorImpl(Map<PrincipalType, PrincipalValidator> principalValidatorByType,
      RoleService roleService, ResourceGroupService resourceGroupService, ScopeService scopeService) {
    this.principalValidatorByType = principalValidatorByType;
    this.roleService = roleService;
    this.resourceGroupService = resourceGroupService;
    this.scopeService = scopeService;
  }

  @Override
  public RoleAssignmentValidationResult validate(RoleAssignmentValidationRequest request) {
    RoleAssignment assignment = request.getRoleAssignment();
    RoleAssignmentValidationResultBuilder builder = RoleAssignmentValidationResult.builder()
                                                        .scopeValidationResult(VALID)
                                                        .principalValidationResult(VALID)
                                                        .resourceGroupValidationResult(VALID)
                                                        .roleValidationResult(VALID);
    if (request.isValidateScope()) {
      builder.scopeValidationResult(validateScope(assignment.getScopeIdentifier()));
    }

    if (request.isValidatePrincipal()) {
      builder.principalValidationResult(validatePrincipal(Principal.builder()
                                                              .principalIdentifier(assignment.getPrincipalIdentifier())
                                                              .principalType(assignment.getPrincipalType())
                                                              .build(),
          assignment.getScopeIdentifier()));
    }
    if (request.isValidateResourceGroup()) {
      builder.resourceGroupValidationResult(
          validateResourceGroup(assignment.getResourceGroupIdentifier(), assignment.getScopeIdentifier()));
    }
    if (request.isValidateRole()) {
      builder.roleValidationResult(validateRole(assignment.getRoleIdentifier(), assignment.getScopeIdentifier()));
    }
    return builder.build();
  }

  private ValidationResult validateScope(String scopeIdentifier) {
    if (scopeService.isPresent(scopeIdentifier)) {
      return ValidationResult.builder().valid(true).build();
    }
    return ValidationResult.builder()
        .valid(false)
        .errorMessage(String.format("Invalid scope %s", scopeIdentifier))
        .build();
  }

  private ValidationResult validatePrincipal(Principal principal, String scopeIdentifier) {
    PrincipalValidator principalValidator = principalValidatorByType.get(principal.getPrincipalType());
    if (principalValidator == null) {
      return ValidationResult.builder()
          .valid(false)
          .errorMessage(
              String.format("Incorrect Principal Type. Please select one out of %s", principalValidatorByType.keySet()))
          .build();
    }
    return principalValidator.validatePrincipal(principal, scopeIdentifier);
  }

  private ValidationResult validateRole(String roleIdentifier, String scopeIdentifier) {
    Optional<Role> role = roleService.get(roleIdentifier, scopeIdentifier, NO_FILTER);
    if (!role.isPresent()) {
      return ValidationResult.builder()
          .valid(false)
          .errorMessage(String.format("Did not find role in %s", scopeIdentifier))
          .build();
    }
    return ValidationResult.builder().valid(true).build();
  }

  private ValidationResult validateResourceGroup(String resourceGroupIdentifier, String scopeIdentifier) {
    Optional<ResourceGroup> resourceGroup =
        resourceGroupService.get(resourceGroupIdentifier, scopeIdentifier, NO_FILTER);
    if (!resourceGroup.isPresent()) {
      return ValidationResult.builder()
          .valid(false)
          .errorMessage(String.format("Did not find resource group identifier in %s", scopeIdentifier))
          .build();
    }
    return ValidationResult.builder().valid(true).build();
  }
}
