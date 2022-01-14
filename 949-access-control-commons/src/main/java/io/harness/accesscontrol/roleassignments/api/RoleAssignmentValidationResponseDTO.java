package io.harness.accesscontrol.roleassignments.api;

import io.harness.accesscontrol.commons.ValidationResultDTO;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.PL)
@Value
@Builder
@ApiModel(value = "RoleAssignmentValidationResponse")
@Schema(name = "RoleAssignmentValidationResponse")
public class RoleAssignmentValidationResponseDTO {
  ValidationResultDTO principalValidationResult;
  ValidationResultDTO roleValidationResult;
  ValidationResultDTO resourceGroupValidationResult;
}
