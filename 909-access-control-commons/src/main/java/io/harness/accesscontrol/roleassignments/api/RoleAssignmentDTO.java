package io.harness.accesscontrol.roleassignments.api;

import io.harness.accesscontrol.principals.PrincipalDTO;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@ApiModel(value = "RoleAssignment")
public class RoleAssignmentDTO {
  @ApiModelProperty(required = true) String identifier;
  @ApiModelProperty(required = true) String resourceGroupIdentifier;
  @ApiModelProperty(required = true) String roleIdentifier;
  @ApiModelProperty(required = true) PrincipalDTO principal;
  boolean harnessManaged;
  boolean disabled;
}