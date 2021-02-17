package io.harness.resourcegroup.framework.service;

import io.harness.resourcegroup.framework.remote.dto.ResourceTypeDTO;
import io.harness.resourcegroup.model.Scope;

public interface ResourceTypeService {
  ResourceTypeDTO getResourceTypes(Scope scope);
}
