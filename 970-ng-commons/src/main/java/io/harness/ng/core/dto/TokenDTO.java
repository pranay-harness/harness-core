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
public class TokenDTO {
  private String identifier;
  @NotNull private String name;
  private Long validFrom;
  private Long validTo;
  private Long scheduledExpireTime;
  private boolean valid;

  @NotNull private String accountIdentifier;
  private String projectIdentifier;
  private String orgIdentifier;
  @NotNull private String apiKeyIdentifier;
  @NotNull private String parentIdentifier;
  @NotNull private ApiKeyType apiKeyType;
}
