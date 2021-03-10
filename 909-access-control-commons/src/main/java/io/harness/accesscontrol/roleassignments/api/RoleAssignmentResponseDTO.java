package io.harness.accesscontrol.roleassignments.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@ApiModel(value = "RoleAssignmentResponse")
public class RoleAssignmentResponseDTO {
  @ApiModelProperty(required = true) RoleAssignmentDTO roleAssignment;
  @ApiModelProperty(required = true) String scope;
  Long createdAt;
  Long lastModifiedAt;
}
