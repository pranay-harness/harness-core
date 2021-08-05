package io.harness.accesscontrol.clients;

import static io.harness.accesscontrol.clients.AccessControlClientUtils.checkPreconditions;
import static io.harness.exception.WingsException.USER;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.security.SecurityContextBuilder;

import java.util.stream.Collectors;

@OwnedBy(HarnessTeam.PL)
public class NonPrivilegedNoOpAccessControlClientImpl extends AbstractAccessControlClient {
  @Override
  protected AccessCheckResponseDTO checkForAccess(AccessCheckRequestDTO accessCheckRequestDTO) {
    boolean preconditionsValid =
        checkPreconditions(SecurityContextBuilder.getPrincipal(), accessCheckRequestDTO.getPrincipal());
    if (!preconditionsValid) {
      throw new InvalidRequestException(
          "Missing principal in context or User doesn't have permission to check access for a different principal",
          USER);
    }
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