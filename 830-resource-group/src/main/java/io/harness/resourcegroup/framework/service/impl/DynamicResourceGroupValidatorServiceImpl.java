package io.harness.resourcegroup.framework.service.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.resourcegroup.beans.ValidatorType.DYNAMIC;

import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.resourcegroup.framework.service.ResourceGroupValidatorService;
import io.harness.resourcegroup.framework.service.ResourceValidator;
import io.harness.resourcegroup.model.DynamicResourceSelector;
import io.harness.resourcegroup.model.ResourceGroup;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@OwnedBy(PL)
public class DynamicResourceGroupValidatorServiceImpl implements ResourceGroupValidatorService {
  Map<String, ResourceValidator> resourceValidators;

  @Inject
  public DynamicResourceGroupValidatorServiceImpl(
      @Named("resourceValidatorMap") Map<String, ResourceValidator> resourceValidators) {
    this.resourceValidators = resourceValidators;
  }

  @Override
  public boolean isResourceGroupValid(ResourceGroup resourceGroup) {
    Scope scope = Scope.of(
        resourceGroup.getAccountIdentifier(), resourceGroup.getOrgIdentifier(), resourceGroup.getProjectIdentifier());
    List<DynamicResourceSelector> dynamicResourceSelectors = resourceGroup.getResourceSelectors()
                                                                 .stream()
                                                                 .filter(DynamicResourceSelector.class ::isInstance)
                                                                 .map(DynamicResourceSelector.class ::cast)
                                                                 .collect(toList());
    Predicate<String> resourceTypeLegalAtScope = e -> resourceValidators.get(e).getScopes().contains(scope);
    Predicate<String> resourceTypeSupportDynamicValidator =
        e -> resourceValidators.get(e).getValidatorTypes().contains(DYNAMIC);
    return dynamicResourceSelectors.stream()
        .map(DynamicResourceSelector::getResourceType)
        .allMatch(resourceTypeLegalAtScope.and(resourceTypeSupportDynamicValidator));
  }
}
