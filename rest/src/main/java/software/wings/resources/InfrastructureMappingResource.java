package software.wings.resources;

import com.google.inject.Inject;

import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.api.DeploymentType;
import software.wings.beans.HostValidationRequest;
import software.wings.beans.HostValidationResponse;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.RestResponse;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.List;
import java.util.Map;
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
@AuthRule(ResourceType.APPLICATION)
public class InfrastructureMappingResource {
  @Inject private InfrastructureMappingService infrastructureMappingService;

  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<InfrastructureMapping>> list(
      @BeanParam PageRequest<InfrastructureMapping> pageRequest) {
    return new RestResponse<>(infrastructureMappingService.list(pageRequest));
  }

  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<InfrastructureMapping> save(@QueryParam("appId") String appId, @QueryParam("envId") String envId,
      InfrastructureMapping infrastructureMapping) {
    infrastructureMapping.setAppId(appId);
    infrastructureMapping.setEnvId(envId);
    return new RestResponse<>(infrastructureMappingService.save(infrastructureMapping));
  }

  @GET
  @Path("{infraMappingId}")
  @Timed
  @ExceptionMetered
  public RestResponse<InfrastructureMapping> get(@QueryParam("appId") String appId, @QueryParam("envId") String envId,
      @PathParam("infraMappingId") String infraMappingId) {
    return new RestResponse<>(infrastructureMappingService.get(appId, infraMappingId));
  }

  @GET
  @Path("compute-providers/{computeProviderId}/hosts")
  @Timed
  @ExceptionMetered
  public RestResponse<List<String>> listComputeProviderHosts(@QueryParam("appId") String appId,
      @QueryParam("envId") String envId, @QueryParam("serviceId") String serviceId,
      @PathParam("computeProviderId") String computeProviderId) {
    return new RestResponse<>(
        infrastructureMappingService.listComputeProviderHosts(appId, envId, serviceId, computeProviderId));
  }

  @GET
  @Path("{infraMappingId}/hosts")
  @Timed
  @ExceptionMetered
  public RestResponse<List<String>> listComputeProviderHosts(
      @QueryParam("appId") String appId, @PathParam("infraMappingId") String infraMappingId) {
    return new RestResponse<>(infrastructureMappingService.listHosts(appId, infraMappingId));
  }

  @GET
  @Path("{infraMappingId}/launchconfigs")
  @Timed
  @ExceptionMetered
  public RestResponse<List<LaunchConfiguration>> listLaunchConfigs(@QueryParam("appId") String appId,
      @QueryParam("envId") String envId, @PathParam("infraMappingId") String infraMappingId) {
    return new RestResponse<>(infrastructureMappingService.listLaunchConfigs(appId, envId, infraMappingId));
  }

  @PUT
  @Path("{infraMappingId}")
  @Timed
  @ExceptionMetered
  public RestResponse<InfrastructureMapping> update(@QueryParam("appId") String appId,
      @QueryParam("envId") String envId, @PathParam("infraMappingId") String infraMappingId,
      InfrastructureMapping infrastructureMapping) {
    infrastructureMapping.setAppId(appId);
    infrastructureMapping.setEnvId(envId);
    infrastructureMapping.setUuid(infraMappingId);
    return new RestResponse<>(infrastructureMappingService.update(infrastructureMapping));
  }

  @DELETE
  @Path("{infraMappingId}")
  @Timed
  @ExceptionMetered
  public RestResponse delete(@QueryParam("appId") String appId, @QueryParam("envId") String envId,
      @PathParam("infraMappingId") String infraMappingId) {
    infrastructureMappingService.delete(appId, envId, infraMappingId);
    return new RestResponse();
  }

  @GET
  @Path("stencils")
  @Timed
  @ExceptionMetered
  public RestResponse<Map<String, Object>> infrastructureMappingSchema(@QueryParam("appId") String appId) {
    return new RestResponse<>(infrastructureMappingService.getInfraMappingStencils(appId));
  }

  @GET
  @Path("infra-types")
  @Timed
  @ExceptionMetered
  public RestResponse<Map<DeploymentType, List<SettingVariableTypes>>> infrastructureTypes(
      @QueryParam("appId") String appId, @QueryParam("envId") String envId, @QueryParam("serviceId") String serviceId) {
    return new RestResponse<>(infrastructureMappingService.listInfraTypes(appId, envId, serviceId));
  }

  @GET
  @Path("compute-providers/{computeProviderId}/clusters")
  @Timed
  @ExceptionMetered
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
  public RestResponse<List<HostValidationResponse>> get(
      @QueryParam("appId") String appId, @QueryParam("envId") String envId, HostValidationRequest validationRequest) {
    validationRequest.setAppId(appId);
    validationRequest.setEnvId(envId);
    return new RestResponse<>(infrastructureMappingService.validateHost(validationRequest));
  }

  @GET
  @Path("compute-providers/{computeProviderId}/images")
  @Timed
  @ExceptionMetered
  public RestResponse<List<String>> getImages(@QueryParam("appId") String appId,
      @QueryParam("deploymentType") String deploymentType, @PathParam("computeProviderId") String computeProviderId,
      @QueryParam("region") String region) {
    return new RestResponse<>(
        infrastructureMappingService.listImages(appId, deploymentType, computeProviderId, region));
  }

  @GET
  @Path("compute-providers/{computeProviderId}/instanceTypes")
  @Timed
  @ExceptionMetered
  public RestResponse<List<String>> getInstanceTypes(@QueryParam("appId") String appId,
      @QueryParam("deploymentType") String deploymentType, @PathParam("computeProviderId") String computeProviderId) {
    return new RestResponse<>(infrastructureMappingService.listInstanceTypes(appId, deploymentType, computeProviderId));
  }

  @GET
  @Path("compute-providers/{computeProviderId}/regions")
  @Timed
  @ExceptionMetered
  public RestResponse<List<String>> getRegions(@QueryParam("appId") String appId,
      @QueryParam("deploymentType") String deploymentType, @PathParam("computeProviderId") String computeProviderId) {
    return new RestResponse<>(infrastructureMappingService.listRegions(appId, deploymentType, computeProviderId));
  }

  @GET
  @Path("compute-providers/{computeProviderId}/instance-roles")
  @Timed
  @ExceptionMetered
  public RestResponse<List<String>> getInstanceRoles(@QueryParam("appId") String appId,
      @QueryParam("deploymentType") String deploymentType, @PathParam("computeProviderId") String computeProviderId) {
    return new RestResponse<>(infrastructureMappingService.listInstanceRoles(appId, deploymentType, computeProviderId));
  }

  @GET
  @Path("compute-providers/{computeProviderId}/vpcs")
  @Timed
  @ExceptionMetered
  public RestResponse<List<String>> listVpcs(@QueryParam("appId") String appId, @QueryParam("region") String region,
      @PathParam("computeProviderId") String computeProviderId) {
    return new RestResponse<>(infrastructureMappingService.listVPC(appId, computeProviderId, region));
  }

  @GET
  @Path("compute-providers/{computeProviderId}/security-groups")
  @Timed
  @ExceptionMetered
  public RestResponse<List<String>> listSecurityGroups(@QueryParam("appId") String appId,
      @QueryParam("region") String region, @QueryParam("vpcIds") List<String> vpcIds,
      @PathParam("computeProviderId") String computeProviderId) {
    return new RestResponse<>(
        infrastructureMappingService.listSecurityGroups(appId, computeProviderId, region, vpcIds));
  }

  @GET
  @Path("compute-providers/{computeProviderId}/subnets")
  @Timed
  @ExceptionMetered
  public RestResponse<List<String>> listSubnets(@QueryParam("appId") String appId, @QueryParam("region") String region,
      @QueryParam("vpcIds") List<String> vpcIds, @PathParam("computeProviderId") String computeProviderId) {
    return new RestResponse<>(infrastructureMappingService.listSubnets(appId, computeProviderId, region, vpcIds));
  }

  @GET
  @Path("{infraMappingId}/iam-roles")
  @Timed
  @ExceptionMetered
  public RestResponse<Map<String, String>> getInstanceRoles(
      @QueryParam("appId") String appId, @PathParam("infraMappingId") String infraMappingId) {
    return new RestResponse<>(infrastructureMappingService.listAwsIamRoles(appId, infraMappingId));
  }

  @GET
  @Path("compute-providers/{computeProviderId}/roles")
  @Timed
  @ExceptionMetered
  public RestResponse<Map<String, String>> getRoles(@QueryParam("appId") String appId,
      @QueryParam("deploymentType") String deploymentType, @PathParam("computeProviderId") String computeProviderId) {
    return new RestResponse<>(infrastructureMappingService.listAllRoles(appId, computeProviderId));
  }

  @GET
  @Path("{infraMappingId}/load-balancers")
  @Timed
  @ExceptionMetered
  public RestResponse<Map<String, String>> getLoadBalancers(
      @QueryParam("appId") String appId, @PathParam("infraMappingId") String infraMappingId) {
    return new RestResponse<>(infrastructureMappingService.listLoadBalancers(appId, infraMappingId));
  }

  @GET
  @Path("{infraMappingId}/load-balancers/{loadbalancerName}/target-groups")
  @Timed
  @ExceptionMetered
  public RestResponse<Map<String, String>> getTargetGroups(@QueryParam("appId") String appId,
      @PathParam("infraMappingId") String infraMappingId, @PathParam("loadbalancerName") String loadbalancerName) {
    return new RestResponse<>(infrastructureMappingService.listTargetGroups(appId, infraMappingId, loadbalancerName));
  }

  @GET
  @Path("compute-providers/{computeProviderId}/load-balancers")
  @Timed
  @ExceptionMetered
  public RestResponse<Map<String, String>> getLoadBalancers(@QueryParam("appId") String appId,
      @QueryParam("deploymentType") String deploymentType, @PathParam("computeProviderId") String computeProviderId) {
    return new RestResponse<>(infrastructureMappingService.listLoadBalancers(appId, deploymentType, computeProviderId));
  }

  @GET
  @Path("compute-providers/{computeProviderId}/classic-load-balancers")
  @Timed
  @ExceptionMetered
  public RestResponse<List<String>> getClassicLoadBalancers(@QueryParam("appId") String appId,
      @QueryParam("region") String region, @PathParam("computeProviderId") String computeProviderId) {
    return new RestResponse<>(infrastructureMappingService.listClassicLoadBalancers(appId, computeProviderId, region));
  }

  @GET
  @Path("compute-providers/{computeProviderId}/load-balancer/{loadbalancerName}/target-groups")
  @Timed
  @ExceptionMetered
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
  public RestResponse<List<String>> getElasticLoadBalancers(@QueryParam("accountId") String accountId,
      @QueryParam("accessKey") String accessKey, @QueryParam("secretKey") String secretKey,
      @QueryParam("region") String region) {
    return new RestResponse<>(
        infrastructureMappingService.listElasticLoadBalancer(accessKey, secretKey.toCharArray(), region));
  }

  @GET
  @Path("compute-providers/{computeProviderId}/codedeploy/application-names")
  @Timed
  @ExceptionMetered
  public RestResponse<List<String>> getCodeDeployApplicationNames(@QueryParam("appId") String appId,
      @QueryParam("region") String region, @PathParam("computeProviderId") String computeProviderId) {
    return new RestResponse<>(infrastructureMappingService.listCodeDeployApplicationNames(computeProviderId, region));
  }

  @GET
  @Path("compute-providers/{computeProviderId}/codedeploy/deployment-groups")
  @Timed
  @ExceptionMetered
  public RestResponse<List<String>> getCodeDeployDeploymentGroups(@QueryParam("appId") String appId,
      @QueryParam("region") String region, @QueryParam("applicationName") String applicationName,
      @PathParam("computeProviderId") String computeProviderId) {
    return new RestResponse<>(
        infrastructureMappingService.listCodeDeployDeploymentGroups(computeProviderId, region, applicationName));
  }

  @GET
  @Path("compute-providers/{computeProviderId}/codedeploy/deployment-configs")
  @Timed
  @ExceptionMetered
  public RestResponse<List<String>> getCodeDeployDeploymentConfigs(@QueryParam("appId") String appId,
      @QueryParam("region") String region, @PathParam("computeProviderId") String computeProviderId) {
    return new RestResponse<>(infrastructureMappingService.listCodeDeployDeploymentConfigs(computeProviderId, region));
  }
}
