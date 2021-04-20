package io.harness.azure.client;

import io.harness.azure.model.AzureConfig;

import com.microsoft.azure.management.graphrbac.BuiltInRole;
import com.microsoft.azure.management.graphrbac.RoleAssignment;
import java.util.List;

public interface AzureAuthorizationClient {
  /**
   * Creates a role assignment.
   *
   * @param azureConfig AzureConfig
   * @param subscriptionId The role assignment scope.
   * @param objectId The principal or object ID.
   * @param roleAssignmentName The name of the role assignment to create. It can be any valid GUID.
   * @param builtInRole The role definition ID.
   * @return RoleAssignment
   */
  RoleAssignment roleAssignmentAtSubscriptionScope(AzureConfig azureConfig, String subscriptionId, String objectId,
      String roleAssignmentName, BuiltInRole builtInRole);

  /**
   * Get Role definition by scope and role name.
   *
   * @param azureConfig
   * @param scope
   * @param roleName
   * @return
   */
  List<RoleAssignment> getRoleDefinition(AzureConfig azureConfig, String scope, String roleName);
}
