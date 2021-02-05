package io.harness.accesscontrol.permissions.database;

import io.harness.accesscontrol.permissions.PermissionDTO;

import lombok.experimental.UtilityClass;

@UtilityClass
class PermissionMapper {
  public static Permission toPermission(PermissionDTO dto) {
    return Permission.builder()
        .identifier(dto.getIdentifier())
        .displayName(dto.getDisplayName())
        .resourceType(dto.getResourceType())
        .action(dto.getAction())
        .status(dto.getStatus())
        .scopes(dto.getScopes())
        .version(dto.getVersion())
        .build();
  }

  public static PermissionDTO fromPermission(Permission permission) {
    return PermissionDTO.builder()
        .identifier(permission.getIdentifier())
        .displayName(permission.getDisplayName())
        .resourceType(permission.getResourceType())
        .action(permission.getAction())
        .status(permission.getStatus())
        .scopes(permission.getScopes())
        .version(permission.getVersion())
        .build();
  }
}
