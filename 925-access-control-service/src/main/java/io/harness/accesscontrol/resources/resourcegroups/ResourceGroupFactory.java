/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.resources.resourcegroups;

import static io.harness.accesscontrol.scopes.core.Scope.PATH_DELIMITER;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.harness.HarnessScopeParams;
import io.harness.accesscontrol.scopes.harness.ScopeMapper;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.resourcegroup.model.DynamicResourceSelector;
import io.harness.resourcegroup.model.ResourceSelector;
import io.harness.resourcegroup.model.ResourceSelectorByScope;
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
        .allowedScopeLevels(resourceGroupDTO.getAllowedScopeLevels())
        .build();
  }

  public ResourceGroup buildResourceGroup(ResourceGroupResponse resourceGroupResponse) {
    ResourceGroupDTO resourceGroupDTO = resourceGroupResponse.getResourceGroup();
    Scope scope = null;
    if (isNotEmpty(resourceGroupDTO.getAccountIdentifier())) {
      scope = ScopeMapper.fromParams(HarnessScopeParams.builder()
                                         .accountIdentifier(resourceGroupDTO.getAccountIdentifier())
                                         .orgIdentifier(resourceGroupDTO.getOrgIdentifier())
                                         .projectIdentifier(resourceGroupDTO.getProjectIdentifier())
                                         .build());
    }
    return buildResourceGroup(resourceGroupResponse, scope == null ? null : scope.toString());
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
      if (Boolean.TRUE.equals(dynamicResourceSelector.getIncludeChildScopes())) {
        return Collections.singleton(PATH_DELIMITER.concat(ResourceGroup.INCLUDE_CHILD_SCOPES_IDENTIFIER)
                                         .concat(PATH_DELIMITER)
                                         .concat(dynamicResourceSelector.getResourceType())
                                         .concat(PATH_DELIMITER)
                                         .concat(ResourceGroup.ALL_RESOURCES_IDENTIFIER));
      }
      return Collections.singleton(PATH_DELIMITER.concat(dynamicResourceSelector.getResourceType())
                                       .concat(PATH_DELIMITER)
                                       .concat(ResourceGroup.ALL_RESOURCES_IDENTIFIER));
    } else if (resourceSelector instanceof ResourceSelectorByScope) {
      ResourceSelectorByScope resourceSelectorByScope = (ResourceSelectorByScope) resourceSelector;
      if (resourceSelectorByScope.isIncludeChildScopes()) {
        return Collections.singleton(PATH_DELIMITER.concat(ResourceGroup.INCLUDE_CHILD_SCOPES_IDENTIFIER)
                                         .concat(PATH_DELIMITER)
                                         .concat(ResourceGroup.ALL_RESOURCES_IDENTIFIER)
                                         .concat(PATH_DELIMITER)
                                         .concat(ResourceGroup.ALL_RESOURCES_IDENTIFIER));
      }
      return Collections.singleton(PATH_DELIMITER.concat(ResourceGroup.ALL_RESOURCES_IDENTIFIER)
                                       .concat(PATH_DELIMITER)
                                       .concat(ResourceGroup.ALL_RESOURCES_IDENTIFIER));
    }
    return Collections.emptySet();
  }
}
