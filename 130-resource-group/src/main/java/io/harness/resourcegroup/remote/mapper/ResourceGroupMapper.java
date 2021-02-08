package io.harness.resourcegroup.remote.mapper;

import io.harness.resourcegroup.model.ResourceGroup;
import io.harness.resourcegroup.remote.dto.ResourceGroupDTO;
import io.harness.resourcegroup.remote.dto.ResourceGroupResponse;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ResourceGroupMapper {
  public static ResourceGroup fromDTO(ResourceGroupDTO resourceGroupDTO) {
    if (resourceGroupDTO == null) {
      return null;
    }
    return ResourceGroup.builder()
        .accountIdentifier(resourceGroupDTO.getAccountIdentifier())
        .orgIdentifier(resourceGroupDTO.getOrgIdentifier())
        .projectIdentifier(resourceGroupDTO.getProjectIdentifier())
        .identifier(resourceGroupDTO.getIdentifier())
        .name(resourceGroupDTO.getName())
        .system(resourceGroupDTO.getSystem())
        .resourceSelectors(resourceGroupDTO.getResourceSelectors())
        .description(resourceGroupDTO.getDescription())
        .build();
  }

  public static ResourceGroupDTO toDTO(ResourceGroup resourceGroup) {
    if (resourceGroup == null) {
      return null;
    }
    return ResourceGroupDTO.builder()
        .accountIdentifier(resourceGroup.getAccountIdentifier())
        .orgIdentifier(resourceGroup.getOrgIdentifier())
        .projectIdentifier(resourceGroup.getProjectIdentifier())
        .identifier(resourceGroup.getIdentifier())
        .name(resourceGroup.getName())
        .system(resourceGroup.getSystem())
        .resourceSelectors(resourceGroup.getResourceSelectors())
        .description(resourceGroup.getDescription())
        .build();
  }

  public static ResourceGroupResponse toResponseWrapper(ResourceGroup resourceGroup) {
    if (resourceGroup == null) {
      return null;
    }
    return ResourceGroupResponse.builder()
        .createdAt(resourceGroup.getCreatedAt())
        .lastModifiedAt(resourceGroup.getLastModifiedAt())
        .resourceGroupDTO(toDTO(resourceGroup))
        .build();
  }
}
