package io.harness.accesscontrol.clients;

import io.harness.accesscontrol.Principal;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.security.SecurityContextBuilder;
import io.harness.security.dto.UserPrincipal;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@OwnedBy(HarnessTeam.PL)
public class NoOpAccessControlClientImpl implements AccessControlClient {
  @Override
  public AccessCheckResponseDTO checkForAccess(
      String principal, PrincipalType principalType, List<PermissionCheckDTO> permissionCheckDTOList) {
    return AccessCheckResponseDTO.builder()
        .principal(Principal.builder().principalIdentifier(principal).principalType(principalType).build())
        .accessControlList(permissionCheckDTOList.stream()
                               .map(x
                                   -> AccessControlDTO.builder()
                                          .resourceScope(x.getResourceScope())
                                          .permission(x.getPermission())
                                          .resourceIdentifier(x.getResourceIdentifier())
                                          .resourceType(x.getResourceType())
                                          .permitted(true)
                                          .build())
                               .collect(Collectors.toList()))
        .build();
  }

  @Override
  public AccessControlDTO checkForAccess(
      String principal, PrincipalType principalType, PermissionCheckDTO permissionCheckDTO) {
    return checkForAccess(principal, principalType, Collections.singletonList(permissionCheckDTO))
        .getAccessControlList()
        .get(0);
  }

  @Override
  public AccessCheckResponseDTO checkForAccess(List<PermissionCheckDTO> permissionCheckDTOList) {
    io.harness.security.dto.Principal principal = SecurityContextBuilder.getPrincipal();
    if (principal == null) {
      return null;
    }
    if (principal instanceof UserPrincipal) {
      UserPrincipal userPrincipal = (UserPrincipal) principal;
      return checkForAccess(userPrincipal.getName(), PrincipalType.USER, permissionCheckDTOList);
    }
    throw new UnsupportedOperationException("Only <User> principal type is supported");
  }

  @Override
  public AccessControlDTO checkForAccess(PermissionCheckDTO permissionCheckDTO) {
    return checkForAccess(Collections.singletonList(permissionCheckDTO)).getAccessControlList().get(0);
  }

  @Override
  public boolean hasAccess(String principal, PrincipalType principalType, PermissionCheckDTO permissionCheckDTO) {
    return true;
  }

  @Override
  public boolean hasAccess(PermissionCheckDTO permissionCheckDTO) {
    return true;
  }

  @Override
  public void checkForAccessOrThrow(PermissionCheckDTO permissionCheckDTO) {
    // do nothing
  }
}