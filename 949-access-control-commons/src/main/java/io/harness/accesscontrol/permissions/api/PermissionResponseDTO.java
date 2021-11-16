package io.harness.accesscontrol.permissions.api;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.PL)
@Value
@Builder
@ApiModel(value = "PermissionResponse")
@Schema(name = "PermissionResponse")
public class PermissionResponseDTO {
  @ApiModelProperty(required = true) PermissionDTO permission;
}
