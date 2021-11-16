package io.harness.accesscontrol.permissions.api;

import io.harness.accesscontrol.permissions.PermissionStatus;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@OwnedBy(HarnessTeam.PL)
@Value
@Builder
@FieldNameConstants(innerTypeName = "PermissionDTOKeys")
@ApiModel(value = PermissionDTO.MODEL_NAME)
@Schema(name = PermissionDTO.MODEL_NAME)
public class PermissionDTO {
  public static final String MODEL_NAME = "Permission";

  @ApiModelProperty(required = true) String identifier;
  @ApiModelProperty(required = true) String name;
  @ApiModelProperty(required = true) PermissionStatus status;
  @ApiModelProperty(required = true) boolean includeInAllRoles;
  @ApiModelProperty(required = true) Set<String> allowedScopeLevels;
  @ApiModelProperty(required = true) String resourceType;
  @ApiModelProperty(required = true) String action;
}
