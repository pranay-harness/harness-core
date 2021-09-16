package io.harness.resourcegroup.framework.service;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.ng.beans.PageRequest;
import io.harness.resourcegroup.model.ResourceGroup;
import io.harness.resourcegroup.remote.dto.ResourceGroupDTO;
import io.harness.resourcegroup.remote.dto.ResourceGroupFilterDTO;
import io.harness.resourcegroupclient.ResourceGroupResponse;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PL)
public interface ResourceGroupService {
  ResourceGroupResponse create(ResourceGroupDTO resourceGroupDTO);

  void createManagedResourceGroup(Scope scope);

  Optional<ResourceGroupResponse> get(Scope scope, String identifier);

  Page<ResourceGroupResponse> list(Scope scope, PageRequest pageRequest, String searchTerm);

  Page<ResourceGroupResponse> list(ResourceGroupFilterDTO resourceGroupFilterDTO, PageRequest pageRequest);

  Page<ResourceGroup> list(Criteria criteria, Pageable pageable);

  Optional<ResourceGroupResponse> update(ResourceGroupDTO resourceGroupDTO, boolean sanitizeResourceSelectors);

  void delete(Scope scope, String identifier);

  void deleteByScope(Scope scope);
}
