package io.harness.accesscontrol.permissions.api;

import io.harness.accesscontrol.permissions.Permission;
import io.harness.accesscontrol.resources.resourcetypes.ResourceType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.PL)
@UtilityClass
class PermissionDTOMapper {
  public static PermissionResponseDTO toDTO(Permission object, ResourceType resourceType) {
    return PermissionResponseDTO.builder()
        .permission(PermissionDTO.builder()
                        .identifier(object.getIdentifier())
                        .name(object.getName())
                        .resourceType(resourceType.getIdentifier())
                        .action(object.getPermissionMetadata(2))
                        .status(object.getStatus())
                        .allowedScopeLevels(object.getAllowedScopeLevels())
                        .build())
        .build();
  }
}
