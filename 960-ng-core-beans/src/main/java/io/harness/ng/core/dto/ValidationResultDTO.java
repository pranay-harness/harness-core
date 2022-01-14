package io.harness.ng.core.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@OwnedBy(PL)
@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode
@Schema(name = "ValidationResult")
public class ValidationResultDTO {
  private boolean valid;
  private String errorMessage;
}
