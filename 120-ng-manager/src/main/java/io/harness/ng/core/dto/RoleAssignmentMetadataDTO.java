package io.harness.ng.core.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Value;

@OwnedBy(PL)
@Value
@Builder
public class RoleAssignmentMetadataDTO {
  @ApiModelProperty(required = true) String identifier;
  @ApiModelProperty(required = true) String roleIdentifier;
  @ApiModelProperty(required = true) String roleName;
  @ApiModelProperty(required = true) String resourceGroupIdentifier;
  @ApiModelProperty(required = true) String resourceGroupName;
  @ApiModelProperty(required = true) boolean managedRole;
  @ApiModelProperty(required = true) boolean managedRoleAssignment;
}
