package io.harness.accesscontrol.roleassignments.validator;

import static io.harness.accesscontrol.common.filter.ManagedFilter.NO_FILTER;

import io.harness.accesscontrol.principals.Principal;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.principals.PrincipalValidator;
import io.harness.accesscontrol.resources.resourcegroups.ResourceGroup;
import io.harness.accesscontrol.resources.resourcegroups.ResourceGroupService;
import io.harness.accesscontrol.roleassignments.RoleAssignment;
import io.harness.accesscontrol.roles.Role;
import io.harness.accesscontrol.roles.RoleService;
import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.exception.InvalidRequestException;

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
  public void validate(RoleAssignment roleAssignment, Scope scope) {
    validatePrincipal(Principal.builder()
                          .principalIdentifier(roleAssignment.getPrincipalIdentifier())
                          .principalType(roleAssignment.getPrincipalType())
                          .build(),
        scope);
    validateRole(roleAssignment.getRoleIdentifier(), scope);
    validateResourceGroup(roleAssignment.getResourceGroupIdentifier(), scope);
  }

  private void validatePrincipal(Principal principal, Scope scope) {
    PrincipalValidator principalValidator = principalValidatorByType.get(principal.getPrincipalType());
    if (principalValidator == null) {
      throw new InvalidRequestException(
          String.format("Incorrect Principal Type. Please select one out of %s", principalValidatorByType.keySet()));
    }
    principalValidator.validatePrincipal(principal, scope);
  }

  private void validateRole(String roleIdentifier, Scope scope) {
    Optional<Role> role = roleService.get(roleIdentifier, scope.toString(), NO_FILTER);
    if (!role.isPresent()) {
      throw new InvalidRequestException(String.format("Did not find role in %s", scope.toString()));
    }
  }

  private void validateResourceGroup(String resourceGroupIdentifier, Scope scope) {
    Optional<ResourceGroup> resourceGroup = resourceGroupService.get(resourceGroupIdentifier, scope.toString());
    if (!resourceGroup.isPresent()) {
      throw new InvalidRequestException(
          String.format("Did not find resource group identifier in %s", scope.toString()));
    }
  }
}
