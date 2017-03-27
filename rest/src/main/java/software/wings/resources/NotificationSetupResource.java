package software.wings.resources;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.NotificationGroup;
import software.wings.beans.NotificationRule;
import software.wings.beans.RestResponse;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.NotificationSetupService;

import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by rishi on 12/25/16.
 */
@Api("/notification-setup")
@Path("/notification-setup")
@Produces("application/json")
public class NotificationSetupResource {
  private NotificationSetupService notificationSetupService;

  @Inject
  public NotificationSetupResource(NotificationSetupService notificationSetupService) {
    this.notificationSetupService = notificationSetupService;
  }

  /**
   * List.
   *
   * @param appId       the app id
   * @param pageRequest the page request
   * @return the rest response
   */
  @GET
  @Path("notification-groups")
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<NotificationGroup>> listNotificationGroups(
      @QueryParam("appId") String appId, @BeanParam PageRequest<NotificationGroup> pageRequest) {
    return new RestResponse<>(notificationSetupService.listNotificationGroups(pageRequest));
  }

  /**
   * Get.
   *
   * @param appId       the app id
   * @param notificationGroupId the notificationGroupId
   * @return the rest response
   */
  @GET
  @Path("notification-groups/{notificationGroupId}")
  @Timed
  @ExceptionMetered
  public RestResponse<NotificationGroup> readNotificationGroup(
      @QueryParam("appId") String appId, @PathParam("notificationGroupId") String notificationGroupId) {
    return new RestResponse<>(notificationSetupService.readNotificationGroup(appId, notificationGroupId));
  }

  /**
   * Create.
   *
   * @param appId       the app id
   * @param notificationGroup       the notificationGroup
   * @return the rest response
   */
  @POST
  @Path("notification-groups")
  @Timed
  @ExceptionMetered
  public RestResponse<NotificationGroup> createNotificationGroups(
      @QueryParam("appId") String appId, NotificationGroup notificationGroup) {
    notificationGroup.setAppId(appId);
    return new RestResponse<>(notificationSetupService.createNotificationGroup(notificationGroup));
  }

  /**
   * List.
   *
   * @param appId       the app id
   * @param pageRequest the page request
   * @return the rest response
   */
  @GET
  @Path("notification-rules")
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<NotificationRule>> listNotificationRules(
      @QueryParam("appId") String appId, @BeanParam PageRequest<NotificationRule> pageRequest) {
    return new RestResponse<>(notificationSetupService.listNotificationRules(pageRequest));
  }

  /**
   * Get.
   *
   * @param appId       the app id
   * @param notificationRuleId the notificationRuleId
   * @return the rest response
   */
  @GET
  @Path("notification-rules/{notificationGroupId}")
  @Timed
  @ExceptionMetered
  public RestResponse<NotificationRule> readNotificationRule(
      @QueryParam("appId") String appId, @PathParam("notificationGroupId") String notificationRuleId) {
    return new RestResponse<>(notificationSetupService.readNotificationRule(appId, notificationRuleId));
  }

  /**
   * Create.
   *
   * @param appId       the app id
   * @param notificationRule       the notificationRule
   * @return the rest response
   */
  @POST
  @Path("notification-rules")
  @Timed
  @ExceptionMetered
  public RestResponse<NotificationRule> createNotificationRule(
      @QueryParam("appId") String appId, NotificationRule notificationRule) {
    notificationRule.setAppId(appId);
    return new RestResponse<>(notificationSetupService.createNotificationRule(notificationRule));
  }
}
