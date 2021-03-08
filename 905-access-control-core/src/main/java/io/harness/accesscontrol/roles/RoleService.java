package io.harness.accesscontrol.roles;

import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;

import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

public interface RoleService {
  Role create(@NotNull @Valid Role role);

  PageResponse<Role> list(@NotNull PageRequest pageRequest, String scopeIdentifier, boolean includeManaged);

  List<Role> list(List<String> roleIdentifiers, String scopeIdentifier);

  Optional<Role> get(@NotEmpty String identifier, String scopeIdentifier, boolean isManaged);

  Role update(@NotNull @Valid Role role);

  boolean removePermissionFromRoles(@NotEmpty String permissionIdentifier);

  Role delete(@NotEmpty String identifier, String scopeIdentifier, boolean isManaged);
}
