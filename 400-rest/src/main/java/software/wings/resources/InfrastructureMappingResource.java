/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import static software.wings.security.PermissionAttribute.Action.READ;
import static software.wings.security.PermissionAttribute.Action.UPDATE;
import static software.wings.security.PermissionAttribute.PermissionType.ENV;
import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;
import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.delegate.task.aws.AwsElbListener;
import io.harness.exception.InvalidRequestException;
import io.harness.rest.RestResponse;

import software.wings.api.DeploymentType;
import software.wings.beans.HostValidationRequest;
import software.wings.beans.HostValidationResponse;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.aws.model.AwsAsgGetRunningCountData;
import software.wings.service.impl.aws.model.AwsRoute53HostedZoneData;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.settings.SettingVariableTypes;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by anubhaw on 1/10/17.
 */
@Api("infrastructure-mappings")
@Path("infrastructure-mappings")
@Produces("application/json")
@Consumes("application/json")
@Scope(APPLICATION)
public class InfrastructureMappingResource {
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private AppService appService;
  @Inject private ServiceResourceService serviceResourceService;

  @GET
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ)
  public RestResponse<PageResponse<InfrastructureMapping>> list(
      @BeanParam PageRequest<InfrastructureMapping> pageRequest) {
    return new RestResponse<>(infrastructureMappingService.list(pageRequest));
  }

  @POST
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = UPDATE)
  public RestResponse<InfrastructureMapping> save(@QueryParam("appId") String appId, @QueryParam("envId") String envId,
      InfrastructureMapping infrastructureMapping) {
    infrastructureMapping.setAppId(appId);
    infrastructureMapping.setEnvId(envId);
    infrastructureMapping.setAccountId(appService.getAccountIdByAppId(appId));
    populateDeploymentTypeIfRequired(appId, infrastructureMapping);
    return new RestResponse<>(infrastructureMappingService.save(infrastructureMapping, null));
  }

  @GET
  @Path("{infraMappingId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ)
  public RestResponse<InfrastructureMapping> get(@QueryParam("appId") String appId, @QueryParam("envId") String envId,
      @PathParam("infraMappingId") String infraMappingId) {
    return new RestResponse<>(infrastructureMappingService.get(appId, infraMappingId));
  }

  @GET
  @Path("compute-providers/{computeProviderId}/hosts")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ)
  public RestResponse<List<String>> listComputeProviderHosts(@QueryParam("appId") String appId,
      @QueryParam("envId") String envId, @QueryParam("serviceId") String serviceId,
      @PathParam("computeProviderId") String computeProviderId) {
    return new RestResponse<>(
        infrastructureMappingService.listComputeProviderHostDisplayNames(appId, envId, serviceId, computeProviderId));
  }

  @GET
  @Path("{infraMappingId}/hosts")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ, skipAuth = true)
  public RestResponse<List<String>> listHosts(
      @QueryParam("appId") String appId, @PathParam("infraMappingId") String infraMappingId) {
    return new RestResponse<>(infrastructureMappingService.listHostDisplayNames(appId, infraMappingId, null));
  }

  @GET
  @Path("{infraMappingId}/containers")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ, skipAuth = true)
  public RestResponse<String> getRunningContainerCount(@QueryParam("appId") String appId,
      @QueryParam("serviceNameExpr") String serviceNameExpr, @PathParam("infraMappingId") String infraMappingId) {
    return new RestResponse<>(
        infrastructureMappingService.getContainerRunningInstances(appId, infraMappingId, serviceNameExpr));
  }

  @GET
  @Path("{infraMappingId}/pcf/runningcount")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ, skipAuth = true)
  public RestResponse<Integer> getRunningCountForPcfApp(@QueryParam("appId") String appId,
      @QueryParam("appNameExpr") String appNameExpr, @PathParam("infraMappingId") String infraMappingId) {
    return new RestResponse<>(infrastructureMappingService.getPcfRunningInstances(appId, infraMappingId, appNameExpr));
  }

  @GET
  @Path("{infraMappingId}/ami/runningcount")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ, skipAuth = true)
  public RestResponse<AwsAsgGetRunningCountData> getRunningCountForAmi(
      @QueryParam("appId") String appId, @PathParam("infraMappingId") String infraMappingId) {
    return new RestResponse<>(infrastructureMappingService.getAmiCurrentlyRunningInstanceCount(infraMappingId, appId));
  }

  @PUT
  @Path("{infraMappingId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = UPDATE)
  public RestResponse<InfrastructureMapping> update(@QueryParam("appId") String appId,
      @QueryParam("envId") String envId, @PathParam("infraMappingId") String infraMappingId,
      InfrastructureMapping infrastructureMapping) {
    infrastructureMapping.setAppId(appId);
    infrastructureMapping.setEnvId(envId);
    infrastructureMapping.setUuid(infraMappingId);
    return new RestResponse<>(infrastructureMappingService.update(infrastructureMapping, null));
  }

  @DELETE
  @Path("{infraMappingId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = UPDATE)
  public RestResponse delete(@QueryParam("appId") String appId, @QueryParam("envId") String envId,
      @PathParam("infraMappingId") String infraMappingId) {
    infrastructureMappingService.delete(appId, infraMappingId);
    return new RestResponse();
  }

  @GET
  @Path("stencils")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<Map<String, Object>> infrastructureMappingSchema(@QueryParam("appId") String appId) {
    return new RestResponse<>(infrastructureMappingService.getInfraMappingStencils(appId));
  }

  @GET
  @Path("infra-types")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ)
  public RestResponse<Map<DeploymentType, List<SettingVariableTypes>>> infrastructureTypes(
      @QueryParam("appId") String appId, @QueryParam("envId") String envId, @QueryParam("serviceId") String serviceId) {
    return new RestResponse<>(infrastructureMappingService.listInfraTypes(appId, envId, serviceId));
  }

  @GET
  @Path("compute-providers/{computeProviderId}/clusters")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ, skipAuth = true)
  public RestResponse<List<String>> getClusterNames(@QueryParam("appId") String appId,
      @QueryParam("deploymentType") String deploymentType, @QueryParam("region") String region,
      @PathParam("computeProviderId") String computeProviderId) {
    return new RestResponse<>(
        infrastructureMappingService.listClusters(appId, deploymentType, computeProviderId, region));
  }

  @POST
  @Path("validate-hosts")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = UPDATE)
  public RestResponse<List<HostValidationResponse>> get(
      @QueryParam("appId") String appId, @QueryParam("envId") String envId, HostValidationRequest validationRequest) {
    validationRequest.setAppId(appId);
    validationRequest.setEnvId(envId);
    return new RestResponse<>(infrastructureMappingService.validateHost(validationRequest));
  }

  @GET
  @Path("compute-providers/{computeProviderId}/instanceTypes")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ, skipAuth = true)
  public RestResponse<List<String>> getInstanceTypes(@QueryParam("appId") String appId,
      @QueryParam("deploymentType") String deploymentType, @PathParam("computeProviderId") String computeProviderId) {
    return new RestResponse<>(infrastructureMappingService.listInstanceTypes(appId, deploymentType, computeProviderId));
  }

  @GET
  @Path("compute-providers/{computeProviderId}/regions")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ, skipAuth = true)
  public RestResponse<List<String>> getRegions(@QueryParam("appId") String appId,
      @QueryParam("deploymentType") String deploymentType, @PathParam("computeProviderId") String computeProviderId) {
    return new RestResponse<>(infrastructureMappingService.listRegions(appId, deploymentType, computeProviderId));
  }

  @GET
  @Path("compute-providers/{computeProviderId}/instance-roles")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ, skipAuth = true)
  public RestResponse<List<String>> getInstanceRoles(@QueryParam("appId") String appId,
      @QueryParam("deploymentType") String deploymentType, @PathParam("computeProviderId") String computeProviderId) {
    return new RestResponse<>(infrastructureMappingService.listInstanceRoles(appId, deploymentType, computeProviderId));
  }

  @GET
  @Path("compute-providers/{computeProviderId}/vpcs")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ, skipAuth = true)
  public RestResponse<List<String>> listVpcs(@QueryParam("appId") String appId, @QueryParam("region") String region,
      @PathParam("computeProviderId") String computeProviderId) {
    return new RestResponse<>(infrastructureMappingService.getVPCIds(appId, computeProviderId, region));
  }

  @GET
  @Path("compute-providers/{computeProviderId}/security-groups")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ, skipAuth = true)
  public RestResponse<List<String>> listSecurityGroups(@QueryParam("appId") String appId,
      @QueryParam("region") String region, @QueryParam("vpcIds") @NotNull List<String> vpcIds,
      @PathParam("computeProviderId") String computeProviderId) {
    return new RestResponse<>(infrastructureMappingService.getSGIds(appId, computeProviderId, region, vpcIds));
  }

  @GET
  @Path("compute-providers/{computeProviderId}/subnets")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ, skipAuth = true)
  public RestResponse<List<String>> listSubnets(@QueryParam("appId") String appId, @QueryParam("region") String region,
      @QueryParam("vpcIds") @NotNull List<String> vpcIds, @PathParam("computeProviderId") String computeProviderId) {
    return new RestResponse<>(infrastructureMappingService.getSubnetIds(appId, computeProviderId, region, vpcIds));
  }

  @GET
  @Path("compute-providers/{computeProviderId}/tags")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ, skipAuth = true)
  public RestResponse<Set<String>> listTags(@QueryParam("appId") String appId, @QueryParam("region") String region,
      @PathParam("computeProviderId") String computeProviderId) {
    return new RestResponse<>(infrastructureMappingService.listTags(appId, computeProviderId, region));
  }

  @GET
  @Path("compute-providers/{computeProviderId}/azure-tags")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ, skipAuth = true)
  public RestResponse<Set<String>> listAzureTags(@QueryParam("appId") String appId,
      @QueryParam("subscriptionId") String subscriptionId, @PathParam("computeProviderId") String computeProviderId) {
    return new RestResponse<>(infrastructureMappingService.listAzureTags(appId, computeProviderId, subscriptionId));
  }

  @GET
  @Path("compute-providers/{computeProviderId}/resource-groups")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ, skipAuth = true)
  public RestResponse<Set<String>> listAzureResourceGroups(@QueryParam("appId") String appId,
      @QueryParam("subscriptionId") String subscriptionId, @PathParam("computeProviderId") String computeProviderId) {
    return new RestResponse<>(
        infrastructureMappingService.listAzureResourceGroups(appId, computeProviderId, subscriptionId));
  }

  @GET
  @Path("compute-providers/{computeProviderId}/auto-scaling-groups")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ, skipAuth = true)
  public RestResponse<List<String>> listAutoScalingGroups(@QueryParam("appId") String appId,
      @QueryParam("region") String region, @PathParam("computeProviderId") String computeProviderId) {
    return new RestResponse<>(infrastructureMappingService.listAutoScalingGroups(appId, computeProviderId, region));
  }

  @GET
  @Path("{infraMappingId}/iam-roles")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ, skipAuth = true)
  public RestResponse<Map<String, String>> getInstanceRoles(
      @QueryParam("appId") String appId, @PathParam("infraMappingId") String infraMappingId) {
    return new RestResponse<>(infrastructureMappingService.listAwsIamRoles(appId, infraMappingId));
  }

  @GET
  @Path("compute-providers/{computeProviderId}/roles")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ, skipAuth = true)
  public RestResponse<Map<String, String>> getRoles(@QueryParam("appId") String appId,
      @QueryParam("deploymentType") String deploymentType, @PathParam("computeProviderId") String computeProviderId) {
    return new RestResponse<>(infrastructureMappingService.listAllRoles(appId, computeProviderId));
  }

  @GET
  @Path("{infraMappingId}/load-balancers")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ, skipAuth = true)
  public RestResponse<Map<String, String>> getLoadBalancers(
      @QueryParam("appId") String appId, @PathParam("infraMappingId") String infraMappingId) {
    return new RestResponse<>(infrastructureMappingService.listLoadBalancers(appId, infraMappingId));
  }

  @GET
  @Path("{infraMappingId}/aws-elastic-balancers")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ, skipAuth = true)
  public RestResponse<Map<String, String>> getAwsLoadBalancers(
      @QueryParam("appId") String appId, @PathParam("infraMappingId") String infraMappingId) {
    return new RestResponse<>(infrastructureMappingService.listElasticLoadBalancers(appId, infraMappingId));
  }

  @GET
  @Path("{infraMappingId}/aws-network-balancers")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ, skipAuth = true)
  public RestResponse<Map<String, String>> getAwsNetworkLoadBalancers(
      @QueryParam("appId") String appId, @PathParam("infraMappingId") String infraMappingId) {
    return new RestResponse<>(infrastructureMappingService.listNetworkLoadBalancers(appId, infraMappingId));
  }

  @GET
  @Path("{infraMappingId}/load-balancers/{loadbalancerName}/target-groups")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ, skipAuth = true)
  public RestResponse<Map<String, String>> getTargetGroups(@QueryParam("appId") String appId,
      @PathParam("infraMappingId") String infraMappingId, @PathParam("loadbalancerName") String loadbalancerName) {
    return new RestResponse<>(infrastructureMappingService.listTargetGroups(appId, infraMappingId, loadbalancerName));
  }

  @GET
  @Path("{infraMappingId}/load-balancers/{loadbalancerName}/listeners")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ, skipAuth = true)
  public RestResponse<List<AwsElbListener>> getListeners(@QueryParam("appId") String appId,
      @PathParam("infraMappingId") String infraMappingId, @PathParam("loadbalancerName") String loadbalancerName) {
    return new RestResponse<>(infrastructureMappingService.listListeners(appId, infraMappingId, loadbalancerName));
  }

  @GET
  @Path("{infraMappingId}/hosted-zones")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ, skipAuth = true)
  public RestResponse<List<AwsRoute53HostedZoneData>> getHostedZones(
      @QueryParam("appId") String appId, @PathParam("infraMappingId") String infraMappingId) {
    return new RestResponse<>(infrastructureMappingService.listHostedZones(appId, infraMappingId));
  }

  @GET
  @Path("compute-providers/{computeProviderId}/load-balancers")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ, skipAuth = true)
  public RestResponse<Map<String, String>> getLoadBalancers(@QueryParam("appId") String appId,
      @QueryParam("deploymentType") String deploymentType, @PathParam("computeProviderId") String computeProviderId) {
    return new RestResponse<>(infrastructureMappingService.listLoadBalancers(appId, deploymentType, computeProviderId));
  }

  @GET
  @Path("compute-providers/{computeProviderId}/classic-load-balancers")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ, skipAuth = true)
  public RestResponse<List<String>> getClassicLoadBalancers(@QueryParam("appId") String appId,
      @QueryParam("region") String region, @PathParam("computeProviderId") String computeProviderId) {
    return new RestResponse<>(infrastructureMappingService.listClassicLoadBalancers(appId, computeProviderId, region));
  }

  @GET
  @Path("compute-providers/{computeProviderId}/load-balancer/{loadbalancerName}/target-groups")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ, skipAuth = true)
  public RestResponse<Map<String, String>> getTargetGroups(@QueryParam("appId") String appId,
      @QueryParam("deploymentType") String deploymentType, @PathParam("computeProviderId") String computeProviderId,
      @PathParam("loadbalancerName") String loadbalancerName) {
    return new RestResponse<>(
        infrastructureMappingService.listTargetGroups(appId, deploymentType, computeProviderId, loadbalancerName));
  }

  @GET
  @Path("elastic-load-balancers")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ, skipAuth = true)
  public RestResponse<List<String>> getElasticLoadBalancers(@QueryParam("accountId") String accountId,
      @QueryParam("accessKey") String accessKey, @QueryParam("secretKey") String secretKey,
      @QueryParam("region") String region) {
    return new RestResponse<>(
        infrastructureMappingService.listElasticLoadBalancer(accessKey, secretKey.toCharArray(), region, accountId));
  }

  @GET
  @Path("compute-providers/{computeProviderId}/target-groups")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ, skipAuth = true)
  public RestResponse<Map<String, String>> getAlbTargetGroups(@QueryParam("appId") String appId,
      @QueryParam("region") String region, @PathParam("computeProviderId") String computeProviderId) {
    return new RestResponse<>(infrastructureMappingService.listAlbTargetGroups(appId, computeProviderId, region));
  }

  @GET
  @Path("compute-providers/{computeProviderId}/codedeploy/application-names")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ, skipAuth = true)
  public RestResponse<List<String>> getCodeDeployApplicationNames(@QueryParam("appId") String appId,
      @QueryParam("region") String region, @PathParam("computeProviderId") String computeProviderId) {
    return new RestResponse<>(
        infrastructureMappingService.listCodeDeployApplicationNames(computeProviderId, region, appId));
  }

  @GET
  @Path("compute-providers/{computeProviderId}/codedeploy/deployment-groups")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ, skipAuth = true)
  public RestResponse<List<String>> getCodeDeployDeploymentGroups(@QueryParam("appId") String appId,
      @QueryParam("region") String region, @QueryParam("applicationName") String applicationName,
      @PathParam("computeProviderId") String computeProviderId) {
    return new RestResponse<>(
        infrastructureMappingService.listCodeDeployDeploymentGroups(computeProviderId, region, applicationName, appId));
  }

  @GET
  @Path("compute-providers/{computeProviderId}/codedeploy/deployment-configs")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ, skipAuth = true)
  public RestResponse<List<String>> getCodeDeployDeploymentConfigs(@QueryParam("appId") String appId,
      @QueryParam("region") String region, @PathParam("computeProviderId") String computeProviderId) {
    return new RestResponse<>(
        infrastructureMappingService.listCodeDeployDeploymentConfigs(computeProviderId, region, appId));
  }

  @GET
  @Path("compute-providers/{computeProviderId}/pcf/organizations")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ)
  public RestResponse<List<String>> getOrganizationsForPcf(
      @QueryParam("appId") String appId, @PathParam("computeProviderId") String computeProviderId) {
    return new RestResponse<>(infrastructureMappingService.listOrganizationsForPcf(appId, computeProviderId));
  }

  @GET
  @Path("compute-providers/{computeProviderId}/pcf/spaces")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ)
  public RestResponse<List<String>> getSpacesForPcf(@QueryParam("appId") String appId, @QueryParam("org") String org,
      @PathParam("computeProviderId") String computeProviderId) {
    return new RestResponse<>(infrastructureMappingService.listSpacesForPcf(appId, computeProviderId, org));
  }

  @GET
  @Path("compute-providers/{computeProviderId}/pcf/routes")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ)
  public RestResponse<List<String>> getRoutesForPcf(@QueryParam("appId") String appId, @QueryParam("org") String org,
      @QueryParam("space") String space, @PathParam("computeProviderId") String computeProviderId) {
    return new RestResponse<>(infrastructureMappingService.lisRouteMapsForPcf(appId, computeProviderId, org, space));
  }

  private void populateDeploymentTypeIfRequired(String appId, InfrastructureMapping infrastructureMapping) {
    if (isBlank(infrastructureMapping.getDeploymentType())) {
      Service service = serviceResourceService.getWithDetails(appId, infrastructureMapping.getServiceId());
      if (service == null || service.getDeploymentType() == null) {
        throw new InvalidRequestException("Deployment type cannot be empty");
      }

      infrastructureMapping.setDeploymentType(service.getDeploymentType().name());
    }
  }
}
