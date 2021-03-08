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
  @Builder.Default @NotNull Set<String> resourceGroupFilter = new HashSet<>();
  @Builder.Default @NotNull Set<String> roleFilter = new HashSet<>();
  @Builder.Default @NotNull Set<PrincipalType> principalTypeFilter = new HashSet<>();
  @Builder.Default @NotNull Set<Principal> principalFilter = new HashSet<>();
  @Builder.Default @NotNull Set<Boolean> managedFilter = new HashSet<>();
  @Builder.Default @NotNull Set<Boolean> disabledFilter = new HashSet<>();

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
