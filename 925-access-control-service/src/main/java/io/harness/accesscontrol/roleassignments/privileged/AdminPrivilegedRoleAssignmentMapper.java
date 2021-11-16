package io.harness.accesscontrol.roleassignments.privileged;

import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.common.collect.ImmutableList;
import java.util.AbstractMap;
import java.util.List;
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
  public static final String ALL_PROJECT_RESOURCES = "_all_project_resources";
  public static final String ALL_ORGANIZATION_RESOURCES = "_all_organization_resources";
  public static final String ALL_ACCOUNT_RESOURCES = "_all_account_resources";
  public static final List<String> MANAGED_RESOURCE_GROUP_IDENTIFIERS = ImmutableList.of(
      ALL_RESOURCES_RESOURCE_GROUP, ALL_ACCOUNT_RESOURCES, ALL_ORGANIZATION_RESOURCES, ALL_PROJECT_RESOURCES);

  public static Optional<PrivilegedRoleAssignment> buildAdminPrivilegedRoleAssignment(
      RoleAssignmentDBO roleAssignment) {
    if (roleToPrivilegedRole.containsKey(roleAssignment.getRoleIdentifier())
        && MANAGED_RESOURCE_GROUP_IDENTIFIERS.stream().anyMatch(
            resourceGroupIdentifier -> resourceGroupIdentifier.equals(roleAssignment.getResourceGroupIdentifier()))) {
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
