package io.harness.accesscontrol.roleassignments.api;

import io.swagger.annotations.ApiModel;
import java.util.List;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@ApiModel(value = "BatchRoleAssignmentCreateRequest")
public class RoleAssignmentCreateRequestDTO {
  @NotEmpty List<RoleAssignmentDTO> roleAssignments;
}
