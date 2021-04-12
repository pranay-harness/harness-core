package io.harness.accesscontrol.roleassignments.api;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.annotations.ApiModel;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@ApiModel(value = "BatchRoleAssignmentCreateRequest")
@Builder
@OwnedBy(HarnessTeam.PL)
public class RoleAssignmentCreateRequestDTO {
  @NotEmpty List<RoleAssignmentDTO> roleAssignments;
}
