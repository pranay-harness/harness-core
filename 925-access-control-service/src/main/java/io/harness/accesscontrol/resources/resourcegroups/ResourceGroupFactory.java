/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.accesscontrol.resources.resourcegroups;

import static io.harness.accesscontrol.scopes.core.Scope.PATH_DELIMITER;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.resourcegroup.model.DynamicResourceSelector;
import io.harness.resourcegroup.model.ResourceSelector;
import io.harness.resourcegroup.model.StaticResourceSelector;
import io.harness.resourcegroup.remote.dto.ResourceGroupDTO;
import io.harness.resourcegroupclient.ResourceGroupResponse;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@OwnedBy(HarnessTeam.PL)
public class ResourceGroupFactory {
  public ResourceGroup buildResourceGroup(ResourceGroupResponse resourceGroupResponse, String scopeIdentifier) {
    ResourceGroupDTO resourceGroupDTO = resourceGroupResponse.getResourceGroup();
    Set<String> resourceSelectors;
    if (resourceGroupDTO.getResourceSelectors() == null) {
      resourceSelectors = new HashSet<>();
    } else {
      resourceSelectors = resourceGroupDTO.getResourceSelectors()
                              .stream()
                              .map(this::buildResourceSelector)
                              .flatMap(Collection::stream)
                              .collect(Collectors.toSet());
    }
    return ResourceGroup.builder()
        .identifier(resourceGroupDTO.getIdentifier())
        .scopeIdentifier(scopeIdentifier)
        .name(resourceGroupDTO.getName())
        .resourceSelectors(resourceSelectors)
        .managed(resourceGroupResponse.isHarnessManaged())
        .fullScopeSelected(resourceGroupDTO.isFullScopeSelected())
        .build();
  }

  public Set<String> buildResourceSelector(ResourceSelector resourceSelector) {
    if (resourceSelector instanceof StaticResourceSelector) {
      StaticResourceSelector staticResourceSelector = (StaticResourceSelector) resourceSelector;
      return staticResourceSelector.getIdentifiers()
          .stream()
          .map(identifier
              -> PATH_DELIMITER.concat(staticResourceSelector.getResourceType())
                     .concat(PATH_DELIMITER)
                     .concat(identifier))
          .collect(Collectors.toSet());
    } else if (resourceSelector instanceof DynamicResourceSelector) {
      DynamicResourceSelector dynamicResourceSelector = (DynamicResourceSelector) resourceSelector;
      return Collections.singleton(PATH_DELIMITER.concat(dynamicResourceSelector.getResourceType())
                                       .concat(PATH_DELIMITER)
                                       .concat(ResourceGroup.ALL_RESOURCES_IDENTIFIER));
    }
    return Collections.emptySet();
  }
}
