package io.harness.resourcegroup.resource.validator.impl;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import io.harness.resourcegroup.model.ResourceGroup;
import io.harness.resourcegroup.model.ResourceType;
import io.harness.resourcegroup.model.Scope;
import io.harness.resourcegroup.model.StaticResourceSelector;
import io.harness.resourcegroup.resource.validator.ResourceGroupValidatorService;
import io.harness.resourcegroup.resource.validator.ResourceValidator;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class StaticResourceGroupValidatorServiceImpl implements ResourceGroupValidatorService {
  Map<ResourceType, ResourceValidator> resourceValidators;

  @Inject
  public StaticResourceGroupValidatorServiceImpl(
      @Named("resourceValidatorMap") Map<ResourceType, ResourceValidator> resourceValidators) {
    this.resourceValidators = resourceValidators;
  }

  @Override
  public boolean isResourceGroupValid(ResourceGroup resourceGroup) {
    Scope scope = Scope.ofResourceGroup(resourceGroup);
    Map<ResourceType, List<String>> resouceTypeMap = resourceGroup.getResourceSelectors()
                                                         .stream()
                                                         .filter(StaticResourceSelector.class ::isInstance)
                                                         .map(StaticResourceSelector.class ::cast)
                                                         .collect(groupingBy(StaticResourceSelector::getResourceType,
                                                             mapping(StaticResourceSelector::getIdentifier, toList())));
    boolean valid = true;
    for (Map.Entry<ResourceType, List<String>> entry : resouceTypeMap.entrySet()) {
      ResourceType resourceType = entry.getKey();
      List<String> resourceIds = entry.getValue();
      if (!resourceType.getScopes().contains(scope)) {
        return false;
      }
      if (!resourceValidators.containsKey(resourceType)) {
        throw new IllegalStateException(
            String.format("Resource validator for resource type %s not registered", resourceType.name()));
      }
      valid = valid
          && resourceValidators.get(resourceType)
                 .validate(resourceIds, resourceGroup.getAccountIdentifier(), resourceGroup.getOrgIdentifier(),
                     resourceGroup.getProjectIdentifier());
    }
    return valid;
  }
}
