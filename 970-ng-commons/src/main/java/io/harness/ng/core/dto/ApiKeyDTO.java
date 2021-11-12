package io.harness.ng.core.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.EntityIdentifier;
import io.harness.ng.core.common.beans.ApiKeyType;

import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import javax.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

@Data
@Builder
@OwnedBy(PL)
@Schema(name = "ApiKey", description = "This is the view of the ApiKey entity defined in Harness")
public class ApiKeyDTO {
  @ApiModelProperty(required = true) @EntityIdentifier @NotBlank private String identifier;
  @ApiModelProperty(required = true) @NotBlank private String name;
  @Size(max = 1024) String description;
  @Size(max = 128) Map<String, String> tags;
  @ApiModelProperty(required = true) @NotBlank private ApiKeyType apiKeyType;
  @ApiModelProperty(required = true) @NotBlank private String parentIdentifier;
  private Long defaultTimeToExpireToken;

  @ApiModelProperty(required = true) @NotBlank private String accountIdentifier;
  @EntityIdentifier(allowBlank = true) private String projectIdentifier;
  @EntityIdentifier(allowBlank = true) private String orgIdentifier;
}
