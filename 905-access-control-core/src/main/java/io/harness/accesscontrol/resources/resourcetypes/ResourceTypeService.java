package io.harness.accesscontrol.resources.resourcetypes;

import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

public interface ResourceTypeService {
  ResourceType save(@NotNull ResourceType resourceType);
  Optional<ResourceType> get(@NotEmpty String identifier);
  Optional<ResourceType> getByPermissionKey(@NotEmpty String permissionKey);
  List<ResourceType> list();
}
