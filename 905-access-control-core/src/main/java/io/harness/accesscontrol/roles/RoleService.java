package io.harness.accesscontrol.roles;

import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;

import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

public interface RoleService {
  Role create(@Valid Role role);

  PageResponse<Role> getAll(@NotNull PageRequest pageRequest, String parentIdentifier, boolean includeManaged);

  Optional<Role> get(@NotEmpty String identifier, @NotEmpty String parentIdentifier);

  Role update(@Valid Role role);

  Optional<Role> delete(@NotEmpty String identifier, @NotEmpty String parentIdentifier, boolean removeManagedRole);
}
