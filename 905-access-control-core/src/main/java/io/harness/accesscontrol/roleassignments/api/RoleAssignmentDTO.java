package io.harness.accesscontrol.roleassignments.api;

import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.data.validator.EntityIdentifier;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@Builder
@ApiModel(value = "RoleAssignment")
public class RoleAssignmentDTO {
  @ApiModelProperty(required = true) @EntityIdentifier String identifier;
  @ApiModelProperty(required = true) @NotEmpty String resourceGroupIdentifier;
  @ApiModelProperty(required = true) @NotEmpty String roleIdentifier;
  @ApiModelProperty(required = true) @NotEmpty String principalIdentifier;
  @ApiModelProperty(required = true) @NotNull PrincipalType principalType;
}