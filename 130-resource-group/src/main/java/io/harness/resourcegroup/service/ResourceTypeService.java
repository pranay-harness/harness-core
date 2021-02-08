package io.harness.resourcegroup.service;

import io.harness.resourcegroup.model.ResourceType;
import io.harness.resourcegroup.model.Scope;

import java.util.List;

public interface ResourceTypeService {
  List<ResourceType> getResourceTypes(Scope scope);
}
