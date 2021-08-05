package io.harness.accesscontrol.clients;

import java.util.stream.Collectors;

public class PrivilegedNoopAccessControlClientImpl extends AbstractAccessControlClient {
  @Override
  protected AccessCheckResponseDTO checkForAccess(AccessCheckRequestDTO accessCheckRequestDTO) {
    return AccessCheckResponseDTO.builder()
        .principal(accessCheckRequestDTO.getPrincipal())
        .accessControlList(accessCheckRequestDTO.getPermissions()
                               .stream()
                               .map(permissionCheckDTO
                                   -> AccessControlDTO.builder()
                                          .permitted(true)
                                          .permission(permissionCheckDTO.getPermission())
                                          .resourceScope(permissionCheckDTO.getResourceScope())
                                          .resourceIdentifier(permissionCheckDTO.getResourceIdentifier())
                                          .resourceType(permissionCheckDTO.getResourceType())
                                          .build())
                               .collect(Collectors.toList()))
        .build();
  }
}
