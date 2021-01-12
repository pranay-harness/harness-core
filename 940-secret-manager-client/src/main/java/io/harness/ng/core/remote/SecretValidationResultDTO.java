package io.harness.ng.core.remote;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SecretValidationResultDTO {
  private boolean success;
  private String message;
}
