package io.harness.accesscontrol.clients;

import static io.harness.exception.WingsException.USER;

import io.harness.accesscontrol.Principal;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.AccessDeniedException;
import io.harness.exception.UnexpectedException;
import io.harness.remote.client.NGRestUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.validation.executable.ValidateOnExecution;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Singleton
@ValidateOnExecution
@NoArgsConstructor
@OwnedBy(HarnessTeam.PL)
public class PrivilegedAccessControlClientImpl implements AccessControlClient {
  private AccessControlHttpClient accessControlHttpClient;
  private static final String ORGANIZATION_RESOURCE_TYPE = "ORGANIZATION";
  private static final String ACCOUNT_RESOURCE_TYPE = "ACCOUNT";
  private static final String PROJECT_RESOURCE_TYPE = "PROJECT";

  @Inject
  public PrivilegedAccessControlClientImpl(@Named("PRIVILEGED") AccessControlHttpClient accessControlHttpClient) {
    this.accessControlHttpClient = accessControlHttpClient;
  }

  @Override
  public AccessCheckResponseDTO checkForAccess(
      String principal, PrincipalType principalType, List<PermissionCheckDTO> permissionCheckRequestList) {
    AccessCheckRequestDTO accessCheckRequestDTO =
        AccessCheckRequestDTO.builder()
            .principal(Principal.builder().principalType(principalType).principalIdentifier(principal).build())
            .permissions(permissionCheckRequestList)
            .build();
    return NGRestUtils.getResponse(this.accessControlHttpClient.getAccessControlList(accessCheckRequestDTO));
  }

  @Override
  public AccessControlDTO checkForAccess(
      String principal, PrincipalType principalType, PermissionCheckDTO permissionCheckDTO) {
    return checkForAccess(principal, principalType, Collections.singletonList(permissionCheckDTO))
        .getAccessControlList()
        .get(0);
  }

  @Override
  public boolean hasAccess(String principal, PrincipalType principalType, PermissionCheckDTO permissionCheckDTO) {
    return checkForAccess(principal, principalType, permissionCheckDTO).isPermitted();
  }

  @Override
  public AccessCheckResponseDTO checkForAccess(List<PermissionCheckDTO> permissionCheckDTOList) {
    AccessCheckRequestDTO accessCheckRequestDTO =
        AccessCheckRequestDTO.builder().principal(null).permissions(permissionCheckDTOList).build();
    return NGRestUtils.getResponse(this.accessControlHttpClient.getAccessControlList(accessCheckRequestDTO));
  }

  @Override
  public AccessControlDTO checkForAccess(PermissionCheckDTO permissionCheckDTO) {
    return checkForAccess(Collections.singletonList(permissionCheckDTO)).getAccessControlList().get(0);
  }

  @Override
  public boolean hasAccess(PermissionCheckDTO permissionCheckDTO) {
    return checkForAccess(permissionCheckDTO).isPermitted();
  }

  private void checkForAccessOrThrow(PermissionCheckDTO permissionCheckDTO) {
    if (!hasAccess(permissionCheckDTO)) {
      throw new AccessDeniedException(
          String.format("You need %s permission on %s with identifier: %s to perform this action",
              permissionCheckDTO.getPermission(), permissionCheckDTO.getResourceType(),
              permissionCheckDTO.getResourceIdentifier()),
          USER);
    }
  }

  private PermissionCheckDTO getParentPermissionCheckDTO(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String permission) {
    PermissionCheckDTO.Builder builder = PermissionCheckDTO.builder().permission(permission);
    if (!StringUtils.isEmpty(projectIdentifier)) {
      return builder
          .resourceScope(
              ResourceScope.builder().accountIdentifier(accountIdentifier).orgIdentifier(orgIdentifier).build())
          .resourceType(PROJECT_RESOURCE_TYPE)
          .resourceIdentifier(projectIdentifier)
          .build();
    }
    if (!StringUtils.isEmpty(orgIdentifier)) {
      return builder.resourceScope(ResourceScope.builder().accountIdentifier(accountIdentifier).build())
          .resourceType(ORGANIZATION_RESOURCE_TYPE)
          .resourceIdentifier(orgIdentifier)
          .build();
    }
    return builder.resourceScope(ResourceScope.builder().build())
        .resourceType(ACCOUNT_RESOURCE_TYPE)
        .resourceIdentifier(accountIdentifier)
        .build();
  }

  @Override
  public void checkForAccessOrThrow(
      @Nullable ResourceScope resourceScope, @Nullable Resource resource, @NotNull String permission) {
    if (resource == null || StringUtils.isEmpty(resource.getResourceIdentifier())) {
      if (resourceScope == null) {
        throw new UnexpectedException("Both resource scope and resource cannot be null simultaneously");
      }
      checkForAccessOrThrow(getParentPermissionCheckDTO(resourceScope.getAccountIdentifier(),
          resourceScope.getOrgIdentifier(), resourceScope.getProjectIdentifier(), permission));
    } else {
      checkForAccessOrThrow(PermissionCheckDTO.builder()
                                .permission(permission)
                                .resourceType(resource.getResourceType())
                                .resourceIdentifier(resource.getResourceIdentifier())
                                .resourceScope(resourceScope)
                                .build());
    }
  }
}
