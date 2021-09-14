/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.accesscontrol.clients;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.stream.Collectors;

@OwnedBy(HarnessTeam.PL)
public class NoOpAccessControlClientImpl extends AbstractAccessControlClient {
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
