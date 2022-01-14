package io.harness.accesscontrol.resources.resourcegroups.persistence;

import io.harness.accesscontrol.common.filter.ManagedFilter;
import io.harness.accesscontrol.resources.resourcegroups.ResourceGroup;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;

import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(HarnessTeam.PL)
public interface ResourceGroupDao {
  ResourceGroup upsert(@NotNull @Valid ResourceGroup resourceGroup);

  PageResponse<ResourceGroup> list(@NotNull PageRequest pageRequest, @NotEmpty String scopeIdentifier);

  List<ResourceGroup> list(List<String> resourceGroupIdentifier, String scopeIdentifier, ManagedFilter managedFilter);

  Optional<ResourceGroup> get(@NotEmpty String identifier, String scopeIdentifier, ManagedFilter managedFilter);

  Optional<ResourceGroup> delete(@NotEmpty String identifier, String scopeIdentifier);
}
