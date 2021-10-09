package io.harness.ng.core.dto;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.PL)
public class ApiKeyAggregateDTO {
  @NotNull private ApiKeyDTO apiKey;
  @NotNull private Long createdAt;
  @NotNull private Long lastModifiedAt;

  private int tokensCount;
}
