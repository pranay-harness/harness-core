package io.harness.accesscontrol.roleassignments;

import static io.harness.accesscontrol.common.filter.ManagedFilter.NO_FILTER;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.common.filter.ManagedFilter;
import io.harness.accesscontrol.principals.Principal;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.roleassignments.validator.ValidRoleAssignmentFilter;
import io.harness.annotations.dev.OwnedBy;

import java.util.HashSet;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(PL)
@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@ValidRoleAssignmentFilter
public class RoleAssignmentFilter {
  @NotEmpty final String scopeFilter;
  final boolean includeChildScopes;
  @Builder.Default @NotNull @Size(max = 100) final Set<String> scopeLevelFilter = new HashSet<>();
  @Builder.Default @NotNull @Size(max = 100) final Set<String> resourceGroupFilter = new HashSet<>();
  @Builder.Default @NotNull @Size(max = 100) final Set<String> roleFilter = new HashSet<>();
  @Setter @Builder.Default @NotNull @Size(max = 100) Set<PrincipalType> principalTypeFilter = new HashSet<>();
  @Setter @Builder.Default @NotNull @Size(max = 100) Set<Principal> principalFilter = new HashSet<>();
  @Builder.Default @NotNull @Valid final ManagedFilter managedFilter = NO_FILTER;
  @Builder.Default @NotNull @Size(max = 100) final Set<Boolean> disabledFilter = new HashSet<>();
}
