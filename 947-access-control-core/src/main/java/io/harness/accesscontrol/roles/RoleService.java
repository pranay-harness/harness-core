package io.harness.accesscontrol.roles;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.common.filter.ManagedFilter;
import io.harness.accesscontrol.roles.filter.RoleFilter;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;

import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(PL)
public interface RoleService {
  Role create(@NotNull @Valid Role role);

  PageResponse<Role> list(@NotNull PageRequest pageRequest, @Valid @NotNull RoleFilter roleFilter);

  Optional<Role> get(@NotEmpty String identifier, String scopeIdentifier, @NotNull ManagedFilter managedFilter);

  RoleUpdateResult update(@NotNull @Valid Role role);

  boolean removePermissionFromRoles(@NotEmpty String permissionIdentifier, @Valid @NotNull RoleFilter roleFilter);

  boolean addPermissionToRoles(@NotEmpty String permissionIdentifier, @Valid @NotNull RoleFilter roleFilter);

  Role delete(@NotEmpty String identifier, @NotEmpty String scopeIdentifier);

  Role deleteManaged(@NotEmpty String identifier);

  long deleteMulti(@Valid @NotNull RoleFilter roleFilter);
}
