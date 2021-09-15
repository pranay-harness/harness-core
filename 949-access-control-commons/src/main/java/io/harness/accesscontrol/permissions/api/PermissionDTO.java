/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.accesscontrol.permissions.api;

import io.harness.accesscontrol.permissions.PermissionStatus;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.Set;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@OwnedBy(HarnessTeam.PL)
@Value
@Builder
@FieldNameConstants(innerTypeName = "PermissionDTOKeys")
@ApiModel(value = PermissionDTO.MODEL_NAME)
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
