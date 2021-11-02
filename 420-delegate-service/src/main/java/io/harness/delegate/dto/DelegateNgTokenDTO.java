package io.harness.delegate.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.EntityIdentifier;
import io.harness.gitsync.beans.YamlDTO;

import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
@OwnedBy(PL)
public class DelegateNgTokenDTO implements YamlDTO {
  @ApiModelProperty(required = true) @EntityIdentifier private String identifier;
  @ApiModelProperty(required = true) @NotEmpty private String name;

  @ApiModelProperty(required = true) private String accountIdentifier;
  @EntityIdentifier(allowBlank = true) private String projectIdentifier;
  @EntityIdentifier(allowBlank = true) private String orgIdentifier;
}