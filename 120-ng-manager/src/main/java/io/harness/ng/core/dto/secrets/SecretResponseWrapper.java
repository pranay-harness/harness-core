package io.harness.ng.core.dto.secrets;

import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Builder
public class SecretResponseWrapper {
  @NotNull private SecretDTOV2 secret;
  private Long createdAt;
  private Long updatedAt;
}
