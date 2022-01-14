package io.harness.accesscontrol.commons;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.PL)
@Value
@Builder
@ApiModel(value = "ValidationResult")
@Schema(name = "ValidationResult")
public class ValidationResultDTO {
  boolean isValid;
  String errorMessage;
}
