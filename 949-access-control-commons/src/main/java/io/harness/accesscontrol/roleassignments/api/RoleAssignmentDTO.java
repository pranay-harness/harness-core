/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.accesscontrol.roleassignments.api;

import static io.harness.accesscontrol.roleassignments.api.RoleAssignmentDTO.MODEL_NAME;
import static io.harness.annotations.dev.HarnessTeam.PL;

import static java.lang.Boolean.TRUE;

import io.harness.accesscontrol.principals.PrincipalDTO;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@Getter
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "RoleAssignmentDTOKey")
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@ApiModel(value = MODEL_NAME)
@OwnedBy(PL)
public class RoleAssignmentDTO {
  public static final String MODEL_NAME = "RoleAssignment";

  final String identifier;
  @ApiModelProperty(required = true) final String resourceGroupIdentifier;
  @ApiModelProperty(required = true) final String roleIdentifier;
  @ApiModelProperty(required = true) final PrincipalDTO principal;
  @Setter boolean disabled;
  @Getter(AccessLevel.NONE) final Boolean managed;

  public Boolean isManaged() {
    return TRUE.equals(managed);
  }
}
