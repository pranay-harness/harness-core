package io.harness.aggregator;

import static io.harness.accesscontrol.principals.PrincipalType.USER_GROUP;

import io.harness.accesscontrol.Principal;
import io.harness.accesscontrol.acl.persistence.ACL;
import io.harness.accesscontrol.acl.persistence.SourceMetadata;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.PL)
@UtilityClass
public class ACLUtils {
  public static ACL buildACL(
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
