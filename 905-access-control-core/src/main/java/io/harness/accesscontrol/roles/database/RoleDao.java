package io.harness.accesscontrol.roles.database;

import io.harness.accesscontrol.roles.RoleDTO;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;

import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

public interface RoleDao {
  RoleDTO create(@Valid RoleDTO roleDTO);

  PageResponse<RoleDTO> getAll(@NotNull PageRequest pageRequest, String parentIdentifier, boolean includeDefault);

  Optional<RoleDTO> get(@NotEmpty String identifier, @NotEmpty String parentIdentifier);

  RoleDTO update(@Valid RoleDTO roleDTO);

  RoleDTO delete(@NotEmpty String identifier, @NotEmpty String parentIdentifier);
}
