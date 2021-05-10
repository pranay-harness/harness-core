package io.harness.accesscontrol.roleassignments.api;

import io.harness.accesscontrol.commons.ValidationResultDTO;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.PL)
@Value
@Builder
@ApiModel(value = "RoleAssignmentValidationResponse")
public class RoleAssignmentValidationResponseDTO {
  ValidationResultDTO principalValidationResult;
  ValidationResultDTO roleValidationResult;
  ValidationResultDTO resourceGroupValidationResult;
}
