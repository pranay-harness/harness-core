package software.wings.resources;

import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.beans.WebHookToken;
import software.wings.beans.WorkflowType;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.WebhookEventType;
import software.wings.beans.trigger.WebhookParameters;
import software.wings.beans.trigger.WebhookSource;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.TriggerService;
import software.wings.utils.Validator;

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
 * Created by sgurubelli on 10/26/17.
 */
@Api("triggers")
@Path("/triggers")
@Produces("application/json")
@Consumes("application/json")
@Scope(APPLICATION)
public class TriggerResource {
  private TriggerService triggerService;

  @Inject
  public TriggerResource(TriggerService triggerService) {
    this.triggerService = triggerService;
  }

  /**
   * @param appId
   * @param pageRequest
   * @return
   */
  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<Trigger>> list(
      @QueryParam("appId") String appId, @BeanParam PageRequest<Trigger> pageRequest) {
    pageRequest.addFilter("appId", EQ, appId);
    return new RestResponse<>(triggerService.list(pageRequest));
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
  public RestResponse<Trigger> get(@QueryParam("appId") String appId, @PathParam("triggerId") String triggerId) {
    return new RestResponse<>(triggerService.get(appId, triggerId));
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
  public RestResponse<Trigger> save(@QueryParam("appId") String appId, Trigger trigger) {
    Validator.notNullCheck("trigger", trigger);
    trigger.setAppId(appId);
    if (trigger.getUuid() != null) {
      return new RestResponse(triggerService.update(trigger));
    }
    return new RestResponse<>(triggerService.save(trigger));
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
  public RestResponse<Trigger> update(
      @QueryParam("appId") String appId, @PathParam("triggerId") String triggerId, Trigger trigger) {
    trigger.setUuid(triggerId);
    trigger.setAppId(appId);
    return new RestResponse<>(triggerService.update(trigger));
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
    triggerService.delete(appId, triggerId);
    return new RestResponse<>();
  }

  @GET
  @Path("{triggerId}/webhook_token")
  @Timed
  @ExceptionMetered
  public RestResponse<WebHookToken> generateWebhookToken(
      @QueryParam("appId") String appId, @PathParam("triggerId") String triggerId) {
    return new RestResponse<>(triggerService.generateWebHookToken(appId, triggerId));
  }

  @GET
  @Path("{triggerId}/webhook_token/git")
  @Timed
  @ExceptionMetered
  public RestResponse<WebHookToken> generateGitWebhookToken(
      @QueryParam("appId") String appId, @PathParam("triggerId") String triggerId) {
    return new RestResponse<>(triggerService.generateWebHookToken(appId, triggerId));
  }

  /**
   * Translate cron rest response.
   *
   * @param inputMap the input map
   * @return the rest response
   */
  @POST
  @Path("cron/translate")
  @Timed
  @ExceptionMetered
  public RestResponse<String> translateCron(Map<String, String> inputMap) {
    return new RestResponse<>(triggerService.getCronDescription(inputMap.get("expression")));
  }

  @GET
  @Path("webhook/parameters")
  @Timed
  @ExceptionMetered
  public RestResponse<WebhookParameters> listWebhookParameters(@QueryParam("appId") String appId,
      @QueryParam("workflowId") String workflowId, @QueryParam("workflowType") WorkflowType workflowType,
      @QueryParam("webhookSource") WebhookSource webhookSource, @QueryParam("eventType") WebhookEventType eventType) {
    return new RestResponse<>(
        triggerService.listWebhookParameters(appId, workflowId, workflowType, webhookSource, eventType));
  }

  @GET
  @Path("webhook/eventTypes")
  @Timed
  @ExceptionMetered
  public RestResponse<WebhookEventType> listWebhookEventTypes(@QueryParam("appId") String appId) {
    return new RestResponse<>();
  }

  @GET
  @Path("execution")
  @Timed
  @ExceptionMetered
  public RestResponse triggerExecution(
      @QueryParam("appId") String appId, @QueryParam("infraMappingId") String infraMappingId) {
    return new RestResponse(triggerService.triggerExecutionByServiceInfra(appId, infraMappingId));
  }
}
