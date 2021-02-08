package io.harness.resourcegroup.resource.validator.impl;

import static java.util.stream.Collectors.toList;

import io.harness.resourcegroup.model.DynamicResourceSelector;
import io.harness.resourcegroup.model.ResourceGroup;
import io.harness.resourcegroup.model.Scope;
import io.harness.resourcegroup.resource.validator.ResourceGroupValidatorService;

import java.util.List;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class DynamicResourceGroupValidatorServiceImpl implements ResourceGroupValidatorService {
  @Override
  public boolean isResourceGroupValid(ResourceGroup resourceGroup) {
    Scope scope = Scope.ofResourceGroup(resourceGroup);
    List<DynamicResourceSelector> dynamicResourceSelectors = resourceGroup.getResourceSelectors()
                                                                 .stream()
                                                                 .filter(DynamicResourceSelector.class ::isInstance)
                                                                 .map(DynamicResourceSelector.class ::cast)
                                                                 .collect(toList());
    return dynamicResourceSelectors.stream()
        .map(DynamicResourceSelector::getResourceType)
        .allMatch(e -> e.getScopes().contains(scope));
  }
}
