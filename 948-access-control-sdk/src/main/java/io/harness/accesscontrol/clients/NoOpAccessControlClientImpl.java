package io.harness.accesscontrol.clients;

import io.harness.accesscontrol.Principal;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.security.SecurityContextBuilder;
import io.harness.security.dto.ServiceAccountPrincipal;
import io.harness.security.dto.UserPrincipal;

import java.util.List;
import java.util.stream.Collectors;

@OwnedBy(HarnessTeam.PL)
public class NoOpAccessControlClientImpl implements AccessControlClient {
  @Override
  public AccessCheckResponseDTO checkForAccess(Principal principal, List<PermissionCheckDTO> permissionCheckDTOList) {
    return AccessCheckResponseDTO.builder()
        .principal(principal)
        .accessControlList(permissionCheckDTOList.stream()
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

  @Override
  public AccessCheckResponseDTO checkForAccess(List<PermissionCheckDTO> permissionCheckDTOList) {
    io.harness.security.dto.Principal principal = SecurityContextBuilder.getPrincipal();
    if (principal == null) {
      return null;
    }
    if (principal instanceof UserPrincipal) {
      return checkForAccess(Principal.of(PrincipalType.USER, principal.getName()), permissionCheckDTOList);
    }
    if (principal instanceof ServiceAccountPrincipal) {
      return checkForAccess(Principal.of(PrincipalType.SERVICE_ACCOUNT, principal.getName()), permissionCheckDTOList);
    }
    throw new UnsupportedOperationException("Only <USER, SERVICE_ACCOUNT> principal type is supported");
  }

  @Override
  public boolean hasAccess(Principal principal, ResourceScope resourceScope, Resource resource, String permission) {
    return true;
  }

  @Override
  public boolean hasAccess(ResourceScope resourceScope, Resource resource, String permission) {
    return true;
  }

  @Override
  public void checkForAccessOrThrow(ResourceScope resourceScope, Resource resource, String permission) {
    // do nothing
  }

  @Override
  public void checkForAccessOrThrow(
      ResourceScope resourceScope, Resource resource, String permission, String exceptionMessage) {
    // do nothing
  }

  @Override
  public void checkForAccessOrThrow(
      Principal principal, ResourceScope resourceScope, Resource resource, String permission, String exceptionMessage) {
    // do nothing
  }
}