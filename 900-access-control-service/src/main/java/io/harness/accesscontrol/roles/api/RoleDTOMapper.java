package io.harness.accesscontrol.roles.api;

import io.harness.accesscontrol.roles.Role;

import java.util.HashSet;
import lombok.experimental.UtilityClass;

@UtilityClass
public class RoleDTOMapper {
  public static RoleResponseDTO toDTO(Role object) {
    return RoleResponseDTO.builder()
        .role(RoleDTO.builder()
                  .identifier(object.getIdentifier())
                  .name(object.getName())
                  .allowedScopeLevels(object.getAllowedScopeLevels())
                  .permissions(object.getPermissions())
                  .description(object.getDescription())
                  .tags(object.getTags())
                  .build())
        .scope(object.getScopeIdentifier())
        .harnessManaged(object.isManaged())
        .createdAt(object.getCreatedAt())
        .lastModifiedAt(object.getLastModifiedAt())
        .build();
  }

  public static Role fromDTO(String scopeIdentifier, RoleDTO object) {
    return Role.builder()
        .identifier(object.getIdentifier())
        .scopeIdentifier(scopeIdentifier)
        .name(object.getName())
        .allowedScopeLevels(object.getAllowedScopeLevels())
        .permissions(object.getPermissions() == null ? new HashSet<>() : object.getPermissions())
        .description(object.getDescription())
        .tags(object.getTags())
        .managed(false)
        .build();
  }
}
