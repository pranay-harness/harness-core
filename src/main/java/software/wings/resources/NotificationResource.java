package software.wings.resources;

import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.SearchFilter.Operator.EQ;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.Notification;
import software.wings.beans.NotificationAction;
import software.wings.beans.RestResponse;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.NotificationService;

import javax.ws.rs.BeanParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by anubhaw on 7/22/16.
 */
@Api("/notifications")
@Path("/notifications")
@AuthRule
@Produces("application/json")
@Timed
@ExceptionMetered
public class NotificationResource {
  @Inject private NotificationService notificationService;

  @GET
  public RestResponse<PageResponse<Notification>> list(@DefaultValue(GLOBAL_APP_ID) @QueryParam("appId") String appId,
      @BeanParam PageRequest<Notification> pageRequest) {
    pageRequest.addFilter("appId", appId, EQ);
    return new RestResponse<>(notificationService.list(pageRequest));
  }

  @GET
  @Path("{notificationId}")
  public RestResponse<Notification> get(@DefaultValue(GLOBAL_APP_ID) @QueryParam("appId") String appId,
      @QueryParam("notificationId") String notificationId) {
    return new RestResponse<>(notificationService.get(appId, notificationId));
  }

  @PUT
  @Path("{notificationId}")
  public RestResponse<Notification> update(@DefaultValue(GLOBAL_APP_ID) @QueryParam("appId") String appId,
      @QueryParam("notificationId") String notificationId, Notification notification) {
    notification.setAppId(appId);
    notification.setUuid(notificationId);
    return new RestResponse<>(notificationService.update(notification));
  }

  @PUT
  @Path("{notificationId}/action/{notificationAction}")
  public RestResponse<Notification> action(@DefaultValue(GLOBAL_APP_ID) @QueryParam("appId") String appId,
      @QueryParam("notificationId") String notificationId,
      @QueryParam("notificationAction") NotificationAction notificationAction) {
    return new RestResponse<>(notificationService.act(appId, notificationId, notificationAction));
  }
}
