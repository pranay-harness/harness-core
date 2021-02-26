package io.harness.accesscontrol.resources.resourcetypes.persistence;

import io.harness.accesscontrol.resources.resourcetypes.ResourceType;

import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

public interface ResourceTypeDao {
  ResourceType save(@Valid @NotNull ResourceType resourceType);
  Optional<ResourceType> get(@NotEmpty String identifier);
  Optional<ResourceType> getByPermissionKey(@NotEmpty String permissionKey);
  List<ResourceType> list();
}
