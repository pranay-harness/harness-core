package software.wings.resources;

import static io.harness.beans.SearchFilter.Operator.EQ;
import static software.wings.security.PermissionAttribute.PermissionType.ENV;
import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;
import static software.wings.security.PermissionAttribute.ResourceType.ENVIRONMENT;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import software.wings.beans.Environment;
import software.wings.beans.RestResponse;
import software.wings.beans.Service;
import software.wings.beans.Setup.SetupStatus;
import software.wings.beans.container.KubernetesPayload;
import software.wings.beans.stats.CloneMetadata;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.ListAPI;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.EnvironmentService;

import java.util.List;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by anubhaw on 4/1/16.
 */
@Api("environments")
@Path("/environments")
@Produces("application/json")
@Consumes("application/json")
@Scope(APPLICATION)
@AuthRule(permissionType = ENV)
public class EnvironmentResource {
  @Inject private EnvironmentService environmentService;

  /**
   * List.
   *
   * @param appId      the app id
   * @param pageRequest the page request
   * @return the rest response
   */
  @GET
  @ListAPI(ENVIRONMENT)
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<Environment>> list(@QueryParam("appId") String appId,
      @BeanParam PageRequest<Environment> pageRequest, @QueryParam("details") @DefaultValue("true") boolean details) {
    if (appId != null) {
      pageRequest.addFilter("appId", EQ, appId);
    }
    return new RestResponse<>(environmentService.list(pageRequest, details));
  }

  /**
   * Save.
   *
   * @param appId       the app id
   * @param environment the environment
   * @return the rest response
   */
  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<Environment> save(@QueryParam("appId") String appId, Environment environment) {
    environment.setAppId(appId);
    return new RestResponse<>(environmentService.save(environment));
  }

  /**
   * List.
   *
   * @param appId  the app id
   * @param envId  the env id
   * @param status the status
   * @return the rest response
   */
  @GET
  @Path("{envId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Environment> get(
      @QueryParam("appId") String appId, @PathParam("envId") String envId, @QueryParam("status") SetupStatus status) {
    try {
      if (status == null) {
        status = SetupStatus.COMPLETE;
      }
      return new RestResponse<>(environmentService.get(appId, envId, status));
    } catch (Exception e) {
      return new RestResponse<>();
    }
  }

  /**
   * List.
   *
   * @param appId  the app id
   * @param envId  the env id
   * @return the rest response
   */
  @GET
  @Path("{envId}/services")
  @Timed
  @ExceptionMetered
  public RestResponse<List<Service>> getServicesWithOverrides(
      @QueryParam("appId") String appId, @PathParam("envId") String envId) {
    return new RestResponse(environmentService.getServicesWithOverrides(appId, envId));
  }

  /**
   * Update.
   *
   * @param appId       the app id
   * @param envId       the env id
   * @param environment the environment
   * @return the rest response
   */
  @PUT
  @Path("{envId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Environment> update(
      @QueryParam("appId") String appId, @PathParam("envId") String envId, Environment environment) {
    environment.setUuid(envId);
    environment.setAppId(appId);
    return new RestResponse<>(environmentService.update(environment));
  }

  /**
   * Delete.
   *
   * @param appId the app id
   * @param envId the env id
   * @return the rest response
   */
  @DELETE
  @Path("{envId}")
  @Timed
  @ExceptionMetered
  public RestResponse delete(@QueryParam("appId") String appId, @PathParam("envId") String envId) {
    environmentService.delete(appId, envId);
    return new RestResponse();
  }

  /**
   * Clone environment rest response.
   *
   * @param appId      the app id
   * @param envId the workflow id
   * @param cloneMetadata   the clone metadata
   * @return the rest response
   */
  @POST
  @Path("{envId}/clone")
  @Timed
  @ExceptionMetered
  public RestResponse<Environment> cloneEnvironment(
      @QueryParam("appId") String appId, @PathParam("envId") String envId, CloneMetadata cloneMetadata) {
    return new RestResponse<>(environmentService.cloneEnvironment(appId, envId, cloneMetadata));
  }

  @POST
  @Path("{envId}/config-map-yaml")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.ENV, action = Action.UPDATE)
  public RestResponse<Environment> setConfigMapYaml(
      @ApiParam(name = "appId", required = true) @QueryParam("appId") String appId,
      @ApiParam(name = "envId", required = true) @PathParam("envId") String envId,
      KubernetesPayload kubernetesPayload) {
    return new RestResponse<>(environmentService.setConfigMapYaml(appId, envId, kubernetesPayload));
  }

  @PUT
  @Path("{envId}/config-map-yaml")
  @Timed
  @ExceptionMetered
  public RestResponse<Environment> updateConfigMapYaml(
      @ApiParam(name = "appId", required = true) @QueryParam("appId") String appId,
      @ApiParam(name = "envId", required = true) @PathParam("envId") String envId,
      KubernetesPayload kubernetesPayload) {
    return new RestResponse<>(environmentService.setConfigMapYaml(appId, envId, kubernetesPayload));
  }

  @DELETE
  @Path("{envId}/config-map-yaml")
  @Timed
  @ExceptionMetered
  public RestResponse<Environment> deleteConfigMapYaml(
      @ApiParam(name = "appId", required = true) @QueryParam("appId") String appId,
      @ApiParam(name = "envId", required = true) @PathParam("envId") String envId) {
    return new RestResponse<>(environmentService.setConfigMapYaml(appId, envId, new KubernetesPayload()));
  }

  @POST
  @Path("{envId}/config-map-yaml/{templateId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.ENV, action = Action.UPDATE)
  public RestResponse<Environment> setConfigMapYamlForService(
      @ApiParam(name = "appId", required = true) @QueryParam("appId") String appId,
      @ApiParam(name = "envId", required = true) @PathParam("envId") String envId,
      @ApiParam(name = "templateId", required = true) @PathParam("templateId") String templateId,
      KubernetesPayload kubernetesPayload) {
    return new RestResponse<>(
        environmentService.setConfigMapYamlForService(appId, envId, templateId, kubernetesPayload));
  }

  @PUT
  @Path("{envId}/config-map-yaml/{templateId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Environment> updateConfigMapYamlForService(
      @ApiParam(name = "appId", required = true) @QueryParam("appId") String appId,
      @ApiParam(name = "envId", required = true) @PathParam("envId") String envId,
      @ApiParam(name = "templateId", required = true) @PathParam("templateId") String templateId,
      KubernetesPayload kubernetesPayload) {
    return new RestResponse<>(
        environmentService.setConfigMapYamlForService(appId, envId, templateId, kubernetesPayload));
  }

  @DELETE
  @Path("{envId}/config-map-yaml/{templateId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Environment> deleteConfigMapYamlForService(
      @ApiParam(name = "appId", required = true) @QueryParam("appId") String appId,
      @ApiParam(name = "envId", required = true) @PathParam("envId") String envId,
      @ApiParam(name = "templateId", required = true) @PathParam("templateId") String templateId) {
    return new RestResponse<>(
        environmentService.setConfigMapYamlForService(appId, envId, templateId, new KubernetesPayload()));
  }
  @POST
  @Path("{envId}/helm-value-yaml")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.ENV, action = Action.UPDATE)
  public RestResponse<Environment> setHelmValueYaml(
      @ApiParam(name = "appId", required = true) @QueryParam("appId") String appId,
      @ApiParam(name = "envId", required = true) @PathParam("envId") String envId,
      KubernetesPayload kubernetesPayload) {
    return new RestResponse<>(environmentService.setHelmValueYaml(appId, envId, kubernetesPayload));
  }

  @PUT
  @Path("{envId}/helm-value-yaml")
  @Timed
  @ExceptionMetered
  public RestResponse<Environment> updateHelmValueYaml(
      @ApiParam(name = "appId", required = true) @QueryParam("appId") String appId,
      @ApiParam(name = "envId", required = true) @PathParam("envId") String envId,
      KubernetesPayload kubernetesPayload) {
    return new RestResponse<>(environmentService.setHelmValueYaml(appId, envId, kubernetesPayload));
  }

  @DELETE
  @Path("{envId}/helm-value-yaml")
  @Timed
  @ExceptionMetered
  public RestResponse<Environment> deleteHelmValueYaml(
      @ApiParam(name = "appId", required = true) @QueryParam("appId") String appId,
      @ApiParam(name = "envId", required = true) @PathParam("envId") String envId) {
    return new RestResponse<>(environmentService.setHelmValueYaml(appId, envId, new KubernetesPayload()));
  }

  @POST
  @Path("{envId}/helm-value-yaml/{templateId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.ENV, action = Action.UPDATE)
  public RestResponse<Environment> setHelmValueYamlForService(
      @ApiParam(name = "appId", required = true) @QueryParam("appId") String appId,
      @ApiParam(name = "envId", required = true) @PathParam("envId") String envId,
      @ApiParam(name = "templateId", required = true) @PathParam("templateId") String templateId,
      KubernetesPayload kubernetesPayload) {
    return new RestResponse<>(
        environmentService.setHelmValueYamlForService(appId, envId, templateId, kubernetesPayload));
  }

  @PUT
  @Path("{envId}/helm-value-yaml/{templateId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Environment> updateHelmValueYamlForService(
      @ApiParam(name = "appId", required = true) @QueryParam("appId") String appId,
      @ApiParam(name = "envId", required = true) @PathParam("envId") String envId,
      @ApiParam(name = "templateId", required = true) @PathParam("templateId") String templateId,
      KubernetesPayload kubernetesPayload) {
    return new RestResponse<>(
        environmentService.setHelmValueYamlForService(appId, envId, templateId, kubernetesPayload));
  }

  @DELETE
  @Path("{envId}/helm-value-yaml/{templateId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Environment> deleteHelmValueYamlForService(
      @ApiParam(name = "appId", required = true) @QueryParam("appId") String appId,
      @ApiParam(name = "envId", required = true) @PathParam("envId") String envId,
      @ApiParam(name = "templateId", required = true) @PathParam("templateId") String templateId) {
    return new RestResponse<>(
        environmentService.setHelmValueYamlForService(appId, envId, templateId, new KubernetesPayload()));
  }
}
