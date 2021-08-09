package io.harness.accesscontrol.roles.filter;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.common.filter.ManagedFilter;
import io.harness.accesscontrol.roles.validator.ValidRoleFilter;
import io.harness.annotations.dev.OwnedBy;

import java.util.HashSet;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@OwnedBy(PL)
@Value
@Builder
@ValidRoleFilter
public class RoleFilter {
  String searchTerm;
  String scopeIdentifier;
  boolean includeChildScopes;
  @Builder.Default Set<String> scopeLevelsFilter = new HashSet<>();
  @Builder.Default @NotNull Set<String> identifierFilter = new HashSet<>();
  @Builder.Default @NotNull Set<String> permissionFilter = new HashSet<>();
  @Builder.Default @NotNull ManagedFilter managedFilter;
}
