package io.harness.resourcegroup.framework.remote.mapper;

import static io.harness.ng.core.mapper.TagMapper.convertToList;
import static io.harness.ng.core.mapper.TagMapper.convertToMap;

import io.harness.resourcegroup.model.ResourceGroup;
import io.harness.resourcegroup.remote.dto.ResourceGroupDTO;
import io.harness.resourcegroupclient.ResourceGroupResponse;

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
        .color(resourceGroupDTO.getColor())
        .tags(convertToList(resourceGroupDTO.getTags()))
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
        .color(resourceGroup.getColor())
        .tags(convertToMap(resourceGroup.getTags()))
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
        .resourceGroup(toDTO(resourceGroup))
        .harnessManaged(Boolean.TRUE.equals(resourceGroup.getHarnessManaged()))
        .build();
  }
}
