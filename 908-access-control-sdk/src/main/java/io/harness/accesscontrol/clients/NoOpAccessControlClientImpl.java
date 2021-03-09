package io.harness.accesscontrol.clients;

import io.harness.accesscontrol.HUserPrincipal;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.security.SecurityContextBuilder;
import io.harness.security.dto.Principal;
import io.harness.security.dto.UserPrincipal;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class NoOpAccessControlClientImpl implements AccessControlClient {
  @Override
  public AccessCheckResponseDTO checkForAccess(
      String principal, PrincipalType principalType, List<PermissionCheckDTO> permissionCheckDTOList) {
    return AccessCheckResponseDTO.builder()
        .principal(HUserPrincipal.builder().principalIdentifier(principal).principalType(principalType).build())
        .accessControlList(permissionCheckDTOList.stream()
                               .map(x
                                   -> HAccessControlDTO.builder()
                                          .accountIdentifier(x.getAccountIdentifier())
                                          .orgIdentifier(x.getOrgIdentifier())
                                          .projectIdentifier(x.getProjectIdentifier())
                                          .permission(x.getPermission())
                                          .resourceIdentifier(x.getResourceIdentifier())
                                          .resourceType(x.getResourceType())
                                          .accessible(true)
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
    Principal principal = SecurityContextBuilder.getPrincipal();
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