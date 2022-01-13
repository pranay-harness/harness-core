/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

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
