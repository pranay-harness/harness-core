package io.harness.accesscontrol.roles.database;

import io.harness.accesscontrol.roles.RoleDTO;

import lombok.experimental.UtilityClass;

@UtilityClass
class RoleMapper {
  public static Role toRole(RoleDTO dto) {
    return Role.builder()
        .identifier(dto.getIdentifier())
        .parentIdentifier(dto.getParentIdentifier())
        .displayName(dto.getDisplayName())
        .validScopes(dto.getValidScopes())
        .permissions(dto.getPermissions())
        .isDefault(dto.isDefault())
        .version(dto.getVersion())
        .build();
  }

  public static RoleDTO fromRole(Role role) {
    return RoleDTO.builder()
        .identifier(role.getIdentifier())
        .parentIdentifier(role.getParentIdentifier())
        .displayName(role.getDisplayName())
        .validScopes(role.getValidScopes())
        .permissions(role.getPermissions())
        .isDefault(role.isDefault())
        .version(role.getVersion())
        .build();
  }
}
