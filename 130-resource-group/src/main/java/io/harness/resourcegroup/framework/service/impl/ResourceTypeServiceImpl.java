package io.harness.resourcegroup.framework.service.impl;

import io.harness.resourcegroup.framework.remote.mapper.ResourceTypeMapper;
import io.harness.resourcegroup.framework.service.ResourceTypeService;
import io.harness.resourcegroup.framework.service.ResourceValidator;
import io.harness.resourcegroup.model.Scope;
import io.harness.resourcegroup.remote.dto.ResourceTypeDTO;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ResourceTypeServiceImpl implements ResourceTypeService {
  Map<String, ResourceValidator> resourceValidators;

  @Inject
  public ResourceTypeServiceImpl(@Named("resourceValidatorMap") Map<String, ResourceValidator> resourceValidators) {
    this.resourceValidators = resourceValidators;
  }

  @Override
  public ResourceTypeDTO getResourceTypes(Scope scope) {
    if (Objects.isNull(scope)) {
      return null;
    }
    return ResourceTypeMapper.toDTO(
        resourceValidators.values()
            .stream()
            .filter(resourceValidator -> resourceValidator.getScopes().contains(scope))
            .map(resourceValidator
                -> ResourceTypeDTO.ResourceType.builder()
                       .name(resourceValidator.getResourceType())
                       .validatorTypes(new ArrayList<>(resourceValidator.getValidatorTypes()))
                       .build())
            .collect(Collectors.toList()));
  }
}
