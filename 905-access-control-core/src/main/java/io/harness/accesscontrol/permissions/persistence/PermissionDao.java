package io.harness.accesscontrol.permissions.persistence;

import io.harness.accesscontrol.permissions.Permission;
import io.harness.accesscontrol.scopes.Scope;

import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

public interface PermissionDao {
  String create(@Valid Permission permission);

  List<Permission> list(@NotNull Scope scope, String resourceType);

  Optional<Permission> get(@NotEmpty String identifier);

  String update(@Valid Permission permission);

  void delete(@NotEmpty String identifier);
}
