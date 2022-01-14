package io.harness.accesscontrol.resources.resourcetypes.persistence;

import io.harness.accesscontrol.resources.resourcetypes.ResourceType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(HarnessTeam.PL)
public interface ResourceTypeDao {
  ResourceType save(@Valid @NotNull ResourceType resourceType);
  Optional<ResourceType> get(@NotEmpty String identifier);
  Optional<ResourceType> getByPermissionKey(@NotEmpty String permissionKey);
  List<ResourceType> list();
}
