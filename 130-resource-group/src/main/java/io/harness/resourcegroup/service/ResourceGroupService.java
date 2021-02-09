package io.harness.resourcegroup.service;

import io.harness.ng.beans.PageRequest;
import io.harness.resourcegroup.remote.dto.ResourceGroupDTO;
import io.harness.resourcegroup.remote.dto.ResourceGroupResponse;

import java.util.Optional;
import org.springframework.data.domain.Page;

public interface ResourceGroupService {
  ResourceGroupResponse create(ResourceGroupDTO resourceGroupDTO);

  Page<ResourceGroupResponse> list(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      PageRequest pageRequest, String searchTerm);

  Optional<ResourceGroupResponse> delete(
      String identifier, String accountIdentifier, String orgIdentifier, String projectIdentifier);

  Optional<ResourceGroupResponse> find(
      String identifier, String accountIdentifier, String orgIdentifier, String projectIdentifier);

  Optional<ResourceGroupResponse> update(ResourceGroupDTO resourceGroupDTO);
}
