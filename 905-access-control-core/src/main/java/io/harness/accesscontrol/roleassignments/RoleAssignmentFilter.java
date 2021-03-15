package io.harness.accesscontrol.roleassignments;

import static io.harness.accesscontrol.common.filter.ManagedFilter.NO_FILTER;

import io.harness.accesscontrol.common.filter.ManagedFilter;
import io.harness.accesscontrol.principals.Principal;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.roleassignments.validator.ValidRoleAssignmentFilter;

import java.util.HashSet;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@Builder
@ValidRoleAssignmentFilter
public class RoleAssignmentFilter {
  @NotEmpty String scopeFilter;
  boolean includeChildScopes;
  @Builder.Default @NotNull Set<String> resourceGroupFilter = new HashSet<>();
  @Builder.Default @NotNull Set<String> roleFilter = new HashSet<>();
  @Builder.Default @NotNull Set<PrincipalType> principalTypeFilter = new HashSet<>();
  @Builder.Default @NotNull Set<Principal> principalFilter = new HashSet<>();
  @Builder.Default @NotNull ManagedFilter managedFilter = NO_FILTER;
  @Builder.Default @NotNull Set<Boolean> disabledFilter = new HashSet<>();
}
