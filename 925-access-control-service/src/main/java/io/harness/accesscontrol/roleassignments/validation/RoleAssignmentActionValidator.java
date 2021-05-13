package io.harness.accesscontrol.roleassignments.validation;

import static io.harness.accesscontrol.principals.PrincipalType.USER;

import io.harness.accesscontrol.common.validation.ValidationResult;
import io.harness.accesscontrol.commons.validation.HarnessActionValidator;
import io.harness.accesscontrol.roleassignments.RoleAssignment;
import io.harness.accesscontrol.roleassignments.RoleAssignmentFilter;
import io.harness.accesscontrol.roleassignments.RoleAssignmentFilter.RoleAssignmentFilterBuilder;
import io.harness.accesscontrol.roleassignments.RoleAssignmentService;
import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.accesscontrol.scopes.harness.HarnessScopeLevel;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

public class RoleAssignmentActionValidator implements HarnessActionValidator<RoleAssignment> {
  private final RoleAssignmentService roleAssignmentService;
  private final ScopeService scopeService;
  private static final String PROJECT_ADMIN = "_project_admin";
  private static final String ORG_ADMIN = "_organization_admin";
  private static final String ACCOUNT_ADMIN = "_account_admin";
  private static final String RESOURCE_GROUP_IDENTIFIER = "_all_resources";

  @Inject
  public RoleAssignmentActionValidator(RoleAssignmentService roleAssignmentService, ScopeService scopeService) {
    this.roleAssignmentService = roleAssignmentService;
    this.scopeService = scopeService;
  }

  @Override
  public ValidationResult canDelete(RoleAssignment roleAssignment) {
    if (roleAssignment.isManaged()) {
      return ValidationResult.builder().valid(false).errorMessage("Cannot delete a managed role assignment").build();
    }
    Scope scope = scopeService.buildScopeFromScopeIdentifier(roleAssignment.getScopeIdentifier());
    if (!RESOURCE_GROUP_IDENTIFIER.equals(roleAssignment.getResourceGroupIdentifier())) {
      return ValidationResult.builder().valid(true).build();
    }
    RoleAssignmentFilterBuilder builder = RoleAssignmentFilter.builder()
                                              .resourceGroupFilter(Sets.newHashSet(RESOURCE_GROUP_IDENTIFIER))
                                              .principalTypeFilter(Sets.newHashSet(USER));
    RoleAssignmentFilter roleAssignmentFilter;
    if (HarnessScopeLevel.ACCOUNT.equals(scope.getLevel())
        && ACCOUNT_ADMIN.equals(roleAssignment.getRoleIdentifier())) {
      roleAssignmentFilter =
          builder.scopeFilter(roleAssignment.getScopeIdentifier()).roleFilter(Sets.newHashSet(ACCOUNT_ADMIN)).build();
    } else if (HarnessScopeLevel.ORGANIZATION.equals(scope.getLevel())
        && ORG_ADMIN.equals(roleAssignment.getRoleIdentifier())) {
      roleAssignmentFilter =
          builder.scopeFilter(roleAssignment.getScopeIdentifier()).roleFilter(Sets.newHashSet(ORG_ADMIN)).build();
    } else if (HarnessScopeLevel.PROJECT.equals(scope.getLevel())
        && PROJECT_ADMIN.equals(roleAssignment.getRoleIdentifier())) {
      roleAssignmentFilter =
          builder.scopeFilter(roleAssignment.getScopeIdentifier()).roleFilter(Sets.newHashSet(PROJECT_ADMIN)).build();
    } else {
      return ValidationResult.builder().valid(true).build();
    }
    PageResponse<RoleAssignment> response =
        roleAssignmentService.list(PageRequest.builder().pageSize(1).build(), roleAssignmentFilter);
    if (response.getTotalItems() < 2) {
      return ValidationResult.builder()
          .valid(false)
          .errorMessage(
              "Please add another Admin assigned to All Resources Resource Group before deleting this role assignment.")
          .build();
    }
    return ValidationResult.builder().valid(true).build();
  }

  @Override
  public ValidationResult canCreate(RoleAssignment object) {
    return ValidationResult.builder().valid(true).build();
  }

  @Override
  public ValidationResult canUpdate(RoleAssignment object) {
    return ValidationResult.builder().valid(true).build();
  }
}
