package io.harness.rbac;

import static io.harness.accesscontrol.clients.ResourceScope.ResourceScopeBuilder;

import io.harness.accesscontrol.clients.PermissionCheckDTO;
import io.harness.accesscontrol.clients.ResourceScope;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;

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
}
