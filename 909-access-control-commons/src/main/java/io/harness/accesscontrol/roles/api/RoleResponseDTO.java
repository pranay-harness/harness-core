package io.harness.accesscontrol.roles.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@ApiModel(value = "RoleResponse")
public class RoleResponseDTO {
  @ApiModelProperty(required = true) RoleDTO role;
  @ApiModelProperty(required = true) String scope;
  boolean harnessManaged;
  Long createdAt;
  Long lastModifiedAt;
}
