package io.harness.accesscontrol.resources;

import static io.harness.accesscontrol.scopes.core.Scope.PATH_DELIMITER;

import io.harness.accesscontrol.resources.resourcegroups.ResourceGroup;
import io.harness.resourcegroup.model.DynamicResourceSelector;
import io.harness.resourcegroup.model.ResourceSelector;
import io.harness.resourcegroup.model.StaticResourceSelector;
import io.harness.resourcegroup.remote.dto.ResourceGroupDTO;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ResourceGroupFactory {
  public ResourceGroup buildResourceGroup(ResourceGroupDTO object, String scopeIdentifier) {
    Set<String> resourceSelectors;
    if (object.getResourceSelectors() == null) {
      resourceSelectors = new HashSet<>();
    } else {
      resourceSelectors = object.getResourceSelectors()
                              .stream()
                              .map(this::buildResourceSelector)
                              .filter(Optional::isPresent)
                              .map(Optional::get)
                              .collect(Collectors.toSet());
    }
    return ResourceGroup.builder()
        .identifier(object.getIdentifier())
        .scopeIdentifier(scopeIdentifier)
        .name(object.getName())
        .resourceSelectors(resourceSelectors)
        .managed(object.getHarnessManaged() == null ? Boolean.FALSE : object.getHarnessManaged())
        .build();
  }

  public Optional<String> buildResourceSelector(ResourceSelector resourceSelector) {
    if (resourceSelector instanceof StaticResourceSelector) {
      StaticResourceSelector staticResourceSelector = (StaticResourceSelector) resourceSelector;
      return Optional.of(PATH_DELIMITER.concat(staticResourceSelector.getResourceType())
                             .concat(PATH_DELIMITER)
                             .concat(staticResourceSelector.getIdentifier()));
    } else if (resourceSelector instanceof DynamicResourceSelector) {
      DynamicResourceSelector dynamicResourceSelector = (DynamicResourceSelector) resourceSelector;
      return Optional.of(PATH_DELIMITER.concat(dynamicResourceSelector.getResourceType())
                             .concat(PATH_DELIMITER)
                             .concat(ResourceGroup.ALL_RESOURCES_IDENTIFIER));
    }
    return Optional.empty();
  }
}
