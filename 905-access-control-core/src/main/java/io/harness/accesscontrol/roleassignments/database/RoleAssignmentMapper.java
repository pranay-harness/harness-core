package io.harness.accesscontrol.roleassignments.database;

import io.harness.accesscontrol.roleassignments.RoleAssignmentDTO;

import lombok.experimental.UtilityClass;

@UtilityClass
class RoleAssignmentMapper {
  public static RoleAssignment fromDTO(RoleAssignmentDTO dto) {
    return RoleAssignment.builder()
        .identifier(dto.getIdentifier())
        .parentIdentifier(dto.getParentIdentifier())
        .resourceGroupIdentifier(dto.getResourceGroupIdentifier())
        .principalIdentifier(dto.getPrincipalIdentifier())
        .principalType(dto.getPrincipalType())
        .roleIdentifier(dto.getRoleIdentifier())
        .isDefault(dto.isDefault())
        .isDisabled(dto.isDisabled())
        .version(dto.getVersion())
        .build();
  }

  public static RoleAssignmentDTO toDTO(RoleAssignment object) {
    return RoleAssignmentDTO.builder()
        .identifier(object.getIdentifier())
        .parentIdentifier(object.getParentIdentifier())
        .resourceGroupIdentifier(object.getResourceGroupIdentifier())
        .principalIdentifier(object.getPrincipalIdentifier())
        .principalType(object.getPrincipalType())
        .roleIdentifier(object.getRoleIdentifier())
        .isDefault(object.isDefault())
        .isDisabled(object.isDisabled())
        .version(object.getVersion())
        .build();
  }
}
