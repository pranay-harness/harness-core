package io.harness.ng.core.invites.remote;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@FieldNameConstants(innerTypeName = "RoleBindingKeys")
@Builder
@OwnedBy(PL)
public class RoleBinding {
  String identifier;
  @ApiModelProperty(required = true) String roleIdentifier;
  @ApiModelProperty(required = true) String roleName;
  String resourceGroupIdentifier;
  String resourceGroupName;
  @ApiModelProperty(required = true) boolean managedRole;
  @ApiModelProperty(required = true) boolean managedRoleAssignment;
}