package io.harness.accesscontrol.roleassignments.validator;

import static io.harness.accesscontrol.common.filter.ManagedFilter.NO_FILTER;

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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class RoleAssignmentValidatorImpl implements RoleAssignmentValidator {
  private final Map<PrincipalType, PrincipalValidator> principalValidatorByType;
  private final RoleService roleService;
  private final ResourceGroupService resourceGroupService;

  @Inject
  public RoleAssignmentValidatorImpl(Map<PrincipalType, PrincipalValidator> principalValidatorByType,
      RoleService roleService, ResourceGroupService resourceGroupService) {
    this.principalValidatorByType = principalValidatorByType;
    this.roleService = roleService;
    this.resourceGroupService = resourceGroupService;
  }

  @Override
  public RoleAssignmentValidationResult validate(RoleAssignmentValidationRequest request) {
    RoleAssignment assignment = request.getRoleAssignment();
    RoleAssignmentValidationResultBuilder builder = RoleAssignmentValidationResult.builder();
    if (request.isValidatePrincipal()) {
      builder.principalValidationResult(validatePrincipal(Principal.builder()
                                                              .principalIdentifier(assignment.getPrincipalIdentifier())
                                                              .principalType(assignment.getPrincipalType())
                                                              .build()));
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

  private ValidationResult validatePrincipal(Principal principal) {
    PrincipalValidator principalValidator = principalValidatorByType.get(principal.getPrincipalType());
    if (principalValidator == null) {
      return ValidationResult.builder()
          .valid(false)
          .errorMessage(
              String.format("Incorrect Principal Type. Please select one out of %s", principalValidatorByType.keySet()))
          .build();
    }
    return principalValidator.validatePrincipal(principal);
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
    Optional<ResourceGroup> resourceGroup = resourceGroupService.get(resourceGroupIdentifier, scopeIdentifier);
    if (!resourceGroup.isPresent()) {
      return ValidationResult.builder()
          .valid(false)
          .errorMessage(String.format("Did not find resource group identifier in %s", scopeIdentifier))
          .build();
    }
    return ValidationResult.builder().valid(true).build();
  }
}
