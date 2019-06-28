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
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import software.wings.api.DeploymentType;
import software.wings.infra.InfrastructureDefinition;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
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
  public RestResponse<PageResponse<InfrastructureDefinition>> list(
      @BeanParam PageRequest<InfrastructureDefinition> pageRequest) {
    return new RestResponse<>(infrastructureDefinitionService.list(pageRequest));
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
}
