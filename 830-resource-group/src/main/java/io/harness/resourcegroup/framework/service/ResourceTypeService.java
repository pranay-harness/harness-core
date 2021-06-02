package io.harness.resourcegroup.framework.service;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ScopeLevel;
import io.harness.resourcegroup.remote.dto.ResourceTypeDTO;

@OwnedBy(PL)
public interface ResourceTypeService {
  ResourceTypeDTO getResourceTypes(ScopeLevel scopeLevel);
}
