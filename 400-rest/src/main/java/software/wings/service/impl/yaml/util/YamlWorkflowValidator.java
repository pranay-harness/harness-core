/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.util;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;

import static software.wings.sm.StateType.ARM_CREATE_RESOURCE;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.OwnedBy;
import io.harness.azure.model.ARMResourceType;
import io.harness.azure.model.ARMScopeType;
import io.harness.exception.InvalidArgumentsException;

import software.wings.beans.ARMSourceType;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(CDP)
@UtilityClass
public class YamlWorkflowValidator {
  public static final String PRE_DEPLOYMENT_STEPS = "preDeploymentSteps";
  public static final String TYPE = "type";
  public static final String PROPERTIES_KEY = "properties";
  public static final String SOURCE_TYPE = "sourceType";
  public static final String SCOPE_TYPE = "scopeType";
  public static final String RESOURCE_TYPE = "resourceType";
  public static final String INLINE_PARAMETERS_EXPRESSION = "inlineParametersExpression";
  public static final String PARAMETERS_GIT_FILE_CONFIG = "parametersGitFileConfig";
  public static final String PROVISIONER_KEY = "provisioner";
  public static final String SUBSCRIPTION_EXPRESSION = "subscriptionExpression";
  public static final String RESOURCE_GROUP_EXPRESSION = "resourceGroupExpression";
  public static final String LOCATION_EXPRESSION = "locationExpression";
  public static final String MANAGEMENT_GROUP_EXPRESSION = "managementGroupExpression";
  public static final String TIMEOUT_EXPRESSION = "timeoutExpression";
  public static final String CLOUD_PROVIDER_ID = "cloudProviderId";
  public static final String USE_BRANCH = "useBranch";
  public static final String CONNECTOR_ID = "connectorId";
  public static final String BRANCH = "branch";
  public static final String COMMIT_ID = "commitId";

  public void validateWorkflowPreDeploymentSteps(Map<String, Object> yamlLoad) {
    validateARMCreateResourcePreDeploymentStep(yamlLoad);
  }

  private void validateARMCreateResourcePreDeploymentStep(Map<String, Object> yamlLoad) {
    if (yamlLoad.containsKey(PRE_DEPLOYMENT_STEPS)) {
      List<LinkedHashMap<String, Object>> armProvisioners =
          ((List<LinkedHashMap<String, Object>>) yamlLoad.get(PRE_DEPLOYMENT_STEPS))
              .stream()
              .filter(map -> map.containsKey(TYPE) && map.get(TYPE).equals(ARM_CREATE_RESOURCE.name()))
              .collect(toList());

      armProvisioners.forEach(armCreateResourceStep
          -> validateARMCreateResourcePreDeploymentStepProperties(getValue(armCreateResourceStep, PROPERTIES_KEY)));
    }
  }

  private void validateARMCreateResourcePreDeploymentStepProperties(Map<String, Object> properties) {
    if (isEmpty(properties)) {
      return;
    }

    validateARMCreateResourceProvisionerProperties(properties);

    ARMResourceType resourceType = getResourceType(properties);
    validateARMCreateResourceARMProperties(properties, resourceType);
    validateARMCreateResourceBlueprintProperties(properties, resourceType);
  }

  private void validateARMCreateResourceProvisionerProperties(Map<String, Object> properties) {
    Map<String, Object> provisioner = getValue(properties, PROVISIONER_KEY);
    String sourceType = (String) provisioner.get(SOURCE_TYPE);
    if (ARMSourceType.getSourceType(sourceType) == null) {
      throw new InvalidArgumentsException(format("Unrecognized ARM source type, value: %s", sourceType), USER);
    }

    ARMScopeType.fromString((String) provisioner.get(SCOPE_TYPE));
    ARMResourceType.fromString((String) provisioner.get(RESOURCE_TYPE));
  }

  private ARMResourceType getResourceType(Map<String, Object> properties) {
    Map<String, Object> provisioner = getValue(properties, PROVISIONER_KEY);
    return ARMResourceType.fromString((String) provisioner.get(RESOURCE_TYPE));
  }

  private void validateARMCreateResourceARMProperties(Map<String, Object> properties, ARMResourceType resourceType) {
    if (ARMResourceType.ARM != resourceType) {
      return;
    }

    validateARMCreateResourceARMOverviewProperties(properties);
    validateARMCreateResourceARMParametersProperties(properties);
  }

  private void validateARMCreateResourceARMOverviewProperties(Map<String, Object> properties) {
    validateCloudProviderId(properties);
    validateTimeoutExpression(properties);

    Map<String, Object> provisioner = getValue(properties, PROVISIONER_KEY);
    validateARMCreateResourceARMResourceTypes(properties, provisioner);
  }

  private void validateARMCreateResourceARMResourceTypes(
      Map<String, Object> properties, Map<String, Object> provisioner) {
    ARMScopeType armScopeType = ARMScopeType.fromString((String) provisioner.get(SCOPE_TYPE));

    validateARMCreateResourceARMResourceGroupScope(properties, armScopeType);
    validateARMCreateResourceARMSubscriptionScope(properties, armScopeType);
    validateARMCreateResourceARMManagementGroupScope(properties, armScopeType);
    validateARMCreateResourceARMTenantScope(properties, armScopeType);
  }

  private void validateARMCreateResourceARMResourceGroupScope(Map<String, Object> properties, ARMScopeType scopeType) {
    if (ARMScopeType.RESOURCE_GROUP != scopeType) {
      return;
    }

    String subscriptionExpression =
        safeCastToJava(properties.get(SUBSCRIPTION_EXPRESSION), String.class, SUBSCRIPTION_EXPRESSION);
    if (isBlank(subscriptionExpression)) {
      throw new InvalidArgumentsException(
          "Parameter subscriptionExpression cannot be null or empty for resource group scope", USER);
    }

    String resourceGroupExpression =
        safeCastToJava(properties.get(RESOURCE_GROUP_EXPRESSION), String.class, RESOURCE_GROUP_EXPRESSION);
    if (isBlank(resourceGroupExpression)) {
      throw new InvalidArgumentsException(
          "Parameter resourceGroupExpression cannot be null or empty for resource group scope", USER);
    }
  }

  private void validateARMCreateResourceARMSubscriptionScope(Map<String, Object> properties, ARMScopeType scopeType) {
    if (ARMScopeType.SUBSCRIPTION != scopeType) {
      return;
    }

    String subscriptionExpression =
        safeCastToJava(properties.get(SUBSCRIPTION_EXPRESSION), String.class, SUBSCRIPTION_EXPRESSION);
    if (isBlank(subscriptionExpression)) {
      throw new InvalidArgumentsException(
          "Parameter subscriptionExpression cannot be null or empty for subscription scope", USER);
    }

    String locationExpression = safeCastToJava(properties.get(LOCATION_EXPRESSION), String.class, LOCATION_EXPRESSION);
    if (isBlank(locationExpression)) {
      throw new InvalidArgumentsException(
          "Parameter locationExpression cannot be null or empty for subscription scope", USER);
    }
  }

  private void validateARMCreateResourceARMManagementGroupScope(
      Map<String, Object> properties, ARMScopeType scopeType) {
    if (ARMScopeType.MANAGEMENT_GROUP != scopeType) {
      return;
    }

    String managementGroupExpression =
        safeCastToJava(properties.get(MANAGEMENT_GROUP_EXPRESSION), String.class, MANAGEMENT_GROUP_EXPRESSION);
    if (isBlank(managementGroupExpression)) {
      throw new InvalidArgumentsException(
          "Parameter managementGroupExpression cannot be null or empty for management scope", USER);
    }

    String locationExpression = safeCastToJava(properties.get(LOCATION_EXPRESSION), String.class, LOCATION_EXPRESSION);
    if (isBlank(locationExpression)) {
      throw new InvalidArgumentsException(
          "Parameter locationExpression cannot be null or empty for management scope", USER);
    }
  }

  private void validateARMCreateResourceARMTenantScope(Map<String, Object> properties, ARMScopeType scopeType) {
    if (ARMScopeType.TENANT != scopeType) {
      return;
    }

    String locationExpression = safeCastToJava(properties.get(LOCATION_EXPRESSION), String.class, LOCATION_EXPRESSION);
    if (isBlank(locationExpression)) {
      throw new InvalidArgumentsException(
          "Parameter locationExpression cannot be null or empty for tenant scope", USER);
    }
  }

  private void validateARMCreateResourceARMParametersProperties(Map<String, Object> properties) {
    String sourceType = safeCastToJava(properties.get(SOURCE_TYPE), String.class, SOURCE_TYPE);
    if (!Arrays.asList("inline", "remote").contains(sourceType)) {
      throw new InvalidArgumentsException(
          format("Unrecognized ARM parameters source type, value: %s", sourceType), USER);
    }

    String inlineParametersExpression =
        safeCastToJava(properties.get(INLINE_PARAMETERS_EXPRESSION), String.class, INLINE_PARAMETERS_EXPRESSION);
    if ("inline".equals(sourceType) && isBlank(inlineParametersExpression)) {
      throw new InvalidArgumentsException(
          "Parameter inlineParametersExpression cannot be null or empty for inline source type", USER);
    }

    Map<String, Object> parametersGitFileConfig = getValue(properties, PARAMETERS_GIT_FILE_CONFIG);
    if ("remote".equals(sourceType) && isEmpty(parametersGitFileConfig)) {
      throw new InvalidArgumentsException(
          "Parameter parametersGitFileConfig cannot be null or empty for remote source type", USER);
    }
    validateParametersGitFileConfig(parametersGitFileConfig);
  }

  private void validateParametersGitFileConfig(Map<String, Object> parametersGitFileConfig) {
    if (isEmpty(parametersGitFileConfig)) {
      return;
    }

    Boolean useBranch = safeCastToJava(parametersGitFileConfig.get(USE_BRANCH), Boolean.class, USE_BRANCH);
    String connectorId = safeCastToJava(parametersGitFileConfig.get(CONNECTOR_ID), String.class, CONNECTOR_ID);
    if (isBlank(connectorId)) {
      throw new InvalidArgumentsException("Connector id cannot be empty.", USER);
    }

    String branch = safeCastToJava(parametersGitFileConfig.get(BRANCH), String.class, BRANCH);
    if (Boolean.TRUE.equals(useBranch) && isBlank(branch)) {
      throw new InvalidArgumentsException("Branch cannot be empty if useBranch is selected.", USER);
    }

    String commitId = safeCastToJava(parametersGitFileConfig.get(COMMIT_ID), String.class, COMMIT_ID);
    if (!Boolean.TRUE.equals(useBranch) && isBlank(commitId)) {
      throw new InvalidArgumentsException("CommitId cannot be empty if useBranch is not selected.", USER);
    }
  }

  private void validateARMCreateResourceBlueprintProperties(
      Map<String, Object> properties, ARMResourceType resourceType) {
    if (ARMResourceType.BLUEPRINT != resourceType) {
      return;
    }

    validateCloudProviderId(properties);
    validateTimeoutExpression(properties);
  }

  private void validateCloudProviderId(Map<String, Object> properties) {
    String cloudProviderId = safeCastToJava(properties.get(CLOUD_PROVIDER_ID), String.class, CLOUD_PROVIDER_ID);
    if (isBlank(cloudProviderId)) {
      throw new InvalidArgumentsException("Parameter cloudProviderId cannot be null or empty", USER);
    }
  }

  private void validateTimeoutExpression(Map<String, Object> properties) {
    Integer timeoutExpression = safeCastToJava(properties.get(TIMEOUT_EXPRESSION), Integer.class, TIMEOUT_EXPRESSION);
    if (timeoutExpression == null) {
      throw new InvalidArgumentsException("Parameter timeoutExpression cannot be null or empty", USER);
    }
  }

  private <T> T safeCastToJava(Object objToCast, Class<T> clazz, String propertyName) {
    if (objToCast == null) {
      return null;
    }

    try {
      return clazz.cast(objToCast);
    } catch (ClassCastException ex) {
      throw new InvalidArgumentsException(
          format("Unable to cast [%s] to required type [%s]", propertyName, clazz.getTypeName()), USER);
    }
  }

  private Map<String, Object> getValue(Map<String, Object> map, final String key) {
    if (isBlank(key) || isEmpty(map)) {
      return Collections.emptyMap();
    }

    return map.entrySet()
        .stream()
        .filter(entry -> key.equals(entry.getKey()))
        .filter(entry -> entry.getValue() instanceof Map)
        .map(entry -> (Map<String, Object>) entry.getValue())
        .findFirst()
        .orElse(Collections.emptyMap());
  }
}
