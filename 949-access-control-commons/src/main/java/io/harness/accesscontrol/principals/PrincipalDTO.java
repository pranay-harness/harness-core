package io.harness.accesscontrol.principals;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "PrincipalDTOKeys")
@ApiModel(value = "Principal")
@Schema(name = "Principal")
@OwnedBy(PL)
public class PrincipalDTO {
  @ApiModelProperty(required = true) String identifier;
  @ApiModelProperty(required = true) PrincipalType type;
}
