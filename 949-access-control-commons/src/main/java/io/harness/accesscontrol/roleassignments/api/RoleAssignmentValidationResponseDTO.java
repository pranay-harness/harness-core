package io.harness.accesscontrol.roleassignments.api;

import io.harness.accesscontrol.commons.ValidationResultDTO;

import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@ApiModel(value = "RoleAssignmentValidationResponse")
public class RoleAssignmentValidationResponseDTO {
  ValidationResultDTO principalValidationResult;
  ValidationResultDTO roleValidationResult;
  ValidationResultDTO resourceGroupValidationResult;
}
