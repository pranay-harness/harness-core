package io.harness.resourcegroup.framework.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.resourcegroup.beans.ValidatorType.DYNAMIC;
import static io.harness.resourcegroup.beans.ValidatorType.STATIC;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import io.harness.beans.Scope;
import io.harness.beans.ScopeLevel;
import io.harness.exception.InvalidRequestException;
import io.harness.resourcegroup.framework.service.Resource;
import io.harness.resourcegroup.framework.service.ResourceGroupValidatorService;
import io.harness.resourcegroup.model.DynamicResourceSelector;
import io.harness.resourcegroup.model.ResourceGroup;
import io.harness.resourcegroup.model.ResourceSelector;
import io.harness.resourcegroup.model.StaticResourceSelector;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ResourceGroupValidatorServiceImpl implements ResourceGroupValidatorService {
  Map<String, Resource> resourceMap;

  @Override
  public void validate(ResourceGroup resourceGroup) {
    if (resourceGroup.getIdentifier().charAt(0) == '_' && !TRUE.equals(resourceGroup.getHarnessManaged())) {
      throw new InvalidRequestException(
          "Identifiers starting with _ are only allowed for Harness managed resource group.");
    }
    if (TRUE.equals(resourceGroup.getFullScopeSelected()) && isNotEmpty(resourceGroup.getResourceSelectors())) {
      throw new InvalidRequestException(
          "Can't have both fullScopeSelected and resource selectors in a resource group.");
    }
    Scope scope = Scope.builder()
                      .accountIdentifier(resourceGroup.getAccountIdentifier())
                      .orgIdentifier(resourceGroup.getOrgIdentifier())
                      .projectIdentifier(resourceGroup.getProjectIdentifier())
                      .build();
    validateDynamicResourceSelector(resourceGroup.getResourceSelectors(), scope);
    validateStaticResourceSelector(resourceGroup.getResourceSelectors(), scope);
  }

  @Override
  public boolean validateAndFilterInvalidOnes(ResourceGroup resourceGroup) {
    boolean valid = true;
    Scope scope = Scope.builder()
                      .accountIdentifier(resourceGroup.getAccountIdentifier())
                      .orgIdentifier(resourceGroup.getOrgIdentifier())
                      .projectIdentifier(resourceGroup.getProjectIdentifier())
                      .build();
    List<ResourceSelector> filteredResourceSelector = new ArrayList<>(resourceGroup.getResourceSelectors().size());
    for (ResourceSelector resourceSelector : resourceGroup.getResourceSelectors()) {
      if (resourceSelector instanceof StaticResourceSelector) {
        StaticResourceSelector staticResourceSelector = (StaticResourceSelector) resourceSelector;
        List<Boolean> results = resourceMap.get(staticResourceSelector.getResourceType())
                                    .validate(staticResourceSelector.getIdentifiers(), scope);
        boolean atleastOneDeleted = results.stream().anyMatch(FALSE::equals);
        valid = valid && !atleastOneDeleted;
        if (atleastOneDeleted) {
          staticResourceSelector.setIdentifiers(IntStream.range(0, staticResourceSelector.getIdentifiers().size())
                                                    .filter(results::get)
                                                    .mapToObj(staticResourceSelector.getIdentifiers()::get)
                                                    .collect(Collectors.toList()));
        }
        if (!staticResourceSelector.getIdentifiers().isEmpty()) {
          filteredResourceSelector.add(staticResourceSelector);
        }
      } else {
        filteredResourceSelector.add(resourceSelector);
      }
      resourceGroup.setResourceSelectors(filteredResourceSelector);
    }
    return valid;
  }

  private void validateDynamicResourceSelector(List<ResourceSelector> resourceSelectors, Scope scope) {
    List<DynamicResourceSelector> dynamicResourceSelectors =
        filterResourceSelectors(resourceSelectors, DynamicResourceSelector.class);
    ScopeLevel scopeLevel =
        ScopeLevel.of(scope.getAccountIdentifier(), scope.getOrgIdentifier(), scope.getProjectIdentifier());
    List<ResourceSelector> invalidResourceSelectors = new ArrayList<>();
    for (DynamicResourceSelector selector : dynamicResourceSelectors) {
      String resourceType = selector.getResourceType();
      if (!resourceMap.containsKey(resourceType)
          || !resourceMap.get(resourceType).getValidScopeLevels().contains(scopeLevel)
          || !resourceMap.get(resourceType).getSelectorKind().contains(DYNAMIC)) {
        invalidResourceSelectors.add(selector);
      }
    }
    if (!invalidResourceSelectors.isEmpty()) {
      throw new InvalidRequestException(
          String.format("Resource Selectors : [%s] are invalid", invalidResourceSelectors));
    }
  }

  private void validateStaticResourceSelector(List<ResourceSelector> resourceSelectors, Scope scope) {
    List<StaticResourceSelector> staticResourceSelectors =
        filterResourceSelectors(resourceSelectors, StaticResourceSelector.class);
    ScopeLevel scopeLevel =
        ScopeLevel.of(scope.getAccountIdentifier(), scope.getOrgIdentifier(), scope.getProjectIdentifier());
    List<ResourceSelector> invalidResourceSelectors = new ArrayList<>();
    for (StaticResourceSelector resourceSelector : staticResourceSelectors) {
      String resourceType = resourceSelector.getResourceType();
      List<String> resourceIds = resourceSelector.getIdentifiers();
      if (!resourceMap.containsKey(resourceType)) {
        invalidResourceSelectors.add(resourceSelector);
        continue;
      }
      Resource resource = resourceMap.get(resourceType);
      if (!resource.getValidScopeLevels().contains(scopeLevel) || !resource.getSelectorKind().contains(STATIC)) {
        invalidResourceSelectors.add(resourceSelector);
        continue;
      }
      List<Boolean> areValid = resource.validate(resourceIds, scope);
      if (areValid.stream().anyMatch(FALSE::equals)) {
        invalidResourceSelectors.add(resourceSelector);
      }
    }
    if (!invalidResourceSelectors.isEmpty()) {
      throw new InvalidRequestException(
          String.format("Resource Selectors : [%s] are invalid", invalidResourceSelectors));
    }
  }

  private <T> List<T> filterResourceSelectors(List<ResourceSelector> resourceSelectors, Class<T> clazz) {
    return resourceSelectors.stream().filter(clazz::isInstance).map(clazz::cast).collect(Collectors.toList());
  }
}
