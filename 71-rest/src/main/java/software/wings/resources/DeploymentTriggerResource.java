package software.wings.resources;

import static io.harness.beans.SearchFilter.Operator.EQ;
import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.trigger.DeploymentTriggerService;
import software.wings.utils.Validator;

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

@Api("deployment-triggers")
@Path("/deployment-triggers")
@Produces("application/json")
@Consumes("application/json")
@Scope(APPLICATION)
public class DeploymentTriggerResource {
  private DeploymentTriggerService deploymentTriggerService;

  @Inject
  public DeploymentTriggerResource(DeploymentTriggerService deploymentTriggerService) {
    this.deploymentTriggerService = deploymentTriggerService;
  }

  /**
   * @param appId
   * @param pageRequest
   * @return
   */
  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<DeploymentTrigger>> list(
      @QueryParam("appId") String appId, @BeanParam PageRequest<DeploymentTrigger> pageRequest) {
    pageRequest.addFilter("appId", EQ, appId);
    return new RestResponse<>(deploymentTriggerService.list(pageRequest));
  }

  /**
   * Gets the.
   *
   * @param appId    the app id
   * @param triggerId the stream id
   * @return the rest response
   */
  @GET
  @Path("{triggerId}")
  @Timed
  @ExceptionMetered
  public RestResponse<DeploymentTrigger> get(
      @QueryParam("appId") String appId, @PathParam("triggerId") String triggerId) {
    return new RestResponse<>(deploymentTriggerService.get(appId, triggerId));
  }

  /**
   * Save rest response.
   *
   * @param appId   the app id
   * @param trigger the artifact stream
   * @return the rest response
   */
  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<DeploymentTrigger> save(@QueryParam("appId") String appId, DeploymentTrigger trigger) {
    Validator.notNullCheck("trigger", trigger);
    trigger.setAppId(appId);
    if (trigger.getUuid() != null) {
      return new RestResponse(deploymentTriggerService.update(trigger));
    }
    return new RestResponse<>(deploymentTriggerService.save(trigger));
  }

  /**
   * Update rest response.
   *
   * @param appId    the app id
   * @param triggerId the stream id
   * @param trigger  the artifact stream
   * @return the rest response
   */
  @PUT
  @Path("{triggerId}")
  @Timed
  @ExceptionMetered
  public RestResponse<DeploymentTrigger> update(
      @QueryParam("appId") String appId, @PathParam("triggerId") String triggerId, DeploymentTrigger trigger) {
    trigger.setUuid(triggerId);
    trigger.setAppId(appId);
    return new RestResponse<>(deploymentTriggerService.update(trigger));
  }

  /**
   * Delete.
   *
   * @param appId the app id
   * @param triggerId    the id
   * @return the rest response
   */
  @DELETE
  @Path("{triggerId}")
  @Timed
  @ExceptionMetered
  public RestResponse delete(@QueryParam("appId") String appId, @PathParam("triggerId") String triggerId) {
    deploymentTriggerService.delete(appId, triggerId);
    return new RestResponse<>();
  }
}
