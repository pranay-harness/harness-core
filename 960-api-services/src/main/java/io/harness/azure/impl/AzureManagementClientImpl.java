package io.harness.azure.impl;

import static io.harness.azure.model.AzureConstants.DEPLOYMENT_DOES_NOT_EXIST_MANAGEMENT_GROUP;
import static io.harness.azure.model.AzureConstants.DEPLOYMENT_DOES_NOT_EXIST_RESOURCE_GROUP;
import static io.harness.azure.model.AzureConstants.DEPLOYMENT_DOES_NOT_EXIST_SUBSCRIPTION;
import static io.harness.azure.model.AzureConstants.DEPLOYMENT_DOES_NOT_EXIST_TENANT;
import static io.harness.azure.model.AzureConstants.DEPLOYMENT_NAME_BLANK_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.LOCATION_BLANK_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.LOCATION_SET_AT_RESOURCE_GROUP_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.MANAGEMENT_GROUP_ID_BLANK_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.RESOURCE_GROUP_NAME_NULL_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.SUBSCRIPTION_ID_NULL_VALIDATION_MSG;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.azure.AzureClient;
import io.harness.azure.client.AzureManagementClient;
import io.harness.azure.context.ARMDeploymentSteadyStateContext;
import io.harness.azure.context.AzureClientContext;
import io.harness.azure.model.ARMScopeType;
import io.harness.azure.model.AzureARMRGTemplateExportOptions;
import io.harness.azure.model.AzureARMTemplate;
import io.harness.azure.model.AzureConfig;
import io.harness.azure.model.management.ManagementGroupInfo;
import io.harness.azure.model.management.ManagementGroupListResult;
import io.harness.exception.AzureClientException;
import io.harness.serializer.JsonUtils;

import com.google.inject.Singleton;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.resources.Deployment;
import com.microsoft.azure.management.resources.DeploymentMode;
import com.microsoft.azure.management.resources.DeploymentProperties;
import com.microsoft.azure.management.resources.Location;
import com.microsoft.azure.management.resources.ResourceGroupExportTemplateOptions;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.implementation.DeploymentExtendedInner;
import com.microsoft.azure.management.resources.implementation.DeploymentInner;
import com.microsoft.azure.management.resources.implementation.DeploymentOperationInner;
import com.microsoft.azure.management.resources.implementation.DeploymentOperationsInner;
import com.microsoft.azure.management.resources.implementation.DeploymentValidateResultInner;
import com.microsoft.azure.management.resources.implementation.DeploymentsInner;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import retrofit2.Response;

@Singleton
@Slf4j
public class AzureManagementClientImpl extends AzureClient implements AzureManagementClient {
  @Override
  public List<String> listLocationsBySubscriptionId(AzureConfig azureConfig, String subscriptionId) {
    Azure azure = isBlank(subscriptionId) ? getAzureClientWithDefaultSubscription(azureConfig)
                                          : getAzureClient(azureConfig, subscriptionId);

    log.debug("Start listing location by subscriptionId {}", subscriptionId);
    Subscription subscription =
        isBlank(subscriptionId) ? azure.getCurrentSubscription() : azure.subscriptions().getById(subscriptionId);

    return subscription != null
        ? subscription.listLocations().stream().map(Location::displayName).collect(Collectors.toList())
        : emptyList();
  }

  @Override
  public List<ManagementGroupInfo> listManagementGroups(AzureConfig azureConfig) {
    log.debug("Start listing management groups, client id: {}", azureConfig.getClientId());
    Response<ManagementGroupListResult> response;
    try {
      response = getAzureManagementRestClient(azureConfig.getAzureEnvironmentType())
                     .listManagementGroups(getAzureBearerAuthToken(azureConfig))
                     .execute();
    } catch (IOException e) {
      String errorMessage = "Error occurred while listing management groups";
      throw new AzureClientException(errorMessage, e);
    }

    if (response.isSuccessful()) {
      ManagementGroupListResult managementGroupListResult = response.body();
      return managementGroupListResult.getValue();
    } else {
      handleAzureErrorResponse(response.errorBody(), response.raw());
    }
    return Collections.emptyList();
  }

  @Override
  public String exportResourceGroupTemplateJSON(
      AzureClientContext context, AzureARMRGTemplateExportOptions rgExportOptions) {
    String resourceGroupName = context.getResourceGroupName();
    String subscriptionId = context.getSubscriptionId();
    ResourceGroupExportTemplateOptions resourceGroupExportTemplateOptions =
        ResourceGroupExportTemplateOptions.valueOf(rgExportOptions.name());

    if (isBlank(resourceGroupName)) {
      throw new IllegalArgumentException(RESOURCE_GROUP_NAME_NULL_VALIDATION_MSG);
    }
    if (isBlank(subscriptionId)) {
      throw new IllegalArgumentException(SUBSCRIPTION_ID_NULL_VALIDATION_MSG);
    }

    Azure azure = getAzureClientByContext(context);
    log.debug(
        "Start exporting template at resource group, subscriptionId: {},  resourceGroupName: {}, resourceGroupExportTemplateOptions: {}",
        subscriptionId, resourceGroupName, resourceGroupExportTemplateOptions);

    return azure.resourceGroups()
        .getByName(resourceGroupName)
        .exportTemplate(resourceGroupExportTemplateOptions)
        .templateJson();
  }

  @Override
  public Deployment getDeploymentAtResourceGroup(AzureClientContext context, String deploymentName) {
    String resourceGroupName = context.getResourceGroupName();
    String subscriptionId = context.getSubscriptionId();

    if (isBlank(resourceGroupName)) {
      throw new IllegalArgumentException(RESOURCE_GROUP_NAME_NULL_VALIDATION_MSG);
    }
    if (isBlank(subscriptionId)) {
      throw new IllegalArgumentException(SUBSCRIPTION_ID_NULL_VALIDATION_MSG);
    }
    if (isBlank(deploymentName)) {
      throw new IllegalArgumentException(DEPLOYMENT_NAME_BLANK_VALIDATION_MSG);
    }

    Azure azure = getAzureClientByContext(context);

    log.debug(
        "Start getting deployment info at resource group scope, deploymentName: {}, subscriptionId: {},  resourceGroupName: {}",
        deploymentName, subscriptionId, resourceGroupName);
    return azure.deployments().getByResourceGroup(resourceGroupName, deploymentName);
  }

  @Override
  public DeploymentValidateResultInner validateDeploymentAtResourceGroupScope(
      AzureClientContext context, AzureARMTemplate template) {
    String resourceGroupName = context.getResourceGroupName();
    String subscriptionId = context.getSubscriptionId();
    String deploymentName = template.getDeploymentName();

    if (isBlank(resourceGroupName)) {
      throw new IllegalArgumentException(RESOURCE_GROUP_NAME_NULL_VALIDATION_MSG);
    }
    if (isBlank(subscriptionId)) {
      throw new IllegalArgumentException(SUBSCRIPTION_ID_NULL_VALIDATION_MSG);
    }
    if (isBlank(deploymentName)) {
      throw new IllegalArgumentException(DEPLOYMENT_NAME_BLANK_VALIDATION_MSG);
    }
    if (isNotBlank(template.getLocation())) {
      throw new IllegalArgumentException(LOCATION_SET_AT_RESOURCE_GROUP_VALIDATION_MSG);
    }

    Azure azure = getAzureClientByContext(context);

    DeploymentProperties properties = getDeploymentProperties(template);
    DeploymentInner parameters = new DeploymentInner();
    parameters.withProperties(properties);

    log.debug(
        "Start deployment validation at resource group scope, deploymentName: {}, subscriptionId: {},  resourceGroupName: {}",
        deploymentName, subscriptionId, resourceGroupName);
    return azure.deployments().manager().inner().deployments().validate(resourceGroupName, deploymentName, parameters);
  }

  @Override
  public Deployment deployAtResourceGroupScope(AzureClientContext context, AzureARMTemplate template) {
    String resourceGroupName = context.getResourceGroupName();
    String subscriptionId = context.getSubscriptionId();
    String deploymentName = template.getDeploymentName();
    DeploymentMode deploymentMode = DeploymentMode.fromString(template.getDeploymentMode().name());

    if (isBlank(resourceGroupName)) {
      throw new IllegalArgumentException(RESOURCE_GROUP_NAME_NULL_VALIDATION_MSG);
    }
    if (isBlank(subscriptionId)) {
      throw new IllegalArgumentException(SUBSCRIPTION_ID_NULL_VALIDATION_MSG);
    }
    if (isBlank(deploymentName)) {
      throw new IllegalArgumentException(DEPLOYMENT_NAME_BLANK_VALIDATION_MSG);
    }
    if (isNotBlank(template.getLocation())) {
      throw new IllegalArgumentException(LOCATION_SET_AT_RESOURCE_GROUP_VALIDATION_MSG);
    }

    Azure azure = getAzureClientByContext(context);
    try {
      log.debug(
          "Start deploying at resource group scope, deploymentName: {}, subscriptionId: {},  resourceGroupName: {}, deploymentMode: {}",
          deploymentName, subscriptionId, resourceGroupName, deploymentMode);
      return azure.deployments()
          .define(deploymentName)
          .withExistingResourceGroup(resourceGroupName)
          .withTemplate(template.getTemplateJSON())
          .withParameters(template.getParametersJSON())
          .withMode(deploymentMode)
          .beginCreate();
    } catch (IOException e) {
      String errorMessage = format(
          "Error occurred while deploying at resource group scope, deploymentName: %s, subscriptionId: %s,  resourceGroupName: %s, deploymentMode: %s",
          deploymentName, subscriptionId, resourceGroupName, deploymentMode);
      throw new AzureClientException(errorMessage, e);
    }
  }

  @Override
  public DeploymentExtendedInner getDeploymentAtSubscriptionScope(
      AzureConfig azureConfig, String subscriptionId, String deploymentName) {
    if (isBlank(subscriptionId)) {
      throw new IllegalArgumentException(SUBSCRIPTION_ID_NULL_VALIDATION_MSG);
    }
    if (isBlank(deploymentName)) {
      throw new IllegalArgumentException(DEPLOYMENT_NAME_BLANK_VALIDATION_MSG);
    }

    Azure azure = getAzureClient(azureConfig, subscriptionId);

    log.debug("Start getting deployment info at subscription scope, deploymentName: {}, subscriptionId: {}",
        deploymentName, subscriptionId);
    return azure.deployments().manager().inner().deployments().getAtSubscriptionScope(deploymentName);
  }

  @Override
  public DeploymentValidateResultInner validateDeploymentAtSubscriptionScope(
      AzureConfig azureConfig, String subscriptionId, AzureARMTemplate template) {
    String deploymentName = template.getDeploymentName();
    if (isBlank(subscriptionId)) {
      throw new IllegalArgumentException(SUBSCRIPTION_ID_NULL_VALIDATION_MSG);
    }
    if (isBlank(deploymentName)) {
      throw new IllegalArgumentException(DEPLOYMENT_NAME_BLANK_VALIDATION_MSG);
    }
    if (isBlank(template.getLocation())) {
      throw new IllegalArgumentException(LOCATION_BLANK_VALIDATION_MSG);
    }

    Azure azure = getAzureClient(azureConfig, subscriptionId);

    DeploymentProperties properties = getDeploymentProperties(template);
    DeploymentInner parameters = new DeploymentInner();
    parameters.withLocation(template.getLocation());
    parameters.withProperties(properties);

    log.debug("Start deployment validation at subscription scope, deploymentName: {}, subscriptionId: {}",
        deploymentName, subscriptionId);
    return azure.deployments().manager().inner().deployments().validateAtSubscriptionScope(deploymentName, parameters);
  }

  @Override
  public DeploymentExtendedInner deployAtSubscriptionScope(
      AzureConfig azureConfig, String subscriptionId, AzureARMTemplate template) {
    String deploymentName = template.getDeploymentName();
    DeploymentMode deploymentMode = DeploymentMode.fromString(template.getDeploymentMode().name());

    if (isBlank(subscriptionId)) {
      throw new IllegalArgumentException(SUBSCRIPTION_ID_NULL_VALIDATION_MSG);
    }
    if (isBlank(deploymentName)) {
      throw new IllegalArgumentException(DEPLOYMENT_NAME_BLANK_VALIDATION_MSG);
    }
    if (isBlank(template.getLocation())) {
      throw new IllegalArgumentException(LOCATION_BLANK_VALIDATION_MSG);
    }

    Azure azure = getAzureClient(azureConfig, subscriptionId);

    DeploymentProperties properties = getDeploymentProperties(template);
    DeploymentInner parameters = new DeploymentInner();
    parameters.withLocation(template.getLocation());
    parameters.withProperties(properties);

    log.debug("Start deploying at subscription scope, deploymentName: {}, subscriptionId: {}, deploymentMode: {}",
        deploymentName, subscriptionId, deploymentMode);
    return azure.deployments().manager().inner().deployments().beginCreateOrUpdateAtSubscriptionScope(
        deploymentName, parameters);
  }

  @Override
  public DeploymentExtendedInner getDeploymentAtManagementScope(
      AzureConfig azureConfig, String groupId, String deploymentName) {
    if (isBlank(deploymentName)) {
      throw new IllegalArgumentException(DEPLOYMENT_NAME_BLANK_VALIDATION_MSG);
    }
    if (isBlank(groupId)) {
      throw new IllegalArgumentException(MANAGEMENT_GROUP_ID_BLANK_VALIDATION_MSG);
    }

    Azure azure = getAzureClientWithDefaultSubscription(azureConfig);

    log.debug("Start getting deployment info at management group scope, deploymentName: {}, groupId: {}",
        deploymentName, groupId);
    return azure.deployments().manager().inner().deployments().getAtManagementGroupScope(groupId, deploymentName);
  }

  @Override
  public DeploymentValidateResultInner validateDeploymentAtManagementGroupScope(
      AzureConfig azureConfig, String groupId, AzureARMTemplate template) {
    String deploymentName = template.getDeploymentName();

    if (isBlank(deploymentName)) {
      throw new IllegalArgumentException(DEPLOYMENT_NAME_BLANK_VALIDATION_MSG);
    }
    if (isBlank(groupId)) {
      throw new IllegalArgumentException(MANAGEMENT_GROUP_ID_BLANK_VALIDATION_MSG);
    }
    if (isBlank(template.getLocation())) {
      throw new IllegalArgumentException(LOCATION_BLANK_VALIDATION_MSG);
    }

    Azure azure = getAzureClientWithDefaultSubscription(azureConfig);

    DeploymentProperties properties = getDeploymentProperties(template);
    DeploymentInner parameters = new DeploymentInner();
    parameters.withLocation(template.getLocation());
    parameters.withProperties(properties);

    log.debug("Start deployment validation at management group scope, deploymentName: {}, groupId: {}", deploymentName,
        groupId);
    return azure.deployments().manager().inner().deployments().validateAtManagementGroupScope(
        groupId, deploymentName, parameters);
  }

  @Override
  public DeploymentExtendedInner deployAtManagementGroupScope(
      AzureConfig azureConfig, String groupId, AzureARMTemplate template) {
    String deploymentName = template.getDeploymentName();
    DeploymentMode deploymentMode = DeploymentMode.fromString(template.getDeploymentMode().name());

    if (isBlank(deploymentName)) {
      throw new IllegalArgumentException(DEPLOYMENT_NAME_BLANK_VALIDATION_MSG);
    }
    if (isBlank(groupId)) {
      throw new IllegalArgumentException(MANAGEMENT_GROUP_ID_BLANK_VALIDATION_MSG);
    }
    if (isBlank(template.getLocation())) {
      throw new IllegalArgumentException(LOCATION_BLANK_VALIDATION_MSG);
    }

    Azure azure = getAzureClientWithDefaultSubscription(azureConfig);

    DeploymentProperties properties = getDeploymentProperties(template);
    DeploymentInner parameters = new DeploymentInner();
    parameters.withLocation(template.getLocation());
    parameters.withProperties(properties);

    log.debug("Start deploying at management group scope, deploymentName: {}, groupId: {}, deploymentMode: {}",
        deploymentName, groupId, deploymentMode);
    return azure.deployments().manager().inner().deployments().beginCreateOrUpdateAtManagementGroupScope(
        groupId, deploymentName, parameters);
  }

  @Override
  public DeploymentExtendedInner getDeploymentAtTenantScope(AzureConfig azureConfig, String deploymentName) {
    if (isBlank(deploymentName)) {
      throw new IllegalArgumentException(DEPLOYMENT_NAME_BLANK_VALIDATION_MSG);
    }

    Azure azure = getAzureClientWithDefaultSubscription(azureConfig);

    log.debug("Start getting deployment info at tenant scope, deploymentName: {}", deploymentName);
    return azure.deployments().manager().inner().deployments().getAtTenantScope(deploymentName);
  }

  @Override
  public DeploymentValidateResultInner validateDeploymentAtTenantScope(
      AzureConfig azureConfig, AzureARMTemplate template) {
    String deploymentName = template.getDeploymentName();

    if (isBlank(deploymentName)) {
      throw new IllegalArgumentException(DEPLOYMENT_NAME_BLANK_VALIDATION_MSG);
    }
    if (isBlank(template.getLocation())) {
      throw new IllegalArgumentException(LOCATION_BLANK_VALIDATION_MSG);
    }

    Azure azure = getAzureClientWithDefaultSubscription(azureConfig);

    DeploymentProperties properties = getDeploymentProperties(template);
    DeploymentInner parameters = new DeploymentInner();
    parameters.withLocation(template.getLocation());
    parameters.withProperties(properties);

    log.debug("Start deployment validation at tenant scope, deploymentName: {}, tenantId: {}", deploymentName,
        azureConfig.getTenantId());
    return azure.deployments().manager().inner().deployments().validateAtTenantScope(deploymentName, parameters);
  }

  @Override
  public DeploymentExtendedInner deployAtTenantScope(AzureConfig azureConfig, AzureARMTemplate template) {
    String deploymentName = template.getDeploymentName();
    DeploymentMode deploymentMode = DeploymentMode.fromString(template.getDeploymentMode().name());

    if (isBlank(deploymentName)) {
      throw new IllegalArgumentException(DEPLOYMENT_NAME_BLANK_VALIDATION_MSG);
    }
    if (isBlank(template.getLocation())) {
      throw new IllegalArgumentException(LOCATION_BLANK_VALIDATION_MSG);
    }

    Azure azure = getAzureClientWithDefaultSubscription(azureConfig);

    DeploymentProperties properties = getDeploymentProperties(template);
    DeploymentInner parameters = new DeploymentInner();
    parameters.withLocation(template.getLocation());
    parameters.withProperties(properties);

    log.debug("Start deploying at tenant scope, deploymentName: {}, tenantId: {}, deploymentMode: {}", deploymentName,
        azureConfig.getTenantId(), deploymentMode);
    return azure.deployments().manager().inner().deployments().beginCreateOrUpdateAtTenantScope(
        deploymentName, parameters);
  }

  @Override
  public String getARMDeploymentStatus(ARMDeploymentSteadyStateContext context) {
    Azure azureClient = getAzureClient(context);
    DeploymentExtendedInner extendedInner = getDeploymentExtenderInner(context, azureClient);
    return extendedInner.properties().provisioningState();
  }

  private DeploymentExtendedInner getDeploymentExtenderInner(
      ARMDeploymentSteadyStateContext context, Azure azureClient) {
    DeploymentExtendedInner extendedInner;
    String deploymentName = context.getDeploymentName();
    DeploymentsInner deploymentsInner = azureClient.deployments().manager().inner().deployments();

    switch (context.getScopeType()) {
      case RESOURCE_GROUP:
        String resourceGroup = context.getResourceGroup();
        if (!deploymentsInner.checkExistence(resourceGroup, deploymentName)) {
          throw new IllegalArgumentException(
              String.format(DEPLOYMENT_DOES_NOT_EXIST_RESOURCE_GROUP, deploymentName, resourceGroup));
        }

        extendedInner = deploymentsInner.getByResourceGroup(resourceGroup, deploymentName);
        break;

      case SUBSCRIPTION:
        if (!deploymentsInner.checkExistenceAtSubscriptionScope(deploymentName)) {
          throw new IllegalArgumentException(
              String.format(DEPLOYMENT_DOES_NOT_EXIST_SUBSCRIPTION, deploymentName, context.getSubscriptionId()));
        }
        extendedInner = deploymentsInner.getAtSubscriptionScope(deploymentName);
        break;

      case MANAGEMENT_GROUP:
        String managementGroupId = context.getManagementGroupId();
        if (!deploymentsInner.checkExistenceAtManagementGroupScope(managementGroupId, deploymentName)) {
          throw new IllegalArgumentException(
              String.format(DEPLOYMENT_DOES_NOT_EXIST_MANAGEMENT_GROUP, deploymentName, managementGroupId));
        }

        extendedInner = deploymentsInner.getAtManagementGroupScope(managementGroupId, deploymentName);
        break;

      case TENANT:
        if (!deploymentsInner.checkExistenceAtTenantScope(deploymentName)) {
          throw new IllegalArgumentException(
              String.format(DEPLOYMENT_DOES_NOT_EXIST_TENANT, deploymentName, context.getTenantId()));
        }
        extendedInner = deploymentsInner.getAtTenantScope(deploymentName);
        break;

      default:
        throw new IllegalStateException("Unexpected value: " + context.getScopeType());
    }
    return extendedInner;
  }

  private Azure getAzureClient(ARMDeploymentSteadyStateContext context) {
    ARMScopeType scopeType = context.getScopeType();
    if (ARMScopeType.RESOURCE_GROUP == scopeType || ARMScopeType.SUBSCRIPTION == scopeType) {
      return getAzureClient(context.getAzureConfig(), context.getSubscriptionId());
    }
    return getAzureClientWithDefaultSubscription(context.getAzureConfig());
  }

  @Override
  public PagedList<DeploymentOperationInner> getDeploymentOperations(ARMDeploymentSteadyStateContext context) {
    PagedList<DeploymentOperationInner> operationInners;
    String deploymentName = context.getDeploymentName();
    Azure azureClient = getAzureClient(context);
    DeploymentOperationsInner deploymentOperationsInner =
        azureClient.deployments().manager().inner().deploymentOperations();

    switch (context.getScopeType()) {
      case RESOURCE_GROUP:
        operationInners = deploymentOperationsInner.listByResourceGroup(context.getResourceGroup(), deploymentName);
        break;

      case SUBSCRIPTION:
        operationInners = deploymentOperationsInner.listAtSubscriptionScope(deploymentName);
        break;

      case MANAGEMENT_GROUP:
        operationInners =
            deploymentOperationsInner.listAtManagementGroupScope(context.getManagementGroupId(), deploymentName);
        break;

      case TENANT:
        operationInners = deploymentOperationsInner.listAtTenantScope(deploymentName);
        break;

      default:
        throw new IllegalStateException("Unexpected value: " + context.getScopeType());
    }
    return operationInners;
  }

  @Override
  public String getARMDeploymentOutputs(ARMDeploymentSteadyStateContext context) {
    String deploymentName = context.getDeploymentName();
    Azure azureClient = getAzureClient(context);
    DeploymentsInner deployments = azureClient.deployments().manager().inner().deployments();
    DeploymentExtendedInner extendedInner;
    switch (context.getScopeType()) {
      case RESOURCE_GROUP:
        extendedInner = deployments.getByResourceGroup(context.getResourceGroup(), deploymentName);
        break;
      case SUBSCRIPTION:
        extendedInner = deployments.getAtSubscriptionScope(deploymentName);
        break;
      case MANAGEMENT_GROUP:
        extendedInner = deployments.getAtManagementGroupScope(context.getManagementGroupId(), deploymentName);
        break;
      case TENANT:
        extendedInner = deployments.getAtTenantScope(deploymentName);
        break;
      default:
        throw new IllegalStateException("Unexpected value: " + context.getScopeType());
    }
    Object outputs = extendedInner.properties().outputs();
    return outputs != null ? JsonUtils.asJson(outputs) : "";
  }

  @NotNull
  private DeploymentProperties getDeploymentProperties(AzureARMTemplate template) {
    DeploymentMode deploymentMode = DeploymentMode.fromString(template.getDeploymentMode().name());
    DeploymentProperties properties = new DeploymentProperties();
    properties.withMode(deploymentMode);
    properties.withTemplate(JsonUtils.readTree(template.getTemplateJSON()));
    properties.withParameters(JsonUtils.readTree(template.getParametersJSON()));
    return properties;
  }
}
