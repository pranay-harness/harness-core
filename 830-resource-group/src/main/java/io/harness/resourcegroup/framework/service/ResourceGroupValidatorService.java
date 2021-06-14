package io.harness.resourcegroup.framework.service;

import io.harness.resourcegroup.model.ResourceGroup;

public interface ResourceGroupValidatorService {
  void validate(ResourceGroup resourceGroup);

  boolean validateAndFilterInvalidResources(ResourceGroup resourceGroup);
}
