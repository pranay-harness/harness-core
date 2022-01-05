package io.harness.ng.core.dto;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.PL)
@Schema(name = "TokenAggregate", description = "This has token details and metadata.")
public class TokenAggregateDTO {
  @NotNull private TokenDTO token;
  @NotNull @Schema(description = "Expiry time of the Token.") private Long expiryAt;
  @NotNull @Schema(description = "This is the time at which Token was created.") private Long createdAt;
  @NotNull @Schema(description = "This is the time at which Token was last modified.") private Long lastModifiedAt;
}
