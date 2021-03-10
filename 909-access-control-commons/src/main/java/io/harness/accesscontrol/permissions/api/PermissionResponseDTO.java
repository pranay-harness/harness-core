package io.harness.accesscontrol.permissions.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@ApiModel(value = "PermissionResponse")
public class PermissionResponseDTO {
  @ApiModelProperty(required = true) PermissionDTO permission;
}
