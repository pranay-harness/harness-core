package io.harness.accesscontrol.roleassignments.api;

import io.swagger.annotations.ApiModel;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@ApiModel(value = "BatchRoleAssignmentCreateRequest")
@Builder
public class RoleAssignmentCreateRequestDTO {
  @NotEmpty List<RoleAssignmentDTO> roleAssignments;
}
