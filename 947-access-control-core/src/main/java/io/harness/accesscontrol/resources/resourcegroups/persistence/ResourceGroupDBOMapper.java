package io.harness.accesscontrol.resources.resourcegroups.persistence;

import io.harness.accesscontrol.resources.resourcegroups.ResourceGroup;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.PL)
@UtilityClass
public class ResourceGroupDBOMapper {
  public static ResourceGroupDBO toDBO(ResourceGroup object) {
    return ResourceGroupDBO.builder()
        .identifier(object.getIdentifier())
        .scopeIdentifier(object.getScopeIdentifier())
        .name(object.getName())
        .allowedScopeLevels(object.getAllowedScopeLevels())
        .resourceSelectors(object.getResourceSelectors())
        .fullScopeSelected(object.isFullScopeSelected())
        .managed(object.isManaged())
        .createdAt(object.getCreatedAt())
        .lastModifiedAt(object.getLastModifiedAt())
        .version(object.getVersion())
        .build();
  }

  public static ResourceGroup fromDBO(ResourceGroupDBO object) {
    return ResourceGroup.builder()
        .identifier(object.getIdentifier())
        .scopeIdentifier(object.getScopeIdentifier())
        .name(object.getName())
        .allowedScopeLevels(object.getAllowedScopeLevels())
        .resourceSelectors(object.getResourceSelectors())
        .fullScopeSelected(Boolean.TRUE.equals(object.getFullScopeSelected()))
        .managed(Boolean.TRUE.equals(object.getManaged()))
        .createdAt(object.getCreatedAt())
        .lastModifiedAt(object.getLastModifiedAt())
        .version(object.getVersion())
        .build();
  }
}
