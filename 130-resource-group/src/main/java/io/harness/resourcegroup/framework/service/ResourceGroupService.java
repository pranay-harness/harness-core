package io.harness.resourcegroup.framework.service;

import io.harness.ng.beans.PageRequest;
import io.harness.resourcegroup.model.ResourceGroup;
import io.harness.resourcegroup.remote.dto.ResourceGroupDTO;
import io.harness.resourcegroupclient.ResourceGroupResponse;

import java.util.Optional;
import org.springframework.data.domain.Page;

public interface ResourceGroupService {
  ResourceGroupResponse create(ResourceGroupDTO resourceGroupDTO);

  void ensureDefaultResourceGroup(String accountIdentifier, String orgIdentifier, String projectIdentifier);

  Page<ResourceGroupResponse> list(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      PageRequest pageRequest, String searchTerm);

  boolean delete(String identifier, String accountIdentifier, String orgIdentifier, String projectIdentifier);

  Optional<ResourceGroupResponse> find(
      String identifier, String accountIdentifier, String orgIdentifier, String projectIdentifier);

  Optional<ResourceGroupResponse> update(ResourceGroupDTO resourceGroupDTO);

  boolean handleResourceDeleteEvent(ResourcePrimaryKey resourcePrimaryKey);

  boolean deleteStaleResources(ResourceGroup resourceGroup);

  boolean createDefaultResourceGroup(ResourcePrimaryKey resourcePrimaryKey);

  boolean restoreResourceGroupsUnderHierarchy(ResourcePrimaryKey resourcePrimaryKey);
}
