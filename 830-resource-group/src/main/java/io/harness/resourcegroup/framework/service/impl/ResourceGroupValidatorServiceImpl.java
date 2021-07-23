package io.harness.resourcegroup.framework.service.impl;

import static io.harness.resourcegroup.beans.ValidatorType.DYNAMIC;
import static io.harness.resourcegroup.beans.ValidatorType.STATIC;

import io.harness.beans.Scope;
import io.harness.beans.ScopeLevel;
import io.harness.resourcegroup.framework.service.Resource;
import io.harness.resourcegroup.framework.service.ResourceGroupValidatorService;
import io.harness.resourcegroup.model.DynamicResourceSelector;
import io.harness.resourcegroup.model.ResourceGroup;
import io.harness.resourcegroup.model.ResourceSelector;
import io.harness.resourcegroup.model.StaticResourceSelector;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Iterator;
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
  public boolean sanitizeResourceSelectors(ResourceGroup resourceGroup) {
    Scope scope = Scope.builder()
                      .accountIdentifier(resourceGroup.getAccountIdentifier())
                      .orgIdentifier(resourceGroup.getOrgIdentifier())
                      .projectIdentifier(resourceGroup.getProjectIdentifier())
                      .build();

    boolean updated = false;
    List<ResourceSelector> newResourceSelectors = new ArrayList<>(resourceGroup.getResourceSelectors());
    for (Iterator<ResourceSelector> iterator = newResourceSelectors.iterator(); iterator.hasNext();) {
      ResourceSelector resourceSelector = iterator.next();
      if (resourceSelector instanceof StaticResourceSelector) {
        StaticResourceSelector staticResourceSelector = (StaticResourceSelector) resourceSelector;
        updated = updated || sanitizeStaticResourceSelector(scope, staticResourceSelector);
        if (updated && staticResourceSelector.getIdentifiers().isEmpty()) {
          iterator.remove();
        }
      } else if (resourceSelector instanceof DynamicResourceSelector) {
        DynamicResourceSelector dynamicResourceSelector = (DynamicResourceSelector) resourceSelector;
        if (!isValidDynamicResourceSelector(scope, dynamicResourceSelector)) {
          iterator.remove();
          updated = true;
        }
      }
    }
    resourceGroup.setResourceSelectors(newResourceSelectors);
    return updated;
  }

  private boolean isValidDynamicResourceSelector(Scope scope, DynamicResourceSelector resourceSelector) {
    String resourceType = resourceSelector.getResourceType();
    ScopeLevel scopeLevel =
        ScopeLevel.of(scope.getAccountIdentifier(), scope.getOrgIdentifier(), scope.getProjectIdentifier());

    return resourceMap.containsKey(resourceType)
        && resourceMap.get(resourceType).getValidScopeLevels().contains(scopeLevel)
        && resourceMap.get(resourceType).getSelectorKind().contains(DYNAMIC);
  }

  private boolean sanitizeStaticResourceSelector(Scope scope, StaticResourceSelector resourceSelector) {
    String resourceType = resourceSelector.getResourceType();
    ScopeLevel scopeLevel =
        ScopeLevel.of(scope.getAccountIdentifier(), scope.getOrgIdentifier(), scope.getProjectIdentifier());

    Resource resource = resourceMap.get(resourceType);
    if (resource == null || !resource.getValidScopeLevels().contains(scopeLevel)
        || !resource.getSelectorKind().contains(STATIC)) {
      resourceSelector.getIdentifiers().clear();
      return true;
    }

    List<String> resourceIds = resourceSelector.getIdentifiers();
    List<Boolean> validationResult = resource.validate(resourceIds, scope);
    List<String> validResourceIds = IntStream.range(0, resourceIds.size())
                                        .filter(validationResult::get)
                                        .mapToObj(resourceIds::get)
                                        .collect(Collectors.toList());
    resourceSelector.setIdentifiers(validResourceIds);

    return resourceIds.isEmpty() || (resourceIds.size() != validResourceIds.size());
  }
}
