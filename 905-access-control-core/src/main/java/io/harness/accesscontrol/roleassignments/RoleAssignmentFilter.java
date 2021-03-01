package io.harness.accesscontrol.roleassignments;

import io.harness.accesscontrol.principals.Principal;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.roleassignments.validator.ValidRoleAssignmentFilter;

import java.util.HashSet;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@ValidRoleAssignmentFilter
public class RoleAssignmentFilter {
  @NotNull Set<String> resourceGroupFilter;
  @NotNull Set<String> roleFilter;
  @NotNull Set<PrincipalType> principalTypeFilter;
  @NotNull Set<Principal> principalFilter;
  @NotNull Set<Boolean> managedFilter;
  @NotNull Set<Boolean> disabledFilter;

  public boolean isEmpty() {
    return resourceGroupFilter.isEmpty() && roleFilter.isEmpty() && principalFilter.isEmpty()
        && principalTypeFilter.isEmpty() && managedFilter.isEmpty() && disabledFilter.isEmpty();
  }

  public static RoleAssignmentFilter buildEmpty() {
    return RoleAssignmentFilter.builder()
        .principalFilter(new HashSet<>())
        .principalTypeFilter(new HashSet<>())
        .resourceGroupFilter(new HashSet<>())
        .roleFilter(new HashSet<>())
        .managedFilter(new HashSet<>())
        .disabledFilter(new HashSet<>())
        .build();
  }
}
