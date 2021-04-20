package io.harness.resourcegroup.framework.service.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.resourcegroup.beans.ValidatorType.STATIC;

import static java.util.stream.Collectors.toMap;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.resourcegroup.framework.service.ResourceGroupValidatorService;
import io.harness.resourcegroup.framework.service.ResourceValidator;
import io.harness.resourcegroup.model.ResourceGroup;
import io.harness.resourcegroup.model.StaticResourceSelector;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@OwnedBy(PL)
public class StaticResourceGroupValidatorServiceImpl implements ResourceGroupValidatorService {
  Map<String, ResourceValidator> resourceValidators;

  @Inject
  public StaticResourceGroupValidatorServiceImpl(
      @Named("resourceValidatorMap") Map<String, ResourceValidator> resourceValidators) {
    this.resourceValidators = resourceValidators;
  }

  @Override
  public boolean isResourceGroupValid(ResourceGroup resourceGroup) {
    Scope scope = Scope.of(
        resourceGroup.getAccountIdentifier(), resourceGroup.getOrgIdentifier(), resourceGroup.getProjectIdentifier());
    Map<String, List<String>> resourceTypeMap =
        resourceGroup.getResourceSelectors()
            .stream()
            .filter(StaticResourceSelector.class ::isInstance)
            .map(StaticResourceSelector.class ::cast)
            .collect(toMap(StaticResourceSelector::getResourceType, StaticResourceSelector::getIdentifiers));

    boolean valid = true;
    Predicate<String> resourceTypeLegalAtScope =
        resourceType -> resourceValidators.get(resourceType).getScopes().contains(scope);
    Predicate<String> resourceTypeSupportStaticValidator =
        resourceType -> resourceValidators.get(resourceType).getValidatorTypes().contains(STATIC);
    BiPredicate<String, List<String>> resourceExists = (resourceType, resourceIds)
        -> resourceValidators.get(resourceType)
               .validate(resourceIds, resourceGroup.getAccountIdentifier(), resourceGroup.getOrgIdentifier(),
                   resourceGroup.getProjectIdentifier())
               .stream()
               .allMatch(Boolean.TRUE::equals);

    for (Map.Entry<String, List<String>> entry : resourceTypeMap.entrySet()) {
      String resourceType = entry.getKey();
      List<String> resourceIds = entry.getValue();
      if (!resourceValidators.containsKey(resourceType)) {
        throw new IllegalStateException(
            String.format("Resource validator for resource type %s not registered", resourceType));
      }
      valid = valid && resourceTypeLegalAtScope.and(resourceTypeSupportStaticValidator).test(resourceType);
      valid = valid && resourceExists.test(resourceType, resourceIds);
    }
    return valid;
  }
}
