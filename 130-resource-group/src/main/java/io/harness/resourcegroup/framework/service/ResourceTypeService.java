package io.harness.resourcegroup.framework.service;

import io.harness.resourcegroup.model.Scope;
import io.harness.resourcegroup.remote.dto.ResourceTypeDTO;

public interface ResourceTypeService {
  ResourceTypeDTO getResourceTypes(Scope scope);
}
