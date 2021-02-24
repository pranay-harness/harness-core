package io.harness.accesscontrol.clients;

import static io.harness.exception.WingsException.USER;

import io.harness.accesscontrol.HUserPrincipal;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.exception.AccessDeniedException;
import io.harness.remote.client.NGRestUtils;
import io.harness.security.SecurityContextBuilder;
import io.harness.security.dto.Principal;
import io.harness.security.dto.UserPrincipal;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Singleton
public class AccessControlClientImpl implements AccessControlClient {
  private final AccessControlHttpClient accessControlHttpClient;

  @Override
  public AccessCheckResponseDTO checkForAccess(
      String principal, PrincipalType principalType, List<PermissionCheckDTO> permissionCheckRequestList) {
    AccessCheckRequestDTO accessCheckRequestDTO =
        AccessCheckRequestDTO.builder()
            .principal(HUserPrincipal.builder().principalType(principalType).principalIdentifier(principal).build())
            .permissions(permissionCheckRequestList)
            .build();
    return NGRestUtils.getResponse(accessControlHttpClient.getAccessControlList(accessCheckRequestDTO));
  }

  @Override
  public AccessControlDTO checkForAccess(
      String principal, PrincipalType principalType, PermissionCheckDTO permissionCheckDTO) {
    AccessCheckResponseDTO responseDTO =
        checkForAccess(principal, principalType, Collections.singletonList(permissionCheckDTO));
    return responseDTO.getAccessControlList().get(0);
  }

  @Override
  public AccessCheckResponseDTO checkForAccess(List<PermissionCheckDTO> permissionCheckDTOList) {
    Principal principal = SecurityContextBuilder.getPrincipal();
    if (principal instanceof UserPrincipal) {
      UserPrincipal userPrincipal = (UserPrincipal) principal;
      return checkForAccess(userPrincipal.getName(), PrincipalType.USER, permissionCheckDTOList);
    }
    throw new UnsupportedOperationException("Only <User> principal type is supported as of now.");
  }

  @Override
  public AccessControlDTO checkForAccess(PermissionCheckDTO permissionCheckDTO) {
    return checkForAccess(Collections.singletonList(permissionCheckDTO)).getAccessControlList().get(0);
  }

  @Override
  public boolean hasAccess(String principal, PrincipalType principalType, PermissionCheckDTO permissionCheckDTO) {
    return ((HAccessControlDTO) checkForAccess(principal, principalType, permissionCheckDTO)).isAccessible();
  }

  @Override
  public boolean hasAccess(PermissionCheckDTO permissionCheckDTO) {
    return ((HAccessControlDTO) checkForAccess(permissionCheckDTO)).isAccessible();
  }

  @Override
  public void checkAccessOrThrow(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String resourceType, String resourceIdentifier, String permissionIdentifier) {
    if (!hasAccess(PermissionCheckDTO.builder()
                       .accountIdentifier(accountIdentifier)
                       .orgIdentifier(orgIdentifier)
                       .projectIdentifier(projectIdentifier)
                       .resourceType(resourceType)
                       .resourceIdentifier(resourceIdentifier)
                       .permission(permissionIdentifier)
                       .build())) {
      throw new AccessDeniedException("Insufficient permissions to perform this action", USER);
    }
  }
}
