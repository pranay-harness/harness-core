package io.harness.ng.core.dto.secrets;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SecretResponseWrapper {
  @NotNull private SecretDTOV2 secret;
  private Long createdAt;
  private Long updatedAt;
  private boolean draft;
}
