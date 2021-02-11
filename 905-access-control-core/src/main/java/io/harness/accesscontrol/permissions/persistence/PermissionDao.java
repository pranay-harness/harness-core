package io.harness.accesscontrol.permissions.persistence;

import io.harness.accesscontrol.permissions.Permission;
import io.harness.accesscontrol.scopes.Scope;

import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import org.hibernate.validator.constraints.NotEmpty;

public interface PermissionDao {
  Permission create(@Valid Permission permission);

  List<Permission> list(Scope scope, String resourceType);

  Optional<Permission> get(@NotEmpty String identifier);

  String update(@Valid Permission permission);

  void delete(@NotEmpty String identifier);
}
