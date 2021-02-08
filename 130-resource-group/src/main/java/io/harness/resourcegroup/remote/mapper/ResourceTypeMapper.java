package io.harness.resourcegroup.remote.mapper;

import io.harness.resourcegroup.model.ResourceType;
import io.harness.resourcegroup.remote.dto.ResourceTypeDTO;

import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ResourceTypeMapper {
  public static ResourceTypeDTO toDTO(List<ResourceType> resourceTypes) {
    if (resourceTypes == null) {
      return null;
    }
    return ResourceTypeDTO.builder().resourceTypes(resourceTypes).build();
  }
}
