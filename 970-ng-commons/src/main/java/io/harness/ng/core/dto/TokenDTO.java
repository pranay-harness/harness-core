package io.harness.ng.core.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.EntityIdentifier;
import io.harness.ng.core.common.beans.ApiKeyType;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
@OwnedBy(PL)
public class TokenDTO {
  @ApiModelProperty @EntityIdentifier(allowBlank = true) private String identifier;
  @ApiModelProperty(required = true) @NotEmpty private String name;
  private Long validFrom;
  private Long validTo;
  private Long scheduledExpireTime;
  private boolean valid;

  @EntityIdentifier private String accountIdentifier;
  @EntityIdentifier(allowBlank = true) private String projectIdentifier;
  @EntityIdentifier(allowBlank = true) private String orgIdentifier;
  @EntityIdentifier private String apiKeyIdentifier;
  @EntityIdentifier private String parentIdentifier;
  @NotNull private ApiKeyType apiKeyType;
}
