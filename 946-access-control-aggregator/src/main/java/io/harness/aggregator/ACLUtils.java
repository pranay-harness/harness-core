package io.harness.aggregator;

import static io.harness.accesscontrol.principals.PrincipalType.USER_GROUP;

import io.harness.accesscontrol.Principal;
import io.harness.accesscontrol.acl.models.ACL;
import io.harness.accesscontrol.acl.models.SourceMetadata;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ACLUtils {
  public static ACL getACL(
      String permission, Principal principal, RoleAssignmentDBO roleAssignment, String resourceSelector) {
    return ACL.builder()
        .roleAssignmentId(roleAssignment.getId())
        .scopeIdentifier(roleAssignment.getScopeIdentifier())
        .permissionIdentifier(permission)
        .sourceMetadata(SourceMetadata.builder()
                            .roleIdentifier(roleAssignment.getRoleIdentifier())
                            .roleAssignmentIdentifier(roleAssignment.getIdentifier())
                            .resourceGroupIdentifier(roleAssignment.getResourceGroupIdentifier())
                            .userGroupIdentifier(USER_GROUP.equals(roleAssignment.getPrincipalType())
                                    ? roleAssignment.getPrincipalIdentifier()
                                    : null)
                            .build())
        .resourceSelector(resourceSelector)
        .principalType(principal.getPrincipalType().name())
        .principalIdentifier(principal.getPrincipalIdentifier())
        .aclQueryString(ACL.getAclQueryString(roleAssignment.getScopeIdentifier(), resourceSelector,
            principal.getPrincipalType().name(), principal.getPrincipalIdentifier(), permission))
        .enabled(!roleAssignment.isDisabled())
        .build();
  }
}
