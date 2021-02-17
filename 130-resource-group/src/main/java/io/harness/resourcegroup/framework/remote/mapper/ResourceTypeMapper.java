package io.harness.resourcegroup.framework.remote.mapper;

import io.harness.resourcegroup.framework.remote.dto.ResourceTypeDTO;

import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ResourceTypeMapper {
  public static ResourceTypeDTO toDTO(List<String> resourceTypes) {
    if (resourceTypes == null) {
      return null;
    }
    return ResourceTypeDTO.builder().resourceTypes(resourceTypes).build();
  }
}
