package io.harness.resourcegroup.framework.service.impl;

import static java.util.stream.Collectors.toList;

import io.harness.resourcegroup.framework.service.ResourceGroupValidatorService;
import io.harness.resourcegroup.framework.service.ResourceValidator;
import io.harness.resourcegroup.model.DynamicResourceSelector;
import io.harness.resourcegroup.model.ResourceGroup;
import io.harness.resourcegroup.model.Scope;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DynamicResourceGroupValidatorServiceImpl implements ResourceGroupValidatorService {
  Map<String, ResourceValidator> resourceValidators;

  @Inject
  public DynamicResourceGroupValidatorServiceImpl(
      @Named("resourceValidatorMap") Map<String, ResourceValidator> resourceValidators) {
    this.resourceValidators = resourceValidators;
  }

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
        .allMatch(e -> resourceValidators.get(e).getScopes().contains(scope));
  }
}
