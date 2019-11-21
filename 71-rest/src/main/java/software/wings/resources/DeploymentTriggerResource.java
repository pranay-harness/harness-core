package software.wings.resources;

import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;
import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.exception.WingsException;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.beans.trigger.WebhookSource;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.trigger.TriggerAuthHandler;
import software.wings.service.intfc.trigger.DeploymentTriggerService;

import java.util.Map;
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

@Api("deployment-triggers")
@Path("/deployment-triggers")
@Produces("application/json")
@Consumes("application/json")
@Scope(APPLICATION)
public class DeploymentTriggerResource {
  private DeploymentTriggerService deploymentTriggerService;
  private TriggerAuthHandler triggerAuthHandler;

  @Inject
  public DeploymentTriggerResource(
      DeploymentTriggerService deploymentTriggerService, TriggerAuthHandler triggerAuthHandler) {
    this.deploymentTriggerService = deploymentTriggerService;
    this.triggerAuthHandler = triggerAuthHandler;
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
   * @param readPrimaryVariablesValueNames Primary variables are service, environment and infra
   * @return the rest response
   */
  @GET
  @Path("{triggerId}")
  @Timed
  @ExceptionMetered
  public RestResponse<DeploymentTrigger> get(@QueryParam("appId") String appId,
      @PathParam("triggerId") String triggerId,
      @QueryParam("readPrimaryVariablesValueNames") @DefaultValue("false") boolean readPrimaryVariablesValueNames) {
    return new RestResponse<>(deploymentTriggerService.get(appId, triggerId, readPrimaryVariablesValueNames));
  }

  @GET
  @Path("subEvents")
  @Timed
  @ExceptionMetered
  public RestResponse<Map<String, WebhookSource.WebhookEventInfo>> fetchWebhookChildEvents(
      @QueryParam("webhookSource") String webhookSource) {
    return new RestResponse<>(deploymentTriggerService.fetchWebhookChildEvents(webhookSource));
  }

  @GET
  @Path("customExp")
  @Timed
  @ExceptionMetered
  public RestResponse<Map<String, String>> fetchCustomExpressionList(
      @QueryParam("webhookSource") String webhookSource) {
    return new RestResponse<>(deploymentTriggerService.fetchCustomExpressionList(webhookSource));
  }

  @POST
  @Path("cron/translate")
  @Timed
  @ExceptionMetered
  public RestResponse<String> translateCron(Map<String, String> inputMap) {
    return new RestResponse<>(deploymentTriggerService.getCronDescription(inputMap.get("expression")));
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
    notNullCheck("trigger", trigger);
    trigger.setAppId(appId);
    if (trigger.getUuid() != null) {
      DeploymentTrigger existingTrigger = deploymentTriggerService.getWithoutRead(appId, trigger.getUuid());
      if (existingTrigger == null) {
        throw new WingsException("Trigger does not exist for uuid:" + trigger.getUuid(), USER);
      }
      triggerAuthHandler.authorize(existingTrigger, true);
      triggerAuthHandler.authorize(trigger, false);
      return new RestResponse<>(deploymentTriggerService.update(trigger));
    }
    triggerAuthHandler.authorize(trigger, false);
    return new RestResponse<>(deploymentTriggerService.save(trigger, false));
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
    DeploymentTrigger existingTrigger = deploymentTriggerService.getWithoutRead(appId, trigger.getUuid());
    if (existingTrigger == null) {
      throw new WingsException("Trigger doesn't exist for uuid: " + triggerId, USER);
    }
    triggerAuthHandler.authorize(existingTrigger, true);
    triggerAuthHandler.authorize(trigger, false);
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
    DeploymentTrigger trigger = deploymentTriggerService.getWithoutRead(appId, triggerId);
    if (trigger != null) {
      triggerAuthHandler.authorize(trigger, true);
      deploymentTriggerService.delete(appId, triggerId);
    }
    return new RestResponse<>();
  }
}
