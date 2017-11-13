package software.wings.resources;

import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import net.redhogs.cronparser.CronExpressionDescriptor;
import net.redhogs.cronparser.DescriptionTypeEnum;
import net.redhogs.cronparser.I18nMessages;
import net.redhogs.cronparser.Options;
import software.wings.beans.ErrorCode;
import software.wings.beans.RestResponse;
import software.wings.beans.WebHookToken;
import software.wings.beans.trigger.Trigger;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.exception.WingsException;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.TriggerService;
import software.wings.utils.Validator;

import java.text.ParseException;
import java.util.Map;
import javax.inject.Inject;
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
@AuthRule(APPLICATION)
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
    pageRequest.addFilter("appId", appId, EQ);
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
  @Path("{pipelineId}/webhook_token")
  @Timed
  @ExceptionMetered
  public RestResponse<WebHookToken> generateWebhookToken(
      @QueryParam("appId") String appId, @PathParam("pipelineId") String pipelineId) {
    return new RestResponse<>(triggerService.generateWebHookToken(appId, pipelineId));
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
    try {
      return new RestResponse<>(CronExpressionDescriptor.getDescription(
          DescriptionTypeEnum.FULL, inputMap.get("expression"), new Options(), I18nMessages.DEFAULT_LOCALE));
    } catch (ParseException e) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "Incorrect cron expression");
    }
  }
}
