package software.wings.resources;

import static software.wings.security.PermissionAttribute.Action.READ;
import static software.wings.security.PermissionAttribute.Action.UPDATE;
import static software.wings.security.PermissionAttribute.PermissionType.ENV;
import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.delegate.task.aws.AwsElbListener;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.api.DeploymentType;
import software.wings.infra.InfraDefinitionDetail;
import software.wings.infra.InfrastructureDefinition;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.aws.model.AwsRoute53HostedZoneData;
import software.wings.service.intfc.InfrastructureDefinitionService;
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

@Api("infrastructure-definitions")
@Path("infrastructure-definitions")
@Produces("application/json")
@Consumes("application/json")
@Scope(ResourceType.APPLICATION)
public class InfrastructureDefinitionResource {
  @Inject private InfrastructureDefinitionService infrastructureDefinitionService;

  @GET
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ)
  public RestResponse<PageResponse<InfrastructureDefinition>> list(@NotEmpty @QueryParam("appId") String appId,
      @NotEmpty @QueryParam("envId") String envId, @QueryParam("serviceId") String serviceId) {
    return new RestResponse<>(infrastructureDefinitionService.list(appId, envId, serviceId));
  }

  @GET
  @Path("details")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ)
  public RestResponse<PageResponse<InfraDefinitionDetail>> listDetails(
      @BeanParam PageRequest<InfrastructureDefinition> pageRequest, @NotEmpty @QueryParam("appId") String appId,
      @NotEmpty @QueryParam("envId") String envId) {
    return new RestResponse<>(infrastructureDefinitionService.listInfraDefinitionDetail(pageRequest, appId, envId));
  }

  @POST
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = UPDATE)
  public RestResponse<InfrastructureDefinition> save(@QueryParam("appId") String appId,
      @QueryParam("envId") String envId, InfrastructureDefinition infrastructureDefinition) {
    infrastructureDefinition.setAppId(appId);
    infrastructureDefinition.setEnvId(envId);
    return new RestResponse<>(infrastructureDefinitionService.save(infrastructureDefinition));
  }

  @GET
  @Path("{infraDefinitionId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ)
  public RestResponse<InfrastructureDefinition> get(@QueryParam("appId") String appId,
      @QueryParam("envId") String envId, @PathParam("infraDefinitionId") String infraDefinitionId) {
    return new RestResponse<>(infrastructureDefinitionService.get(appId, infraDefinitionId));
  }

  @GET
  @Path("detail/{infraDefinitionId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ)
  public RestResponse<InfraDefinitionDetail> getDetail(@QueryParam("appId") String appId,
      @QueryParam("envId") String envId, @PathParam("infraDefinitionId") String infraDefinitionId) {
    return new RestResponse<>(infrastructureDefinitionService.getDetail(appId, infraDefinitionId));
  }

  @DELETE
  @Path("{infraDefinitionId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = UPDATE)
  public RestResponse delete(@QueryParam("appId") String appId, @QueryParam("envId") String envId,
      @PathParam("infraDefinitionId") String infraDefinitionId) {
    infrastructureDefinitionService.delete(appId, infraDefinitionId);
    return new RestResponse();
  }

  @PUT
  @Path("{infraDefinitionId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = UPDATE)
  public RestResponse<InfrastructureDefinition> update(@QueryParam("appId") String appId,
      @QueryParam("envId") String envId, @PathParam("infraDefinitionId") String infraDefinitionId,
      InfrastructureDefinition infrastructureDefinition) {
    infrastructureDefinition.setAppId(appId);
    infrastructureDefinition.setEnvId(envId);
    infrastructureDefinition.setUuid(infraDefinitionId);
    return new RestResponse<>(infrastructureDefinitionService.update(infrastructureDefinition));
  }

  @GET
  @Path("deployment-cloudProviders")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<Map<DeploymentType, List<SettingVariableTypes>>> infrastructureTypes() {
    return new RestResponse<>(infrastructureDefinitionService.getDeploymentTypeCloudProviderOptions());
  }

  @GET
  @Path("{infraDefinitionId}/hosts")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ, skipAuth = true)
  public RestResponse<List<String>> listHosts(
      @QueryParam("appId") String appId, @PathParam("infraDefinitionId") String infraDefinitionId) {
    return new RestResponse<>(infrastructureDefinitionService.listHostDisplayNames(appId, infraDefinitionId, null));
  }

  @GET
  @Path("{infraDefinitionId}/iam-roles")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ, skipAuth = true)
  public RestResponse<Map<String, String>> getInstanceRoles(
      @QueryParam("appId") String appId, @PathParam("infraDefinitionId") String infraDefinitionId) {
    return new RestResponse<>(infrastructureDefinitionService.listAwsIamRoles(appId, infraDefinitionId));
  }

  @GET
  @Path("{infraDefinitionId}/load-balancers")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ, skipAuth = true)
  public RestResponse<Map<String, String>> getLoadBalancers(
      @QueryParam("appId") String appId, @PathParam("infraDefinitionId") String infraDefinitionId) {
    return new RestResponse<>(infrastructureDefinitionService.listLoadBalancers(appId, infraDefinitionId));
  }

  @GET
  @Path("{infraDefinitionId}/aws-elastic-balancers")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ, skipAuth = true)
  public RestResponse<Map<String, String>> getAwsLoadBalancers(
      @QueryParam("appId") String appId, @PathParam("infraDefinitionId") String infraDefinitionId) {
    return new RestResponse<>(infrastructureDefinitionService.listElasticLoadBalancers(appId, infraDefinitionId));
  }

  @GET
  @Path("{infraDefinitionId}/aws-network-balancers")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ, skipAuth = true)
  public RestResponse<Map<String, String>> getAwsNetworkLoadBalancers(
      @QueryParam("appId") String appId, @PathParam("infraDefinitionId") String infraDefinitionId) {
    return new RestResponse<>(infrastructureDefinitionService.listNetworkLoadBalancers(appId, infraDefinitionId));
  }

  @GET
  @Path("{infraDefinitionId}/load-balancers/{loadbalancerName}/target-groups")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ, skipAuth = true)
  public RestResponse<Map<String, String>> getTargetGroups(@QueryParam("appId") String appId,
      @PathParam("infraDefinitionId") String infraDefinitionId,
      @PathParam("loadbalancerName") String loadbalancerName) {
    return new RestResponse<>(
        infrastructureDefinitionService.listTargetGroups(appId, infraDefinitionId, loadbalancerName));
  }

  @GET
  @Path("{infraDefinitionId}/load-balancers/{loadbalancerName}/listeners")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ, skipAuth = true)
  public RestResponse<List<AwsElbListener>> getListeners(@QueryParam("appId") String appId,
      @PathParam("infraDefinitionId") String infraDefinitionId,
      @PathParam("loadbalancerName") String loadbalancerName) {
    return new RestResponse<>(
        infrastructureDefinitionService.listListeners(appId, infraDefinitionId, loadbalancerName));
  }

  @GET
  @Path("{infraDefinitionId}/hosted-zones")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = READ, skipAuth = true)
  public RestResponse<List<AwsRoute53HostedZoneData>> getHostedZones(
      @QueryParam("appId") String appId, @PathParam("infraDefinitionId") String infraDefinitionId) {
    return new RestResponse<>(infrastructureDefinitionService.listHostedZones(appId, infraDefinitionId));
  }
}
