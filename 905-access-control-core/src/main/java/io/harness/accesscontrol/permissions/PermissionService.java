package io.harness.accesscontrol.permissions;

import io.harness.accesscontrol.scopes.Scope;

import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import org.hibernate.validator.constraints.NotEmpty;

public interface PermissionService {
  String create(@Valid PermissionDTO permission);

  Optional<PermissionDTO> get(@NotEmpty String identifier);

  List<PermissionDTO> list(Scope scope, String resourceType);

  String update(@Valid PermissionDTO permissionDTO);

  void delete(@NotEmpty String identifier);
}
