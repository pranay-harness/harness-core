package io.harness.resourcegroup.resource.validator;

import io.harness.resourcegroup.model.ResourceGroup;

public interface ResourceGroupValidatorService {
  boolean isResourceGroupValid(ResourceGroup resourceGroup);
}
