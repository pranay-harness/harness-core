package io.harness.accesscontrol.resources.resourcegroups.persistence;

import io.harness.accesscontrol.resources.resourcegroups.ResourceGroup;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;

import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

public interface ResourceGroupDao {
  ResourceGroup upsert(@NotNull @Valid ResourceGroup resourceGroup);

  PageResponse<ResourceGroup> list(@NotNull PageRequest pageRequest, @NotEmpty String scopeIdentifier);

  Optional<ResourceGroup> get(@NotEmpty String identifier, @NotEmpty String scopeIdentifier);

  Optional<ResourceGroup> delete(@NotEmpty String identifier, @NotEmpty String scopeIdentifier);
}
