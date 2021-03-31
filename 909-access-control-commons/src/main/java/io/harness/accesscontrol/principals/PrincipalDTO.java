package io.harness.accesscontrol.principals;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "PrincipalDTOKeys")
@ApiModel(value = "Principal")
public class PrincipalDTO {
  @ApiModelProperty(required = true) String identifier;
  @ApiModelProperty(required = true) PrincipalType type;
}
