package io.harness.accesscontrol.permissions.api;

import io.harness.accesscontrol.permissions.Permission;

import java.util.Arrays;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
class PermissionDTOMapper {
  public static PermissionResponseDTO toDTO(Permission object) {
    List<String> permissionMetadata = Arrays.asList(object.getIdentifier().split("\\."));
    String resourceType = permissionMetadata.get(permissionMetadata.size() - 2);
    String action = permissionMetadata.get(permissionMetadata.size() - 1);
    return PermissionResponseDTO.builder()
        .permission(PermissionDTO.builder()
                        .identifier(object.getIdentifier())
                        .name(object.getName())
                        .resourceType(resourceType)
                        .action(action)
                        .status(object.getStatus())
                        .scopes(object.getScopes())
                        .build())
        .build();
  }

  public static Permission fromDTO(PermissionDTO object) {
    return Permission.builder()
        .identifier(object.getIdentifier())
        .name(object.getName())
        .status(object.getStatus())
        .scopes(object.getScopes())
        .build();
  }
}
