package io.harness.resourcegroup.framework.service;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.beans.PageRequest;
import io.harness.resourcegroup.model.ResourceGroup;
import io.harness.resourcegroup.remote.dto.ResourceGroupDTO;
import io.harness.resourcegroupclient.ResourceGroupResponse;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PL)
public interface ResourceGroupService {
  ResourceGroupResponse create(ResourceGroupDTO resourceGroupDTO);

  ResourceGroupResponse createManagedResourceGroup(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, ResourceGroupDTO resourceGroupDTO);

  Optional<ResourceGroupResponse> get(
      String identifier, String accountIdentifier, String orgIdentifier, String projectIdentifier);

  Page<ResourceGroupResponse> list(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      PageRequest pageRequest, String searchTerm);

  Page<ResourceGroup> list(Criteria criteria, Pageable pageable);

  Optional<ResourceGroupResponse> update(ResourceGroupDTO resourceGroupDTO);

  boolean delete(String identifier, String accountIdentifier, String orgIdentifier, String projectIdentifier,
      boolean forceDeleteRoleAssignments);

  boolean restoreAll(String accountIdentifier, String orgIdentifier, String projectIdentifier);
}
