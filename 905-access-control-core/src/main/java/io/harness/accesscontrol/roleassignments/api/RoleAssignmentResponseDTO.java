package io.harness.accesscontrol.roleassignments.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@Builder
@ApiModel(value = "RoleAssignmentResponse")
public class RoleAssignmentResponseDTO {
  @ApiModelProperty(required = true) @NotNull RoleAssignmentDTO roleAssignment;
  @NotEmpty String parentIdentifier;
  boolean harnessManaged;
  boolean disabled;
  Long createdAt;
  Long lastModifiedAt;
}
