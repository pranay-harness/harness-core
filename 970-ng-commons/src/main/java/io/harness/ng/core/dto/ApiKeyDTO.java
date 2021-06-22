package io.harness.ng.core.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.common.beans.ApiKeyType;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(PL)
public class ApiKeyDTO {
  @NotNull private String identifier;
  @NotNull private ApiKeyType apiKeyType;
  @NotNull private String parentIdentifier;
  private Long defaultTimeToExpireToken;

  @NotNull private String accountIdentifier;
  private String projectIdentifier;
  private String orgIdentifier;
}
