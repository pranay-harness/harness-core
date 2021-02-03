package io.harness.accesscontrol.roles;

import java.util.Optional;
import javax.validation.Valid;
import org.hibernate.validator.constraints.NotEmpty;

interface RoleService {
  String create(@Valid RoleDTO roleDTO);

  Optional<RoleDTO> get(@NotEmpty String identifier, @NotEmpty String parentIdentifier);

  String update(@Valid RoleDTO roleDTO);

  RoleDTO delete(@NotEmpty String identifier, @NotEmpty String parentIdentifier);
}
