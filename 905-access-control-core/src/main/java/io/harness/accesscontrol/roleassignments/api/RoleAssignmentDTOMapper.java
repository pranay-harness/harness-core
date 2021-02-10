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
        .parentIdentifier(object.getParentIdentifier())
        .disabled(object.isDisabled())
        .harnessManaged(object.isManaged())
        .createdAt(object.getCreatedAt())
        .lastModifiedAt(object.getLastModifiedAt())
        .build();
  }

  public static RoleAssignment fromDTO(String parentIdentifier, RoleAssignmentDTO object) {
    return RoleAssignment.builder()
        .identifier(object.getIdentifier())
        .parentIdentifier(parentIdentifier)
        .principalIdentifier(object.getPrincipalIdentifier())
        .principalType(object.getPrincipalType())
        .resourceGroupIdentifier(object.getResourceGroupIdentifier())
        .roleIdentifier(object.getRoleIdentifier())
        .build();
  }
}
