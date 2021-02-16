package io.harness.resourcegroup.framework.service.impl;

import io.harness.resourcegroup.framework.remote.dto.ResourceTypeDTO;
import io.harness.resourcegroup.framework.remote.mapper.ResourceTypeMapper;
import io.harness.resourcegroup.framework.service.ResourceTypeService;
import io.harness.resourcegroup.model.Scope;
import io.harness.resourcegroup.resourceclient.api.ResourceValidator;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
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
    return ResourceTypeMapper.toDTO(new ArrayList<>(resourceValidators.keySet()));
  }
}
