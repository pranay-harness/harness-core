package software.wings.resources;

import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.security.PermissionAttribute.ResourceType.ENVIRONMENT;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.Environment;
import software.wings.beans.RestResponse;
import software.wings.beans.Service;
import software.wings.beans.Setup.SetupStatus;
import software.wings.beans.stats.CloneMetadata;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.ListAPI;
import software.wings.service.intfc.EnvironmentService;

import java.util.List;
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
 * Created by anubhaw on 4/1/16.
 */
@Api("environments")
@Path("/environments")
@Produces("application/json")
@Consumes("application/json")
@AuthRule(ResourceType.APPLICATION)
public class EnvironmentResource {
  @Inject private EnvironmentService envService;

  /**
   * List.
   *
   * @param appId       the app id
   * @param pageRequest the page request
   * @return the rest response
   */
  @GET
  @ListAPI(ENVIRONMENT)
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<Environment>> list(
      @QueryParam("appId") String appId, @BeanParam PageRequest<Environment> pageRequest) {
    pageRequest.addFilter("appId", EQ, appId);
    return new RestResponse<>(envService.list(pageRequest, true));
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
    return new RestResponse<>(envService.save(environment));
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
    if (status == null) {
      status = SetupStatus.COMPLETE;
    }
    return new RestResponse<>(envService.get(appId, envId, status));
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
    return new RestResponse(envService.getServicesWithOverrides(appId, envId));
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
    return new RestResponse<>(envService.update(environment));
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
    envService.delete(appId, envId);
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
    return new RestResponse<>(envService.cloneEnvironment(appId, envId, cloneMetadata));
  }
}
