package io.harness.accesscontrol.roles.database;

import io.harness.accesscontrol.roles.RoleDTO;

import java.util.Optional;
import javax.validation.Valid;
import org.hibernate.validator.constraints.NotEmpty;

public interface RoleDao {
  String create(@Valid RoleDTO roleDTO);

  Optional<RoleDTO> get(@NotEmpty String identifier, @NotEmpty String parentIdentifier);

  String update(@Valid RoleDTO permissionDTO);

  void delete(@NotEmpty String identifier, @NotEmpty String parentIdentifier);
}
