/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

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
