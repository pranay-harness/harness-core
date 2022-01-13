/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.azure.impl;

import static io.harness.azure.model.AzureConstants.OBJECT_ID_NAME_BLANK_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.ROLE_ASSIGNMENT_NAME_BLANK_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.SUBSCRIPTION_ID_NULL_VALIDATION_MSG;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.azure.AzureClient;
import io.harness.azure.client.AzureAuthorizationClient;
import io.harness.azure.model.AzureConfig;

import com.google.inject.Singleton;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.graphrbac.BuiltInRole;
import com.microsoft.azure.management.graphrbac.RoleAssignment;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class AzureAuthorizationClientImpl extends AzureClient implements AzureAuthorizationClient {
  public RoleAssignment roleAssignmentAtSubscriptionScope(final AzureConfig azureConfig, final String subscriptionId,
      final String objectId, final String roleAssignmentName, final BuiltInRole builtInRole) {
    if (isBlank(subscriptionId)) {
      throw new IllegalArgumentException(SUBSCRIPTION_ID_NULL_VALIDATION_MSG);
    }
    if (isBlank(objectId)) {
      throw new IllegalArgumentException(OBJECT_ID_NAME_BLANK_VALIDATION_MSG);
    }
    if (isBlank(roleAssignmentName)) {
      throw new IllegalArgumentException(ROLE_ASSIGNMENT_NAME_BLANK_VALIDATION_MSG);
    }

    Azure azure = getAzureClient(azureConfig, subscriptionId);

    log.debug(
        "Start role assignment at subscription scope, subscriptionId: {}, objectId: {}, roleAssignmentName: {}, builtInRole: {}",
        subscriptionId, objectId, roleAssignmentName, builtInRole.toString());
    return azure.accessManagement()
        .roleAssignments()
        .define(roleAssignmentName)
        .forObjectId(objectId)
        .withBuiltInRole(builtInRole)
        .withSubscriptionScope(subscriptionId)
        .create();
  }

  public List<RoleAssignment> getRoleDefinition(
      final AzureConfig azureConfig, final String scope, final String roleName) {
    if (isBlank(scope)) {
      throw new IllegalArgumentException("Parameter scope cannot be null or empty");
    }
    if (isBlank(roleName)) {
      throw new IllegalArgumentException("Parameter roleName cannot be null or empty");
    }

    Azure azure = getAzureClient(azureConfig, scope);

    log.debug("Start getting role definition at scope: {}, subscriptionId: {}, roleName: {}", scope, roleName);
    PagedList<RoleAssignment> roleAssignments = azure.accessManagement().roleAssignments().listByScope(scope);
    return new ArrayList<>(roleAssignments.size());
  }
}
