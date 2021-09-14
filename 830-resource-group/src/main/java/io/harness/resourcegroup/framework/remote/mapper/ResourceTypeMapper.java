/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.resourcegroup.framework.remote.mapper;

import io.harness.resourcegroup.remote.dto.ResourceTypeDTO;
import io.harness.resourcegroup.remote.dto.ResourceTypeDTO.ResourceType;

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
