package io.harness.resourcegroup.framework.service.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ScopeLevel;
import io.harness.resourcegroup.framework.remote.mapper.ResourceTypeMapper;
import io.harness.resourcegroup.framework.service.Resource;
import io.harness.resourcegroup.framework.service.ResourceTypeService;
import io.harness.resourcegroup.remote.dto.ResourceTypeDTO;
import io.harness.resourcegroup.remote.dto.ResourceTypeDTO.ResourceType;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(PL)
public class ResourceTypeServiceImpl implements ResourceTypeService {
  Map<String, Resource> resources;

  @Inject
  public ResourceTypeServiceImpl(Map<String, Resource> resources) {
    this.resources = resources;
  }

  private static ResourceType toResourceType(Resource resource, ScopeLevel scopeLevel) {
    return ResourceType.builder()
        .name(resource.getType())
        .validatorTypes(resource.getSelectorKind().get(scopeLevel) == null
                ? new ArrayList<>()
                : new ArrayList<>(resource.getSelectorKind().get(scopeLevel)))
        .build();
  }

  @Override
  public ResourceTypeDTO getResourceTypes(ScopeLevel scopeLevel) {
    if (Objects.isNull(scopeLevel)) {
      return null;
    }

    return ResourceTypeMapper.toDTO(resources.values()
                                        .stream()
                                        .filter(resource -> resource.getValidScopeLevels().contains(scopeLevel))
                                        .map(resource -> toResourceType(resource, scopeLevel))
                                        .collect(Collectors.toList()));
  }
}
