package io.harness.resourcegroup.framework.service;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.beans.PageRequest;
import io.harness.resourcegroup.model.ResourceGroup;
import io.harness.resourcegroup.remote.dto.ResourceGroupDTO;
import io.harness.resourcegroupclient.ResourceGroupResponse;

import java.util.Optional;
import org.springframework.data.domain.Page;

@OwnedBy(PL)
public interface ResourceGroupService {
  ResourceGroupResponse create(ResourceGroupDTO resourceGroupDTO);

  ResourceGroupResponse createManagedResourceGroup(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, ResourceGroupDTO resourceGroupDTO);

  Page<ResourceGroupResponse> list(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      PageRequest pageRequest, String searchTerm);

  boolean delete(
      String identifier, String accountIdentifier, String orgIdentifier, String projectIdentifier, boolean safeDelete);

  Optional<ResourceGroupResponse> get(
      String identifier, String accountIdentifier, String orgIdentifier, String projectIdentifier);

  Optional<ResourceGroupResponse> update(ResourceGroupDTO resourceGroupDTO);

  boolean handleResourceDeleteEvent(ResourcePrimaryKey resourcePrimaryKey);

  boolean deleteStaleResources(ResourceGroup resourceGroup);

  boolean createDefaultResourceGroup(ResourcePrimaryKey resourcePrimaryKey);

  boolean restoreResourceGroupsUnderHierarchy(ResourcePrimaryKey resourcePrimaryKey);
}
