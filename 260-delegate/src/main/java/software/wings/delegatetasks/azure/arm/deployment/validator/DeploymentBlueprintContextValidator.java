/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.delegatetasks.azure.arm.deployment.validator;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.azure.model.AzureConstants;
import io.harness.exception.InvalidArgumentsException;

import software.wings.delegatetasks.azure.arm.deployment.context.DeploymentBlueprintContext;

public class DeploymentBlueprintContextValidator implements Validator<DeploymentBlueprintContext> {
  @Override
  public void validate(DeploymentBlueprintContext context) {
    if (context.getAzureConfig() == null) {
      throw new InvalidArgumentsException(AzureConstants.AZURE_CONFIG_BLANK_VALIDATION_MSG);
    }
    if (isBlank(context.getDefinitionResourceScope())) {
      throw new InvalidArgumentsException(AzureConstants.RESOURCE_SCOPE_BLANK_VALIDATION_MSG);
    }
    if (isBlank(context.getBlueprintName())) {
      throw new InvalidArgumentsException(AzureConstants.BLUEPRINT_NAME_BLANK_VALIDATION_MSG);
    }
    if (isBlank(context.getBlueprintJSON())) {
      throw new InvalidArgumentsException(AzureConstants.BLUEPRINT_JSON_BLANK_VALIDATION_MSG);
    }
    if (isBlank(context.getVersionId())) {
      throw new InvalidArgumentsException(AzureConstants.VERSION_ID_BLANK_VALIDATION_MSG);
    }
    if (isBlank(context.getAssignmentJSON())) {
      throw new InvalidArgumentsException(AzureConstants.ASSIGNMENT_JSON_BLANK_VALIDATION_MSG);
    }
    if (isBlank(context.getRoleAssignmentName())) {
      throw new InvalidArgumentsException(AzureConstants.ROLE_ASSIGNMENT_NAME_BLANK_VALIDATION_MSG);
    }
    if (isBlank(context.getAssignmentSubscriptionId())) {
      throw new InvalidArgumentsException(AzureConstants.ASSIGNMENT_SUBSCRIPTION_ID_BLANK_VALIDATION_MSG);
    }
    if (context.getAssignment() == null) {
      throw new InvalidArgumentsException(AzureConstants.ASSIGNMENT_BLANK_VALIDATION_MSG);
    }
  }
}
