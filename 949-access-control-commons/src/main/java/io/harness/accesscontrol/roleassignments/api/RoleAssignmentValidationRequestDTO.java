package io.harness.accesscontrol.roleassignments.api;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.PL)
@Value
@Builder
@ApiModel(value = "RoleAssignmentValidationRequest")
@Schema(name = "RoleAssignmentValidationRequest")
public class RoleAssignmentValidationRequestDTO {
  @Schema(description = "Role Assignment to validate", required = true)
  @NotNull
  @Valid
  RoleAssignmentDTO roleAssignment;
  @Schema(description = "Set it to true if the principal needs to be validated") boolean validatePrincipal;
  @Schema(description = "Set it to true if the role needs to be validated") boolean validateRole;
  @Schema(description = "Set it to true if the resource group needs to be validated") boolean validateResourceGroup;
}
