package io.harness.accesscontrol.permissions.api;

import io.harness.accesscontrol.permissions.Permission;

import lombok.experimental.UtilityClass;

@UtilityClass
class PermissionDTOMapper {
  public static PermissionResponseDTO toDTO(Permission object) {
    return PermissionResponseDTO.builder()
        .permission(PermissionDTO.builder()
                        .identifier(object.getIdentifier())
                        .name(object.getName())
                        .resourceType(object.getResourceType())
                        .action(object.getAction())
                        .status(object.getStatus())
                        .allowedScopeLevels(object.getAllowedScopeLevels())
                        .build())
        .build();
  }

  public static Permission fromDTO(PermissionDTO object) {
    return Permission.builder()
        .identifier(object.getIdentifier())
        .name(object.getName())
        .status(object.getStatus())
        .allowedScopeLevels(object.getAllowedScopeLevels())
        .build();
  }
}
