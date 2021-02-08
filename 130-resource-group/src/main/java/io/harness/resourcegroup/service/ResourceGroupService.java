package io.harness.resourcegroup.service;

import io.harness.resourcegroup.remote.dto.ResourceGroupDTO;
import io.harness.resourcegroup.remote.dto.ResourceGroupResponse;

import java.util.Optional;

public interface ResourceGroupService {
  ResourceGroupResponse create(ResourceGroupDTO resourceGroupDTO);

  Optional<ResourceGroupResponse> delete(
      String identifier, String accountIdentifier, String orgIdentifier, String projectIdentifier);

  Optional<ResourceGroupResponse> find(
      String identifier, String accountIdentifier, String orgIdentifier, String projectIdentifier);

  Optional<ResourceGroupResponse> update(ResourceGroupDTO resourceGroupDTO);
}
