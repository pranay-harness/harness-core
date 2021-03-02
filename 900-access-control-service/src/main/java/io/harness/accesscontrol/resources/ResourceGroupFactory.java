package io.harness.accesscontrol.resources;

import static io.harness.accesscontrol.scopes.core.Scope.PATH_DELIMITER;

import io.harness.accesscontrol.resources.resourcegroups.ResourceGroup;
import io.harness.resourcegroup.model.DynamicResourceSelector;
import io.harness.resourcegroup.model.ResourceSelector;
import io.harness.resourcegroup.model.StaticResourceSelector;
import io.harness.resourcegroup.remote.dto.ResourceGroupDTO;
import io.harness.resourcegroupclient.ResourceGroupResponse;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ResourceGroupFactory {
  public ResourceGroup buildResourceGroup(ResourceGroupResponse resourceGroupResponse, String scopeIdentifier) {
    ResourceGroupDTO resourceGroupDto = resourceGroupResponse.getResourceGroup();
    Set<String> resourceSelectors;
    if (resourceGroupDto.getResourceSelectors() == null) {
      resourceSelectors = new HashSet<>();
    } else {
      resourceSelectors = resourceGroupDto.getResourceSelectors()
                              .stream()
                              .map(this::buildResourceSelector)
                              .flatMap(Collection::stream)
                              .filter(Optional::isPresent)
                              .map(Optional::get)
                              .collect(Collectors.toSet());
    }
    return ResourceGroup.builder()
        .identifier(resourceGroupDto.getIdentifier())
        .scopeIdentifier(scopeIdentifier)
        .name(resourceGroupDto.getName())
        .resourceSelectors(resourceSelectors)
        .managed(resourceGroupResponse.isHarnessManaged())
        .build();
  }

  public List<Optional<String>> buildResourceSelector(ResourceSelector resourceSelector) {
    if (resourceSelector instanceof StaticResourceSelector) {
      StaticResourceSelector staticResourceSelector = (StaticResourceSelector) resourceSelector;
      return staticResourceSelector.getIdentifiers()
          .stream()
          .map(identifier
              -> Optional.of(PATH_DELIMITER.concat(staticResourceSelector.getResourceType())
                                 .concat(PATH_DELIMITER)
                                 .concat(identifier)))
          .collect(Collectors.toList());
    } else if (resourceSelector instanceof DynamicResourceSelector) {
      DynamicResourceSelector dynamicResourceSelector = (DynamicResourceSelector) resourceSelector;
      return Collections.singletonList(Optional.of(PATH_DELIMITER.concat(dynamicResourceSelector.getResourceType())
                                                       .concat(PATH_DELIMITER)
                                                       .concat(ResourceGroup.ALL_RESOURCES_IDENTIFIER)));
    }
    return Collections.singletonList(Optional.empty());
  }
}
