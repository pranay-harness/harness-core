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
        .allowedScopeLevels(object.getAllowedScopeLevels())
        .version(object.getVersion())
        .build();
  }

  public static Permission fromDBO(PermissionDBO object) {
    return Permission.builder()
        .identifier(object.getIdentifier())
        .name(object.getName())
        .status(object.getStatus())
        .allowedScopeLevels(object.getAllowedScopeLevels())
        .version(object.getVersion())
        .build();
  }
}
