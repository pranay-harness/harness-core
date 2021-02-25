package io.harness.accesscontrol.resources.resourcegroups.persistence;

import io.harness.accesscontrol.resources.resourcegroups.ResourceGroup;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ResourceGroupDBOMapper {
  public static ResourceGroupDBO toDBO(ResourceGroup object) {
    return ResourceGroupDBO.builder()
        .identifier(object.getIdentifier())
        .scopeIdentifier(object.getScopeIdentifier())
        .name(object.getName())
        .resourceSelectors(object.getResourceSelectors())
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
        .resourceSelectors(object.getResourceSelectors())
        .managed(object.isManaged())
        .createdAt(object.getCreatedAt())
        .lastModifiedAt(object.getLastModifiedAt())
        .version(object.getVersion())
        .build();
  }
}
