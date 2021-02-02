package io.harness.accesscontrol.permissions.core;

import io.harness.accesscontrol.permissions.PermissionDTO;

import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import org.hibernate.validator.constraints.NotEmpty;

public interface PermissionService {
  String create(@Valid PermissionDTO permission);

  Optional<PermissionDTO> get(@NotEmpty String identifier);

  List<PermissionDTO> list(String scope);

  String update(@Valid PermissionDTO permissionDTO);

  void delete(@NotEmpty String identifier);
}
