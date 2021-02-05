package io.harness.accesscontrol.permissions.database;

import io.harness.accesscontrol.permissions.PermissionDTO;
import io.harness.accesscontrol.scopes.Scope;

import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import org.hibernate.validator.constraints.NotEmpty;

public interface PermissionDao {
  String create(@Valid PermissionDTO permission);

  List<PermissionDTO> list(@NotEmpty Scope scope, String resourceType);

  Optional<PermissionDTO> get(@NotEmpty String identifier);

  String update(@Valid PermissionDTO permissionDTO);

  void delete(@NotEmpty String identifier);
}
