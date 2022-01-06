/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.testframework.restutils;

import io.harness.azure.model.VirtualMachineScaleSetData;
import io.harness.beans.PageResponse;
import io.harness.delegate.task.azure.appservice.webapp.response.DeploymentSlotData;
import io.harness.exception.EmptyRestResponseException;
import io.harness.rest.RestResponse;
import io.harness.testframework.framework.Setup;

import software.wings.infra.InfrastructureDefinition;
import software.wings.service.impl.aws.model.AwsAsgGetRunningCountData;

import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.core.GenericType;
import lombok.experimental.UtilityClass;

@UtilityClass
public class InfrastructureDefinitionRestUtils {
  public static InfrastructureDefinition save(String bearerToken, InfrastructureDefinition infrastructureDefinition) {
    GenericType<RestResponse<InfrastructureDefinition>> infraDefinitionType =
        new GenericType<RestResponse<InfrastructureDefinition>>() {};
    RestResponse<InfrastructureDefinition> response = Setup.portal()
                                                          .auth()
                                                          .oauth2(bearerToken)
                                                          .queryParam("appId", infrastructureDefinition.getAppId())
                                                          .queryParam("envId", infrastructureDefinition.getEnvId())
                                                          .body(infrastructureDefinition, ObjectMapperType.JACKSON_2)
                                                          .contentType(ContentType.JSON)
                                                          .post("/infrastructure-definitions")
                                                          .as(infraDefinitionType.getType());

    if (response.getResource() == null) {
      throw new EmptyRestResponseException(
          "/infrastructure-definitions", String.valueOf(response.getResponseMessages()));
    }
    return response.getResource();
  }

  public static InfrastructureDefinition update(String bearerToken, InfrastructureDefinition infrastructureDefinition) {
    GenericType<RestResponse<InfrastructureDefinition>> infraDefinitionType =
        new GenericType<RestResponse<InfrastructureDefinition>>() {};
    RestResponse<InfrastructureDefinition> response =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam("appId", infrastructureDefinition.getAppId())
            .queryParam("envId", infrastructureDefinition.getEnvId())
            .body(infrastructureDefinition, ObjectMapperType.JACKSON_2)
            .contentType(ContentType.JSON)
            .put("/infrastructure-definitions/" + infrastructureDefinition.getUuid())
            .as(infraDefinitionType.getType());

    if (response.getResource() == null) {
      throw new EmptyRestResponseException(
          "/infrastructure-definitions", String.valueOf(response.getResponseMessages()));
    }
    return response.getResource();
  }

  public static InfrastructureDefinition get(
      String bearerToken, String accountId, String appId, String envId, String infraDefinitionId) {
    GenericType<RestResponse<InfrastructureDefinition>> infraDefinitionType =
        new GenericType<RestResponse<InfrastructureDefinition>>() {};
    RestResponse<InfrastructureDefinition> restResponse = Setup.portal()
                                                              .auth()
                                                              .oauth2(bearerToken)
                                                              .contentType(ContentType.JSON)
                                                              .queryParam("appId", appId)
                                                              .queryParam("envId", envId)
                                                              .queryParam("routingId", accountId)
                                                              .get("/infrastructure-definitions/" + infraDefinitionId)
                                                              .as(infraDefinitionType.getType());
    if (restResponse.getResource() == null) {
      throw new EmptyRestResponseException(
          "/infrastructure-definitions/" + infraDefinitionId, String.valueOf(restResponse.getResponseMessages()));
    }
    return restResponse.getResource();
  }

  public static RestResponse delete(
      String bearerToken, String accountId, String appId, String envId, String infraDefinitionId) {
    GenericType<RestResponse> restResponseGenericType = new GenericType<RestResponse>() {};
    return Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .contentType(ContentType.JSON)
        .queryParam("appId", appId)
        .queryParam("envId", envId)
        .queryParam("routingId", accountId)
        .delete("/infrastructure-definitions/" + infraDefinitionId)
        .as(restResponseGenericType.getType());
  }

  public static List<String> listHosts(String bearerToken, String appId, String envId, String infraDefinitionId) {
    GenericType<RestResponse<List<String>>> restResponseGenericType = new GenericType<RestResponse<List<String>>>() {};
    RestResponse<List<String>> restResponse = Setup.portal()
                                                  .auth()
                                                  .oauth2(bearerToken)
                                                  .contentType(ContentType.JSON)
                                                  .queryParam("appId", appId)
                                                  .queryParam("envId", envId)
                                                  .get("/infrastructure-definitions/" + infraDefinitionId + "/hosts")
                                                  .as(restResponseGenericType.getType());
    if (restResponse.getResource() == null) {
      throw new EmptyRestResponseException("/infrastructure-definitions/" + infraDefinitionId + "/hosts",
          String.valueOf(restResponse.getResponseMessages()));
    }
    return restResponse.getResource();
  }

  public static List<String> listInfraDefinitionByService(
      String bearerToken, String accountId, String appId, String serviceId, String envId) {
    GenericType<RestResponse<PageResponse<InfrastructureDefinition>>> restResponseGenericType =
        new GenericType<RestResponse<PageResponse<InfrastructureDefinition>>>() {};
    RestResponse<PageResponse<InfrastructureDefinition>> restResponse = Setup.portal()
                                                                            .auth()
                                                                            .oauth2(bearerToken)
                                                                            .contentType(ContentType.JSON)
                                                                            .queryParam("routingId", accountId)
                                                                            .queryParam("appId", appId)
                                                                            .queryParam("envId", envId)
                                                                            .queryParam("serviceId", serviceId)
                                                                            .get("/infrastructure-definitions/")
                                                                            .as(restResponseGenericType.getType());
    if (restResponse.getResource() == null) {
      throw new EmptyRestResponseException(
          "/infrastructure-definitions", String.valueOf(restResponse.getResponseMessages()));
    }
    return restResponse.getResource()
        .getResponse()
        .stream()
        .map(InfrastructureDefinition::getUuid)
        .collect(Collectors.toList());
  }

  public Map<String, String> listVirtualMachineScaleSets(
      String bearerToken, String appId, String subscriptionId, String resourceGroup, String cloudProviderId) {
    GenericType<RestResponse<Map<String, String>>> restResponseGenericType =
        new GenericType<RestResponse<Map<String, String>>>() {};
    String scaleSetsPath =
        String.format("/infrastructure-definitions/compute-providers/%s/vm-scale-sets", cloudProviderId);
    RestResponse<Map<String, String>> restResponse = Setup.portal()
                                                         .auth()
                                                         .oauth2(bearerToken)
                                                         .contentType(ContentType.JSON)
                                                         .queryParam("appId", appId)
                                                         .queryParam("subscriptionId", subscriptionId)
                                                         .queryParam("resourceGroupName", resourceGroup)
                                                         .get(scaleSetsPath)
                                                         .as(restResponseGenericType.getType());
    if (restResponse.getResource() == null) {
      throw new EmptyRestResponseException(scaleSetsPath, String.valueOf(restResponse.getResponseMessages()));
    }

    return restResponse.getResource();
  }

  public List<String> getAzureLoadBalancerBackendPools(
      String bearerToken, String appId, String infraDefinitionId, String loadBalancerName) {
    GenericType<RestResponse<List<String>>> restResponseGenericType = new GenericType<RestResponse<List<String>>>() {};
    String backendPoolsPath = String.format(
        "/infrastructure-definitions/%s/azure-load-balancers/%s/backend-pools", infraDefinitionId, loadBalancerName);
    RestResponse<List<String>> restResponse = Setup.portal()
                                                  .auth()
                                                  .oauth2(bearerToken)
                                                  .contentType(ContentType.JSON)
                                                  .queryParam("appId", appId)
                                                  .get(backendPoolsPath)
                                                  .as(restResponseGenericType.getType());
    if (restResponse.getResource() == null) {
      throw new EmptyRestResponseException(backendPoolsPath, String.valueOf(restResponse.getResponseMessages()));
    }

    return restResponse.getResource();
  }

  public List<String> listLoadBalancers(String bearerToken, String appId, String infraDefinitionId) {
    GenericType<RestResponse<List<String>>> restResponseGenericType = new GenericType<RestResponse<List<String>>>() {};
    String loadBalancersPath = String.format("/infrastructure-definitions/%s/azure-load-balancers", infraDefinitionId);
    RestResponse<List<String>> restResponse = Setup.portal()
                                                  .auth()
                                                  .oauth2(bearerToken)
                                                  .contentType(ContentType.JSON)
                                                  .queryParam("appId", appId)
                                                  .get(loadBalancersPath)
                                                  .as(restResponseGenericType.getType());
    if (restResponse.getResource() == null) {
      throw new EmptyRestResponseException(loadBalancersPath, String.valueOf(restResponse.getResponseMessages()));
    }
    return restResponse.getResource();
  }

  public List<String> listWebApps(String bearerToken, String appId, String infraDefinitionId) {
    GenericType<RestResponse<List<String>>> restResponseGenericType = new GenericType<RestResponse<List<String>>>() {};
    String appServicesPath = String.format("/infrastructure-definitions/%s/azure-app-services", infraDefinitionId);
    RestResponse<List<String>> restResponse = Setup.portal()
                                                  .auth()
                                                  .oauth2(bearerToken)
                                                  .contentType(ContentType.JSON)
                                                  .queryParam("appId", appId)
                                                  .queryParam("appType", "WEB_APP")
                                                  .get(appServicesPath)
                                                  .as(restResponseGenericType.getType());
    if (restResponse.getResource() == null) {
      throw new EmptyRestResponseException(appServicesPath, String.valueOf(restResponse.getResponseMessages()));
    }
    return restResponse.getResource();
  }

  public List<DeploymentSlotData> listSlots(
      String bearerToken, String appId, String appService, String infraDefinitionId) {
    GenericType<RestResponse<List<DeploymentSlotData>>> restResponseGenericType =
        new GenericType<RestResponse<List<DeploymentSlotData>>>() {};
    String appServiceSlotsPath =
        String.format("/infrastructure-definitions/%s/azure-app-services/%s/slots", infraDefinitionId, appService);
    RestResponse<List<DeploymentSlotData>> restResponse = Setup.portal()
                                                              .auth()
                                                              .oauth2(bearerToken)
                                                              .contentType(ContentType.JSON)
                                                              .queryParam("appId", appId)
                                                              .queryParam("appType", "WEB_APP")
                                                              .get(appServiceSlotsPath)
                                                              .as(restResponseGenericType.getType());
    if (restResponse.getResource() == null) {
      throw new EmptyRestResponseException(appServiceSlotsPath, String.valueOf(restResponse.getResponseMessages()));
    }

    return restResponse.getResource();
  }

  public VirtualMachineScaleSetData getAzureVirtualMachineScaleSetByName(String bearerToken, String appId,
      String subscriptionId, String resourceGroup, String cloudProviderId, String vmScaleSetName) {
    GenericType<RestResponse<VirtualMachineScaleSetData>> restResponseGenericType =
        new GenericType<RestResponse<VirtualMachineScaleSetData>>() {};
    String scaleSetsPath = String.format(
        "/infrastructure-definitions/compute-providers/%s/vm-scale-sets/%s", cloudProviderId, vmScaleSetName);
    RestResponse<VirtualMachineScaleSetData> restResponse = Setup.portal()
                                                                .auth()
                                                                .oauth2(bearerToken)
                                                                .contentType(ContentType.JSON)
                                                                .queryParam("appId", appId)
                                                                .queryParam("subscriptionId", subscriptionId)
                                                                .queryParam("resourceGroupName", resourceGroup)
                                                                .get(scaleSetsPath)
                                                                .as(restResponseGenericType.getType());
    if (restResponse.getResource() == null) {
      throw new EmptyRestResponseException(scaleSetsPath, String.valueOf(restResponse.getResponseMessages()));
    }

    return restResponse.getResource();
  }

  public Map<String, String> getAzureSubscriptions(String bearerToken, String appId, String cloudProviderId) {
    GenericType<RestResponse<Map<String, String>>> restResponseGenericType =
        new GenericType<RestResponse<Map<String, String>>>() {};
    String subscriptionsPath =
        String.format("/infrastructure-definitions/compute-providers/%s/subscriptions", cloudProviderId);
    RestResponse<Map<String, String>> restResponse = Setup.portal()
                                                         .auth()
                                                         .oauth2(bearerToken)
                                                         .contentType(ContentType.JSON)
                                                         .queryParam("appId", appId)
                                                         .get(subscriptionsPath)
                                                         .as(restResponseGenericType.getType());
    if (restResponse.getResource() == null) {
      throw new EmptyRestResponseException(subscriptionsPath, String.valueOf(restResponse.getResponseMessages()));
    }

    return restResponse.getResource();
  }

  public List<String> getAzureResourceGroupsNames(
      String bearerToken, String appId, String cloudProviderId, String subscriptionId) {
    GenericType<RestResponse<List<String>>> restResponseGenericType = new GenericType<RestResponse<List<String>>>() {};
    String subscriptionsPath =
        String.format("/infrastructure-definitions/compute-providers/%s/resource-groups", cloudProviderId);
    RestResponse<List<String>> restResponse = Setup.portal()
                                                  .auth()
                                                  .oauth2(bearerToken)
                                                  .contentType(ContentType.JSON)
                                                  .queryParam("appId", appId)
                                                  .queryParam("subscriptionId", subscriptionId)
                                                  .get(subscriptionsPath)
                                                  .as(restResponseGenericType.getType());
    if (restResponse.getResource() == null) {
      throw new EmptyRestResponseException(subscriptionsPath, String.valueOf(restResponse.getResponseMessages()));
    }

    return restResponse.getResource();
  }

  public List<String> getSubscriptionLocations(
      String bearerToken, String appId, String cloudProviderId, String subscriptionId) {
    GenericType<RestResponse<List<String>>> restResponseGenericType = new GenericType<RestResponse<List<String>>>() {};
    String subscriptionsPath = String.format(
        "/infrastructure-definitions/compute-providers/%s/subscriptions/%s/locations", cloudProviderId, subscriptionId);
    RestResponse<List<String>> restResponse = Setup.portal()
                                                  .auth()
                                                  .oauth2(bearerToken)
                                                  .contentType(ContentType.JSON)
                                                  .queryParam("appId", appId)
                                                  .get(subscriptionsPath)
                                                  .as(restResponseGenericType.getType());

    if (restResponse.getResource() == null) {
      throw new EmptyRestResponseException(subscriptionsPath, String.valueOf(restResponse.getResponseMessages()));
    }

    return restResponse.getResource();
  }

  public List<String> getCloudProviderLocations(String bearerToken, String appId, String cloudProviderId) {
    GenericType<RestResponse<List<String>>> restResponseGenericType = new GenericType<RestResponse<List<String>>>() {};
    String subscriptionsPath =
        String.format("/infrastructure-definitions/compute-providers/%s/locations", cloudProviderId);
    RestResponse<List<String>> restResponse = Setup.portal()
                                                  .auth()
                                                  .oauth2(bearerToken)
                                                  .contentType(ContentType.JSON)
                                                  .queryParam("appId", appId)
                                                  .get(subscriptionsPath)
                                                  .as(restResponseGenericType.getType());

    if (restResponse.getResource() == null) {
      throw new EmptyRestResponseException(subscriptionsPath, String.valueOf(restResponse.getResponseMessages()));
    }

    return restResponse.getResource();
  }

  public Map<String, String> getManagementGroups(String bearerToken, String appId, String cloudProviderId) {
    GenericType<RestResponse<Map<String, String>>> restResponseGenericType =
        new GenericType<RestResponse<Map<String, String>>>() {};
    String subscriptionsPath =
        String.format("/infrastructure-definitions/compute-providers/%s/management-groups", cloudProviderId);
    RestResponse<Map<String, String>> restResponse = Setup.portal()
                                                         .auth()
                                                         .oauth2(bearerToken)
                                                         .contentType(ContentType.JSON)
                                                         .queryParam("appId", appId)
                                                         .get(subscriptionsPath)
                                                         .as(restResponseGenericType.getType());

    if (restResponse.getResource() == null) {
      throw new EmptyRestResponseException(subscriptionsPath, String.valueOf(restResponse.getResponseMessages()));
    }

    return restResponse.getResource();
  }

  public static List<String> listAutoScalingGroups(
      String bearerToken, String accountId, String appId, String cloudProviderId, String region) {
    GenericType<RestResponse<List<String>>> restResponseGenericType = new GenericType<RestResponse<List<String>>>() {};
    RestResponse<List<String>> restResponse =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .contentType(ContentType.JSON)
            .queryParam("routingId", accountId)
            .queryParam("appId", appId)
            .queryParams("region", region)
            .get("infrastructure-mappings/compute-providers/" + cloudProviderId + "/auto-scaling"
                + "-groups/")
            .as(restResponseGenericType.getType());
    if (restResponse.getResource() == null) {
      throw new EmptyRestResponseException("infrastructure-mappings/compute-providers/" + cloudProviderId
              + "/auto-scaling"
              + "-groups/",
          String.valueOf(restResponse.getResponseMessages()));
    }
    return restResponse.getResource();
  }

  public static AwsAsgGetRunningCountData amiRunningInstances(
      String bearerToken, String accountId, String appId, String serviceId, String infraDefinitionId) {
    GenericType<RestResponse<AwsAsgGetRunningCountData>> restResponseGenericType =
        new GenericType<RestResponse<AwsAsgGetRunningCountData>>() {};
    RestResponse<AwsAsgGetRunningCountData> restResponse =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .contentType(ContentType.JSON)
            .queryParam("routingId", accountId)
            .queryParam("appId", appId)
            .queryParam("serviceId", serviceId)
            .get("/infrastructure-definitions/" + infraDefinitionId + "/ami/runningcount")
            .as(restResponseGenericType.getType());
    if (restResponse.getResource() == null) {
      throw new EmptyRestResponseException("/infrastructure-definitions/" + infraDefinitionId + "/ami/runningcount",
          String.valueOf(restResponse.getResponseMessages()));
    }
    return restResponse.getResource();
  }

  public static Set<String> listAzureTags(
      String bearerToken, String appId, String subscriptionId, String computeProviderId) {
    GenericType<RestResponse<Set<String>>> restResponseGenericType = new GenericType<RestResponse<Set<String>>>() {};
    RestResponse<Set<String>> restResponse =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .contentType(ContentType.JSON)
            .queryParam("appId", appId)
            .queryParam("subscriptionId", subscriptionId)
            .queryParam("computeProviderId", computeProviderId)
            .get("infrastructure-mappings/compute-providers/" + computeProviderId + "/azure-tags")
            .as(restResponseGenericType.getType());
    if (restResponse.getResource() == null) {
      throw new EmptyRestResponseException(
          "infrastructure-mappings/compute-providers/" + computeProviderId + "/azure-tags",
          String.valueOf(restResponse.getResponseMessages()));
    }
    return restResponse.getResource();
  }

  public static Map<String, String> listAzureSubscriptions(
      String bearerToken, String accountId, String cloudProviderId) {
    GenericType<RestResponse<Map<String, String>>> restResponseGenericType =
        new GenericType<RestResponse<Map<String, String>>>() {};
    RestResponse<Map<String, String>> restResponse = Setup.portal()
                                                         .auth()
                                                         .oauth2(bearerToken)
                                                         .contentType(ContentType.JSON)
                                                         .queryParams("accountId", accountId)
                                                         .queryParam("cloudProviderId", cloudProviderId)
                                                         .get("azure/subscriptions")
                                                         .as(restResponseGenericType.getType());
    if (restResponse.getResource() == null) {
      throw new EmptyRestResponseException("azure/subscriptions", String.valueOf(restResponse.getResponseMessages()));
    }
    return restResponse.getResource();
  }

  public static Set<String> listAzureResources(
      String bearerToken, String appId, String subscriptionId, String computeProviderId) {
    GenericType<RestResponse<Set<String>>> restResponseGenericType = new GenericType<RestResponse<Set<String>>>() {};
    RestResponse<Set<String>> restResponse =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .contentType(ContentType.JSON)
            .queryParam("appId", appId)
            .queryParam("subscriptionId", subscriptionId)
            .queryParam("computeProviderId", computeProviderId)
            .get("infrastructure-mappings/compute-providers/" + computeProviderId + "/resource-groups")
            .as(restResponseGenericType.getType());
    if (restResponse.getResource() == null) {
      throw new EmptyRestResponseException(
          "infrastructure-mappings/compute-providers/" + computeProviderId + "/resource-groups",
          String.valueOf(restResponse.getResponseMessages()));
    }
    return restResponse.getResource();
  }

  public static List<String> listAwsClassicLoadBalancers(
      String bearerToken, String appId, String cloudProviderId, String region) {
    GenericType<RestResponse<List<String>>> restResponseGenericType = new GenericType<RestResponse<List<String>>>() {};
    RestResponse<List<String>> restResponse =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .contentType(ContentType.JSON)
            .queryParam("appId", appId)
            .queryParams("region", region)
            .get("infrastructure-mappings/compute-providers/" + cloudProviderId + "/classic-load-balancers/")
            .as(restResponseGenericType.getType());
    if (restResponse.getResource() == null) {
      throw new EmptyRestResponseException(
          "infrastructure-mappings/compute-providers/" + cloudProviderId + "/classic-load-balancers",
          String.valueOf(restResponse.getResponseMessages()));
    }
    return restResponse.getResource();
  }

  public static List<String> listAwsAlbTargetGroups(
      String bearerToken, String appId, String cloudProviderId, String region) {
    GenericType<RestResponse<List<String>>> restResponseGenericType = new GenericType<RestResponse<List<String>>>() {};
    RestResponse<List<String>> restResponse =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .contentType(ContentType.JSON)
            .queryParam("appId", appId)
            .queryParams("region", region)
            .get("infrastructure-mappings/compute-providers/" + cloudProviderId + "/classic-load-balancers/")
            .as(restResponseGenericType.getType());
    if (restResponse.getResource() == null) {
      throw new EmptyRestResponseException(
          "infrastructure-mappings/compute-providers/" + cloudProviderId + "/classic-load-balancers",
          String.valueOf(restResponse.getResponseMessages()));
    }
    return restResponse.getResource();
  }
}
