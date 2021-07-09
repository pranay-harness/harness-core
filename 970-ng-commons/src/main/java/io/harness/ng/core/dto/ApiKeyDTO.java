package io.harness.ng.core.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.EntityIdentifier;
import io.harness.ng.core.common.beans.ApiKeyType;

import io.swagger.annotations.ApiModelProperty;
import java.util.Map;
import javax.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
@OwnedBy(PL)
public class ApiKeyDTO {
  @ApiModelProperty(required = true) @EntityIdentifier private String identifier;
  @ApiModelProperty(required = true) @NotEmpty private String name;
  @Size(max = 1024) String description;
  @Size(max = 128) Map<String, String> tags;
  @ApiModelProperty(required = true) private ApiKeyType apiKeyType;
  @ApiModelProperty(required = true) private String parentIdentifier;
  private Long defaultTimeToExpireToken;

  @ApiModelProperty(required = true) private String accountIdentifier;
  @EntityIdentifier(allowBlank = true) private String projectIdentifier;
  @EntityIdentifier(allowBlank = true) private String orgIdentifier;
}
