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
  public boolean validateAndFilterInvalidResourceSelectors(ResourceGroup resourceGroup) {
    Scope scope = Scope.builder()
                      .accountIdentifier(resourceGroup.getAccountIdentifier())
                      .orgIdentifier(resourceGroup.getOrgIdentifier())
                      .projectIdentifier(resourceGroup.getProjectIdentifier())
                      .build();
    boolean valid = false;
    for (ResourceSelector resourceSelector : resourceGroup.getResourceSelectors()) {
      if (resourceSelector instanceof StaticResourceSelector) {
        valid |= validateAndSanitizeStaticResourceSelector(scope, (StaticResourceSelector) resourceSelector);
      } else if (resourceSelector instanceof DynamicResourceSelector) {
        valid |= validateAndSanitizeDynamicResourceSelector(scope, (DynamicResourceSelector) resourceSelector);
      }
    }
    return valid;
  }

  private boolean validateAndSanitizeDynamicResourceSelector(Scope scope, DynamicResourceSelector resourceSelector) {
    String resourceType = resourceSelector.getResourceType();
    ScopeLevel scopeLevel =
        ScopeLevel.of(scope.getAccountIdentifier(), scope.getOrgIdentifier(), scope.getProjectIdentifier());

    return resourceMap.containsKey(resourceType)
        && resourceMap.get(resourceType).getValidScopeLevels().contains(scopeLevel)
        && resourceMap.get(resourceType).getSelectorKind().contains(DYNAMIC);
  }

  private boolean validateAndSanitizeStaticResourceSelector(Scope scope, StaticResourceSelector resourceSelector) {
    String resourceType = resourceSelector.getResourceType();
    List<String> resourceIds = resourceSelector.getIdentifiers();
    ScopeLevel scopeLevel =
        ScopeLevel.of(scope.getAccountIdentifier(), scope.getOrgIdentifier(), scope.getProjectIdentifier());

    if (!resourceMap.containsKey(resourceType)) {
      return false;
    }

    Resource resource = resourceMap.get(resourceType);
    if (!resource.getValidScopeLevels().contains(scopeLevel) || !resource.getSelectorKind().contains(STATIC)) {
      return false;
    }

    List<Boolean> validationResult = resource.validate(resourceIds, scope);
    List<String> validResourceIds = IntStream.range(0, resourceIds.size())
                                        .filter(validationResult::get)
                                        .mapToObj(resourceIds::get)
                                        .collect(Collectors.toList());
    if (validResourceIds.isEmpty()) {
      return false;
    } else {
      resourceSelector.setIdentifiers(validResourceIds);
      return true;
    }
  }
}
