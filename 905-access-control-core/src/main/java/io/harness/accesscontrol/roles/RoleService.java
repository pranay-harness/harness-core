package io.harness.accesscontrol.roles;

import io.harness.accesscontrol.common.filter.ManagedFilter;
import io.harness.accesscontrol.roles.filter.RoleFilter;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;

import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

public interface RoleService {
  Role create(@NotNull @Valid Role role);

  PageResponse<Role> list(@NotNull PageRequest pageRequest, @Valid @NotNull RoleFilter roleFilter);

  Optional<Role> get(@NotEmpty String identifier, String scopeIdentifier, @NotNull ManagedFilter managedFilter);

  Role update(@NotNull @Valid Role role);

  boolean removePermissionFromRoles(@NotEmpty String permissionIdentifier);

  Role delete(@NotEmpty String identifier, @NotEmpty String scopeIdentifier);

  long deleteMulti(@Valid @NotNull RoleFilter roleFilter);
}
