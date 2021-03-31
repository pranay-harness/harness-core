package io.harness.connector.accesscontrol;

import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.clients.PermissionCheckDTO;
import io.harness.accesscontrol.clients.ResourceScope;
import io.harness.encryption.Scope;
import io.harness.encryption.ScopeHelper;

import com.google.inject.Inject;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ConnectorAccessControlHelper {
  @Inject private AccessControlClient accessControlClient;
  public static boolean checkForCreateAccess(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    Scope scope = ScopeHelper.getScope(accountIdentifier, orgIdentifier, projectIdentifier);
    String resourceType = getResourceTypeFromScope(scope);
    String resourceIdentifier =
        getResourceIdentifierFromScope(scope, accountIdentifier, orgIdentifier, projectIdentifier);

    return accessControlClient.hasAccess(PermissionCheckDTO.builder()
                                             .resourceType(resourceType)
                                             .resourceIdentifier(resourceIdentifier)
                                             .resourceScope(ResourceScope.builder()
                                                                .accountIdentifier(accountIdentifier)
                                                                .projectIdentifier(projectIdentifier)
                                                                .orgIdentifier(orgIdentifier)
                                                                .build())
                                             .permission(ConnectorsAccessControlPermissions.CREATE_CONNECTOR_PERMISSION)
                                             .build());
  }

  private static String getResourceIdentifierFromScope(
      Scope scope, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    if (scope == Scope.PROJECT) {
      return projectIdentifier;
    } else if (scope == Scope.ORG) {
      return orgIdentifier;
    } else if (scope == Scope.ACCOUNT) {
      return accountIdentifier;
    }
    return null;
  }

  private static String getResourceTypeFromScope(Scope scope) {
    if (scope == Scope.PROJECT) {
      return ResourceTypes.PROJECT;
    } else if (scope == Scope.ORG) {
      return ResourceTypes.ORGANIZATION;
    } else if (scope == Scope.ACCOUNT) {
      return ResourceTypes.ACCOUNT;
    }
    return null;
  }
}
