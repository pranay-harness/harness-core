package io.harness.accesscontrol.roleassignments.api;

import io.harness.accesscontrol.roleassignments.RoleAssignment;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RoleAssignmentDTOMapper {
  public static RoleAssignmentResponseDTO toDTO(RoleAssignment object) {
    return RoleAssignmentResponseDTO.builder()
        .roleAssignment(RoleAssignmentDTO.builder()
                            .identifier(object.getIdentifier())
                            .principalIdentifier(object.getPrincipalIdentifier())
                            .principalType(object.getPrincipalType())
                            .resourceGroupIdentifier(object.getResourceGroupIdentifier())
                            .roleIdentifier(object.getRoleIdentifier())
                            .build())
        .scope(object.getScopeIdentifier())
        .disabled(object.isDisabled())
        .harnessManaged(object.isManaged())
        .createdAt(object.getCreatedAt())
        .lastModifiedAt(object.getLastModifiedAt())
        .build();
  }

  public static RoleAssignment fromDTO(String scopeIdentifier, RoleAssignmentDTO object) {
    return RoleAssignment.builder()
        .identifier(object.getIdentifier())
        .scopeIdentifier(scopeIdentifier)
        .principalIdentifier(object.getPrincipalIdentifier())
        .principalType(object.getPrincipalType())
        .resourceGroupIdentifier(object.getResourceGroupIdentifier())
        .roleIdentifier(object.getRoleIdentifier())
        .build();
  }
}
