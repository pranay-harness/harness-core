package io.harness.resourcegroup.framework.service.impl;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import io.harness.resourcegroup.framework.beans.ResourceGroupConstants;
import io.harness.resourcegroup.framework.service.ResourceGroupValidatorService;
import io.harness.resourcegroup.framework.service.ResourceValidator;
import io.harness.resourcegroup.model.ResourceGroup;
import io.harness.resourcegroup.model.Scope;
import io.harness.resourcegroup.model.StaticResourceSelector;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StaticResourceGroupValidatorServiceImpl implements ResourceGroupValidatorService {
  Map<String, ResourceValidator> resourceValidators;

  @Inject
  public StaticResourceGroupValidatorServiceImpl(
      @Named("resourceValidatorMap") Map<String, ResourceValidator> resourceValidators) {
    this.resourceValidators = resourceValidators;
  }

  @Override
  public boolean isResourceGroupValid(ResourceGroup resourceGroup) {
    Scope scope = Scope.ofResourceGroup(resourceGroup);
    Map<String, List<String>> resouceTypeMap = resourceGroup.getResourceSelectors()
                                                   .stream()
                                                   .filter(StaticResourceSelector.class ::isInstance)
                                                   .map(StaticResourceSelector.class ::cast)
                                                   .collect(groupingBy(StaticResourceSelector::getResourceType,
                                                       mapping(StaticResourceSelector::getIdentifier, toList())));
    boolean valid = true;
    for (Map.Entry<String, List<String>> entry : resouceTypeMap.entrySet()) {
      String resourceType = entry.getKey();
      List<String> resourceIds = entry.getValue();
      if (!resourceValidators.containsKey(resourceType)) {
        throw new IllegalStateException(
            String.format("Resource validator for resource type %s not registered", resourceType));
      }
      valid = valid && areResourcesLegalAtScope(resourceType, resourceIds, scope, resourceGroup);
      valid = valid
          && resourceValidators.get(resourceType)
                 .validate(resourceIds, resourceGroup.getAccountIdentifier(), resourceGroup.getOrgIdentifier(),
                     resourceGroup.getProjectIdentifier())
                 .stream()
                 .allMatch(Boolean.TRUE::equals);
    }
    return valid;
  }

  private boolean areResourcesLegalAtScope(
      String resourceType, List<String> resourceIds, Scope scope, ResourceGroup resourcegroup) {
    boolean valid = resourceValidators.get(resourceType).getScopes().contains(scope);
    if (resourceType.equals(ResourceGroupConstants.ACCOUNT)) {
      valid = valid && resourceIds.stream().allMatch(resourcegroup.getAccountIdentifier()::equals);
    }
    if (resourceType.equals(ResourceGroupConstants.ORGANIZATION)) {
      valid = valid && resourceIds.stream().allMatch(resourcegroup.getOrgIdentifier()::equals);
    }
    if (resourceType.equals(ResourceGroupConstants.PROJECT)) {
      valid = valid && resourceIds.stream().allMatch(resourcegroup.getProjectIdentifier()::equals);
    }
    return valid;
  }
}
