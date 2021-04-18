package io.harness.audit.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;

import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.clients.PermissionCheckDTO;
import io.harness.accesscontrol.clients.ResourceScope;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.beans.ResourceScopeDTO;
import io.harness.exception.AccessDeniedException;

import com.google.inject.Inject;
import lombok.AllArgsConstructor;

@OwnedBy(PL)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class AuditPermissionValidator {
  @Inject private final AccessControlClient accessControlClient;
  private static final String ACCOUNT_RESOURCE_TYPE = "ACCOUNT";
  private static final String ORGANIZATION_RESOURCE_TYPE = "ORGANIZATION";
  private static final String PROJECT_RESOURCE_TYPE = "PROJECT";
  private static final String AUDIT_VIEW_PERMISSION = "core_audit_view";

  public void validate(String accountIdentifier, ResourceScopeDTO resourceScopeDTO) {
    checkPermissions(accountIdentifier, resourceScopeDTO);
  }

  private void checkPermissions(String accountIdentifier, ResourceScopeDTO resourceScopeDTO) {
    boolean hasAccess = hasAccountLevelPermission(accountIdentifier)
        || hasOrganizationLevelPermission(accountIdentifier, resourceScopeDTO.getOrgIdentifier())
        || hasProjectLevelPermission(
            accountIdentifier, resourceScopeDTO.getOrgIdentifier(), resourceScopeDTO.getProjectIdentifier());

    if (!hasAccess) {
      throw new AccessDeniedException(getAccessDeniedExceptionMessage(accountIdentifier,
                                          resourceScopeDTO.getOrgIdentifier(), resourceScopeDTO.getProjectIdentifier()),
          USER);
    }
  }

  private boolean hasAccountLevelPermission(String accountIdentifier) {
    if (isEmpty(accountIdentifier)) {
      return false;
    }
    return accessControlClient.hasAccess(PermissionCheckDTO.builder()
                                             .resourceScope(ResourceScope.of(accountIdentifier, null, null))
                                             .resourceType(ACCOUNT_RESOURCE_TYPE)
                                             .resourceIdentifier(accountIdentifier)
                                             .permission(AUDIT_VIEW_PERMISSION)
                                             .build());
  }

  private boolean hasOrganizationLevelPermission(String accountIdentifier, String orgIdentifier) {
    if (isEmpty(accountIdentifier) || isEmpty(orgIdentifier)) {
      return false;
    }
    return accessControlClient.hasAccess(PermissionCheckDTO.builder()
                                             .resourceScope(ResourceScope.of(accountIdentifier, orgIdentifier, null))
                                             .resourceType(ORGANIZATION_RESOURCE_TYPE)
                                             .resourceIdentifier(orgIdentifier)
                                             .permission(AUDIT_VIEW_PERMISSION)
                                             .build());
  }

  private boolean hasProjectLevelPermission(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    if (isEmpty(accountIdentifier) || isEmpty(orgIdentifier) || isEmpty(projectIdentifier)) {
      return false;
    }
    return accessControlClient.hasAccess(
        PermissionCheckDTO.builder()
            .resourceScope(ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier))
            .resourceType(PROJECT_RESOURCE_TYPE)
            .resourceIdentifier(projectIdentifier)
            .permission(AUDIT_VIEW_PERMISSION)
            .build());
  }

  private String getAccessDeniedExceptionMessage(
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    String resourceType = ACCOUNT_RESOURCE_TYPE;
    String resourceIdentifier = accountIdentifier;
    if (isNotEmpty(projectIdentifier)) {
      resourceType = PROJECT_RESOURCE_TYPE;
      resourceIdentifier = projectIdentifier;
    } else if (isNotEmpty(orgIdentifier)) {
      resourceType = ORGANIZATION_RESOURCE_TYPE;
      resourceIdentifier = orgIdentifier;
    }
    return String.format("You need %s permission on %s with identifier: %s to perform this action",
        AUDIT_VIEW_PERMISSION, resourceType, resourceIdentifier);
  }
}
