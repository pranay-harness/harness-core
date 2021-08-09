package io.harness.ng.core.dto.secrets;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@OwnedBy(PL)
@Data
@Builder
public class SecretResponseWrapper {
  @NotNull private SecretDTOV2 secret;
  private Long createdAt;
  private Long updatedAt;
  private boolean draft;
}
