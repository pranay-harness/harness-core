package io.harness.accesscontrol.clients;

import static io.harness.exception.WingsException.USER;

import io.harness.accesscontrol.HPrincipal;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.exception.AccessDeniedException;
import io.harness.remote.client.NGRestUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;
import java.util.List;
import javax.validation.executable.ValidateOnExecution;
import lombok.AllArgsConstructor;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Singleton
@ValidateOnExecution
public class AccessControlClientImpl implements AccessControlClient {
  private final AccessControlHttpClient accessControlHttpClient;

  @Override
  public HAccessCheckResponseDTO checkForAccess(
      String principal, PrincipalType principalType, List<PermissionCheckDTO> permissionCheckRequestList) {
    HAccessCheckRequestDTO accessCheckRequestDTO =
        HAccessCheckRequestDTO.builder()
            .principal(HPrincipal.builder().principalType(principalType).principalIdentifier(principal).build())
            .permissions(permissionCheckRequestList)
            .build();
    return NGRestUtils.getResponse(this.accessControlHttpClient.getAccessControlList(accessCheckRequestDTO));
  }

  @Override
  public HAccessControlDTO checkForAccess(
      String principal, PrincipalType principalType, PermissionCheckDTO permissionCheckDTO) {
    return checkForAccess(principal, principalType, Collections.singletonList(permissionCheckDTO))
        .getAccessControlList()
        .get(0);
  }

  @Override
  public boolean hasAccess(String principal, PrincipalType principalType, PermissionCheckDTO permissionCheckDTO) {
    return checkForAccess(principal, principalType, permissionCheckDTO).isAccessible();
  }

  @Override
  public HAccessCheckResponseDTO checkForAccess(List<PermissionCheckDTO> permissionCheckDTOList) {
    HAccessCheckRequestDTO accessCheckRequestDTO =
        HAccessCheckRequestDTO.builder().principal(null).permissions(permissionCheckDTOList).build();
    return NGRestUtils.getResponse(this.accessControlHttpClient.getAccessControlList(accessCheckRequestDTO));
  }

  @Override
  public HAccessControlDTO checkForAccess(PermissionCheckDTO permissionCheckDTO) {
    return checkForAccess(Collections.singletonList(permissionCheckDTO)).getAccessControlList().get(0);
  }

  @Override
  public boolean hasAccess(PermissionCheckDTO permissionCheckDTO) {
    return checkForAccess(permissionCheckDTO).isAccessible();
  }

  @Override
  public void checkForAccessOrThrow(PermissionCheckDTO permissionCheckDTO) {
    if (!hasAccess(permissionCheckDTO)) {
      throw new AccessDeniedException("Insufficient permissions to perform this action", USER);
    }
  }
}
