package io.harness.accesscontrol.clients;

import io.harness.accesscontrol.Principal;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;

@OwnedBy(HarnessTeam.PL)
public interface AccessControlClient {
  AccessCheckResponseDTO checkForAccess(Principal principal, List<PermissionCheckDTO> permissionCheckDTOList);

  AccessCheckResponseDTO checkForAccess(List<PermissionCheckDTO> permissionCheckDTOList);

  boolean hasAccess(Principal principal, ResourceScope resourceScope, Resource resource, String permission);

  boolean hasAccess(ResourceScope resourceScope, Resource resource, String permission);

  void checkForAccessOrThrow(ResourceScope resourceScope, Resource resource, String permission);

  void checkForAccessOrThrow(
      ResourceScope resourceScope, Resource resource, String permission, String exceptionMessage);

  void checkForAccessOrThrow(
      Principal principal, ResourceScope resourceScope, Resource resource, String permission, String exceptionMessage);
}
