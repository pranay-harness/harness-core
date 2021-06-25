package io.harness.resourcegroup.framework.service.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ScopeLevel;
import io.harness.resourcegroup.framework.remote.mapper.ResourceTypeMapper;
import io.harness.resourcegroup.framework.service.Resource;
import io.harness.resourcegroup.framework.service.ResourceTypeService;
import io.harness.resourcegroup.remote.dto.ResourceTypeDTO;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(PL)
public class ResourceTypeServiceImpl implements ResourceTypeService {
  Map<String, Resource> resourceValidators;

  @Inject
  public ResourceTypeServiceImpl(Map<String, Resource> resourceValidators) {
    this.resourceValidators = resourceValidators;
  }

  @Override
  public ResourceTypeDTO getResourceTypes(ScopeLevel scopeLevel) {
    if (Objects.isNull(scopeLevel)) {
      return null;
    }
    return ResourceTypeMapper.toDTO(
        resourceValidators.values()
            .stream()
            .filter(resourceValidator -> resourceValidator.getValidScopeLevels().contains(scopeLevel))
            .map(resourceValidator
                -> ResourceTypeDTO.ResourceType.builder()
                       .name(resourceValidator.getType())
                       .validatorTypes(new ArrayList<>(resourceValidator.getSelectorKind()))
                       .build())
            .collect(Collectors.toList()));
  }
}
