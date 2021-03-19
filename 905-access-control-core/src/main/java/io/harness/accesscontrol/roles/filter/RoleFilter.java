package io.harness.accesscontrol.roles.filter;

import io.harness.accesscontrol.common.filter.ManagedFilter;
import io.harness.accesscontrol.roles.validator.ValidRoleFilter;

import java.util.HashSet;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@ValidRoleFilter
public class RoleFilter {
  String scopeIdentifier;
  boolean includeChildScopes;
  @Builder.Default @NotNull Set<String> identifierFilter = new HashSet<>();
  @Builder.Default @NotNull Set<String> permissionFilter = new HashSet<>();
  @Builder.Default @NotNull ManagedFilter managedFilter;
}
