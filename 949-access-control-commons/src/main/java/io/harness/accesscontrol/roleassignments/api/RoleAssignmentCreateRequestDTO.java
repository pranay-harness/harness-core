package io.harness.accesscontrol.roleassignments.api;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@ApiModel(value = "RoleAssignmentCreateRequest")
@Schema(name = "RoleAssignmentCreateRequest")
@Builder
@OwnedBy(HarnessTeam.PL)
public class RoleAssignmentCreateRequestDTO {
  @NotEmpty List<RoleAssignmentDTO> roleAssignments;
}
