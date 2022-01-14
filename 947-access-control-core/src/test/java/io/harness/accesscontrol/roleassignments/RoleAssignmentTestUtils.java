package io.harness.accesscontrol.roleassignments;

import static io.harness.accesscontrol.common.AccessControlTestUtils.getRandomString;

import io.harness.accesscontrol.principals.Principal;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;
import org.hibernate.validator.constraints.NotEmpty;

@UtilityClass
@OwnedBy(HarnessTeam.PL)
public class RoleAssignmentTestUtils {
  public static RoleAssignment buildRoleAssignment(
      @NotEmpty String scopeId, @NotEmpty String roleId, @NotEmpty String rgId, @NotEmpty Principal principal) {
    return RoleAssignment.builder()
        .identifier(getRandomString(20))
        .scopeIdentifier(scopeId)
        .roleIdentifier(roleId)
        .principalIdentifier(principal.getPrincipalIdentifier())
        .principalType(principal.getPrincipalType())
        .resourceGroupIdentifier(rgId)
        .build();
  }

  public static RoleAssignmentDBO buildRoleAssignmentDBO(
      @NotEmpty String scopeId, @NotEmpty String roleId, @NotEmpty String rgId, @NotEmpty Principal principal) {
    return RoleAssignmentDBO.builder()
        .id(getRandomString(20))
        .identifier(getRandomString(20))
        .scopeIdentifier(scopeId)
        .roleIdentifier(roleId)
        .resourceGroupIdentifier(rgId)
        .principalIdentifier(principal.getPrincipalIdentifier())
        .principalType(principal.getPrincipalType())
        .build();
  }
}
