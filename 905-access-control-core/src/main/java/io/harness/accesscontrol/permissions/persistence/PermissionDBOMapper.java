package io.harness.accesscontrol.permissions.persistence;

import io.harness.accesscontrol.permissions.Permission;

import lombok.experimental.UtilityClass;

@UtilityClass
class PermissionDBOMapper {
  public static PermissionDBO toDBO(Permission object) {
    return PermissionDBO.builder()
        .identifier(object.getIdentifier())
        .name(object.getName())
        .status(object.getStatus())
        .allowedScopeLevels(object.getAllowedScopeLevels())
        .version(object.getVersion())
        .build();
  }

  public static Permission fromDBO(PermissionDBO object) {
    return Permission.builder()
        .identifier(object.getIdentifier())
        .name(object.getName())
        .status(object.getStatus())
        .allowedScopeLevels(object.getAllowedScopeLevels())
        .version(object.getVersion())
        .build();
  }
}
