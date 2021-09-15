/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.accesscontrol.permissions.persistence;

import io.harness.accesscontrol.permissions.Permission;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.PL)
@UtilityClass
class PermissionDBOMapper {
  public static PermissionDBO toDBO(Permission object) {
    return PermissionDBO.builder()
        .identifier(object.getIdentifier())
        .name(object.getName())
        .status(object.getStatus())
        .includeInAllRoles(object.isIncludeInAllRoles())
        .allowedScopeLevels(object.getAllowedScopeLevels())
        .version(object.getVersion())
        .build();
  }

  public static Permission fromDBO(PermissionDBO object) {
    return Permission.builder()
        .identifier(object.getIdentifier())
        .name(object.getName())
        .status(object.getStatus())
        .includeInAllRoles(Boolean.TRUE.equals(object.getIncludeInAllRoles()))
        .allowedScopeLevels(object.getAllowedScopeLevels())
        .version(object.getVersion())
        .build();
  }
}
