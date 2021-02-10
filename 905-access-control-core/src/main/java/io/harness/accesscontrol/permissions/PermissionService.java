package io.harness.accesscontrol.permissions;

import io.harness.accesscontrol.scopes.Scope;

import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

public interface PermissionService {
  Permission create(@Valid Permission permission);

  Optional<Permission> get(@NotEmpty String identifier);

  List<Permission> list(@NotNull Scope scope, String resourceType);

  String update(@Valid Permission permission);

  void delete(@NotEmpty String identifier);
}
