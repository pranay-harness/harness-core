package io.harness.accesscontrol.roles.persistence;

import io.harness.accesscontrol.roles.Role;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

public interface RoleDao {
  Role create(@NotNull @Valid Role role);

  PageResponse<Role> list(@NotNull PageRequest pageRequest, String scopeIdentifier, boolean includeManaged,
      @NotNull Set<String> allowedScopeLevels);

  List<Role> list(List<String> roleIdentifiers, String scopeIdentifier);

  Optional<Role> get(@NotEmpty String identifier, String scopeIdentifier, boolean isManaged);

  Role update(@NotNull @Valid Role role);

  Optional<Role> delete(@NotEmpty String identifier, String scopeIdentifier);

  boolean removePermissionFromRoles(@NotEmpty String permissionIdentifier);
}
