/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.accesscontrol.roleassignments.privileged;

import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.PL)
@UtilityClass
public class AdminPrivilegedRoleAssignmentMapper {
  public static final Map<String, String> roleToPrivilegedRole =
      Stream
          .of(new AbstractMap.SimpleEntry<>("_account_admin", "_super_account_admin"),
              new AbstractMap.SimpleEntry<>("_organization_admin", "_super_organization_admin"))
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  public static final String ALL_RESOURCES_RESOURCE_GROUP = "_all_resources";

  public static Optional<PrivilegedRoleAssignment> buildAdminPrivilegedRoleAssignment(
      RoleAssignmentDBO roleAssignment) {
    if (roleToPrivilegedRole.containsKey(roleAssignment.getRoleIdentifier())
        && roleAssignment.getResourceGroupIdentifier().equals(ALL_RESOURCES_RESOURCE_GROUP)) {
      PrivilegedRoleAssignment privilegedRoleAssignment =
          PrivilegedRoleAssignment.builder()
              .principalIdentifier(roleAssignment.getPrincipalIdentifier())
              .principalType(roleAssignment.getPrincipalType())
              .roleIdentifier(roleToPrivilegedRole.get(roleAssignment.getRoleIdentifier()))
              .scopeIdentifier(roleAssignment.getScopeIdentifier())
              .linkedRoleAssignment(roleAssignment.getId())
              .managed(false)
              .global(false)
              .build();
      return Optional.of(privilegedRoleAssignment);
    }
    return Optional.empty();
  }
}
