package io.harness.resourcegroup.service.impl;

import io.harness.resourcegroup.model.ResourceType;
import io.harness.resourcegroup.model.Scope;
import io.harness.resourcegroup.service.ResourceTypeService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ResourceTypeServiceImpl implements ResourceTypeService {
  @Override
  public List<ResourceType> getResourceTypes(Scope scope) {
    if (Objects.isNull(scope)) {
      return Collections.emptyList();
    }
    return Arrays.stream(ResourceType.values()).filter(e -> e.getScopes().contains(scope)).collect(Collectors.toList());
  }
}
