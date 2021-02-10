package io.harness.accesscontrol.roles.api;

import io.harness.accesscontrol.roles.Role;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RoleDTOMapper {
  public static RoleResponseDTO toDTO(Role object) {
    return RoleResponseDTO.builder()
        .role(RoleDTO.builder()
                  .identifier(object.getIdentifier())
                  .name(object.getName())
                  .scopes(object.getScopes())
                  .permissions(object.getPermissions())
                  .description(object.getDescription())
                  .tags(object.getTags())
                  .build())
        .parentIdentifier(object.getParentIdentifier())
        .harnessManaged(object.isManaged())
        .createdAt(object.getCreatedAt())
        .lastModifiedAt(object.getLastModifiedAt())
        .build();
  }

  public static Role fromDTO(String parentIdentifier, RoleDTO object) {
    return Role.builder()
        .identifier(object.getIdentifier())
        .parentIdentifier(parentIdentifier)
        .name(object.getName())
        .scopes(object.getScopes())
        .permissions(object.getPermissions())
        .description(object.getDescription())
        .tags(object.getTags())
        .build();
  }
}
