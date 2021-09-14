/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.rbac;

import static io.harness.accesscontrol.clients.ResourceScope.ResourceScopeBuilder;

import io.harness.accesscontrol.clients.PermissionCheckDTO;
import io.harness.accesscontrol.clients.ResourceScope;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.ng.core.environment.dto.EnvironmentResponse;
import io.harness.ng.core.service.dto.ServiceResponse;
import io.harness.pms.rbac.NGResourceType;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.PIPELINE)
@UtilityClass
public class CDNGRbacUtility {
  public PermissionCheckDTO getPermissionDTO(String accountId, String orgId, String projectId, String permission) {
    ResourceScopeBuilder scope = ResourceScope.builder();
    String resourceType = "ACCOUNT";
    String resouceIdentifier = accountId;
    scope.accountIdentifier(accountId);
    if (EmptyPredicate.isNotEmpty(orgId)) {
      resourceType = "ORGANIZATION";
      resouceIdentifier = orgId;
      if (EmptyPredicate.isNotEmpty(projectId)) {
        resouceIdentifier = projectId;
        resourceType = "PROJECT";
        scope.orgIdentifier(orgId);
      }
    }

    return PermissionCheckDTO.builder()
        .resourceType(resourceType)
        .resourceScope(scope.build())
        .resourceIdentifier(resouceIdentifier)
        .permission(permission)
        .build();
  }

  public PermissionCheckDTO serviceResponseToPermissionCheckDTO(ServiceResponse serviceResponse) {
    return PermissionCheckDTO.builder()
        .permission(CDNGRbacPermissions.SERVICE_RUNTIME_PERMISSION)
        .resourceIdentifier(serviceResponse.getService().getIdentifier())
        .resourceScope(ResourceScope.builder()
                           .accountIdentifier(serviceResponse.getService().getAccountId())
                           .orgIdentifier(serviceResponse.getService().getOrgIdentifier())
                           .projectIdentifier(serviceResponse.getService().getProjectIdentifier())
                           .build())
        .resourceType(NGResourceType.SERVICE)
        .build();
  }

  public PermissionCheckDTO environmentResponseToPermissionCheckDTO(EnvironmentResponse environmentResponse) {
    return PermissionCheckDTO.builder()
        .permission(CDNGRbacPermissions.ENVIRONMENT_RUNTIME_PERMISSION)
        .resourceIdentifier(environmentResponse.getEnvironment().getIdentifier())
        .resourceScope(ResourceScope.builder()
                           .accountIdentifier(environmentResponse.getEnvironment().getAccountId())
                           .orgIdentifier(environmentResponse.getEnvironment().getOrgIdentifier())
                           .projectIdentifier(environmentResponse.getEnvironment().getProjectIdentifier())
                           .build())
        .resourceType(NGResourceType.ENVIRONMENT)
        .build();
  }
}
