package io.harness.accesscontrol.resources.resourcegroups.api;

import io.harness.accesscontrol.resourcegroups.api.ResourceGroupDTO;
import io.harness.accesscontrol.resources.resourcegroups.ResourceGroup;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ResourceGroupDTOMapper {
  public static ResourceGroupDTO toDTO(ResourceGroup resourceGroup) {
    return ResourceGroupDTO.builder().identifier(resourceGroup.getIdentifier()).name(resourceGroup.getName()).build();
  }
}
