package io.harness.accesscontrol.roleassignments.persistence;

import io.harness.accesscontrol.roleassignments.RoleAssignment;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.PL)
@UtilityClass
class RoleAssignmentDBOMapper {
  public static RoleAssignmentDBO toDBO(RoleAssignment object) {
    return RoleAssignmentDBO.builder()
        .identifier(object.getIdentifier())
        .scopeIdentifier(object.getScopeIdentifier())
        .scopeLevel(object.getScopeLevel())
        .resourceGroupIdentifier(object.getResourceGroupIdentifier())
        .principalIdentifier(object.getPrincipalIdentifier())
        .principalType(object.getPrincipalType())
        .roleIdentifier(object.getRoleIdentifier())
        .managed(object.isManaged())
        .disabled(object.isDisabled())
        .version(object.getVersion())
        .createdAt(object.getCreatedAt())
        .lastModifiedAt(object.getLastModifiedAt())
        .build();
  }

  public static RoleAssignment fromDBO(RoleAssignmentDBO object) {
    return RoleAssignment.builder()
        .identifier(object.getIdentifier())
        .scopeIdentifier(object.getScopeIdentifier())
        .scopeLevel(object.getScopeLevel())
        .resourceGroupIdentifier(object.getResourceGroupIdentifier())
        .principalIdentifier(object.getPrincipalIdentifier())
        .principalType(object.getPrincipalType())
        .roleIdentifier(object.getRoleIdentifier())
        .managed(object.isManaged())
        .disabled(object.isDisabled())
        .version(object.getVersion())
        .createdAt(object.getCreatedAt())
        .lastModifiedAt(object.getLastModifiedAt())
        .build();
  }
}
