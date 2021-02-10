package io.harness.accesscontrol.roles.persistence;

import io.harness.accesscontrol.roles.Role;

import lombok.experimental.UtilityClass;

@UtilityClass
class RoleDBOMapper {
  public static RoleDBO toDBO(Role object) {
    return RoleDBO.builder()
        .identifier(object.getIdentifier())
        .parentIdentifier(object.getParentIdentifier())
        .name(object.getName())
        .scopes(object.getScopes())
        .permissions(object.getPermissions())
        .managed(object.isManaged())
        .description(object.getDescription())
        .tags(object.getTags())
        .createdAt(object.getCreatedAt())
        .lastModifiedAt(object.getLastModifiedAt())
        .build();
  }

  public static Role fromDBO(RoleDBO roleDBO) {
    return Role.builder()
        .identifier(roleDBO.getIdentifier())
        .parentIdentifier(roleDBO.getParentIdentifier())
        .name(roleDBO.getName())
        .scopes(roleDBO.getScopes())
        .permissions(roleDBO.getPermissions())
        .managed(roleDBO.isManaged())
        .description(roleDBO.getDescription())
        .tags(roleDBO.getTags())
        .createdAt(roleDBO.getCreatedAt())
        .lastModifiedAt(roleDBO.getLastModifiedAt())
        .build();
  }
}
