package io.harness.ng.core.dto.secrets;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SecretResponseWrapper {
  private SecretDTOV2 secret;
  private Long createdAt;
  private Long updatedAt;
}
