package io.harness.accesscontrol.roles.persistence;

import io.harness.accesscontrol.roles.Role;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;

import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

public interface RoleDao {
  Role create(@Valid Role role);

  PageResponse<Role> getAll(@NotNull PageRequest pageRequest, String scopeIdentifier, boolean includeManaged);

  Optional<Role> get(@NotEmpty String identifier, @NotEmpty String scopeIdentifier);

  Role update(@Valid Role role);

  Optional<Role> delete(@NotEmpty String identifier, @NotEmpty String scopeIdentifier);
}
