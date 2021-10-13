package io.harness.resourcegroup.framework.service;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.ng.beans.PageRequest;
import io.harness.resourcegroup.remote.dto.ManagedFilter;
import io.harness.resourcegroup.remote.dto.ResourceGroupDTO;
import io.harness.resourcegroup.remote.dto.ResourceGroupFilterDTO;
import io.harness.resourcegroupclient.ResourceGroupResponse;

import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.domain.Page;

@OwnedBy(PL)
public interface ResourceGroupService {
  ResourceGroupResponse create(ResourceGroupDTO resourceGroupDTO, boolean harnessManaged);

  Optional<ResourceGroupResponse> get(
      @NotNull Scope scope, @NotEmpty String identifier, @NotNull ManagedFilter managedFilter);

  Page<ResourceGroupResponse> list(Scope scope, PageRequest pageRequest, String searchTerm);

  Page<ResourceGroupResponse> list(
      @NotNull @Valid ResourceGroupFilterDTO resourceGroupFilterDTO, @NotNull PageRequest pageRequest);

  Optional<ResourceGroupResponse> update(
      ResourceGroupDTO resourceGroupDTO, boolean sanitizeResourceSelectors, boolean harnessManaged);

  void delete(Scope scope, String identifier);

  void deleteManaged(@NotEmpty String identifier);

  void deleteByScope(Scope scope);
}
