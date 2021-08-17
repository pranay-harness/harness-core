package io.harness.accesscontrol.roleassignments.privileged.persistence;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.roleassignments.privileged.PrivilegedRoleAssignment;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class PrivilegedRoleAssignmentDBOMapper {
  public static PrivilegedRoleAssignmentDBO toDBO(PrivilegedRoleAssignment object) {
    return PrivilegedRoleAssignmentDBO.builder()
        .principalType(object.getPrincipalType())
        .principalIdentifier(object.getPrincipalIdentifier())
        .roleIdentifier(object.getRoleIdentifier())
        .global(object.isGlobal())
        .scopeIdentifier(object.getScopeIdentifier())
        .managed(object.isManaged())
        .linkedRoleAssignment(object.getLinkedRoleAssignment())
        .userGroupIdentifier(object.getUserGroupIdentifier())
        .build();
  }

  public static PrivilegedRoleAssignment fromDBO(PrivilegedRoleAssignmentDBO object) {
    return PrivilegedRoleAssignment.builder()
        .principalType(object.getPrincipalType())
        .principalIdentifier(object.getPrincipalIdentifier())
        .roleIdentifier(object.getRoleIdentifier())
        .global(object.isGlobal())
        .scopeIdentifier(object.getScopeIdentifier())
        .managed(object.isManaged())
        .linkedRoleAssignment(object.getLinkedRoleAssignment())
        .userGroupIdentifier(object.getUserGroupIdentifier())
        .build();
  }
}
