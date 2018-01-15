package software.wings.resources;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.util.Arrays.asList;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SortOrder.Builder.aSortOrder;
import static software.wings.beans.SortOrder.OrderType.ASC;
import static software.wings.beans.SortOrder.OrderType.DESC;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.Notification;
import software.wings.beans.NotificationAction.NotificationActionType;
import software.wings.beans.RestResponse;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.NotificationService;

import javax.ws.rs.BeanParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by anubhaw on 7/22/16.
 */
@Api("/notifications")
@Path("/notifications")
@Produces("application/json")
@AuthRule(ResourceType.APPLICATION)
public class NotificationResource {
  @Inject private NotificationService notificationService;

  /**
   * List rest response.
   *
   * @param appId       the app id
   * @param pageRequest the page request
   * @return the rest response
   */
  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<Notification>> list(@QueryParam("appId") String appId,
      @BeanParam PageRequest<Notification> pageRequest, @QueryParam("accountId") @NotEmpty String accountId) {
    if (!isEmpty(appId)) {
      pageRequest.addFilter("appId", appId, EQ);
    }
    pageRequest.setOrders(
        asList(aSortOrder().withField("complete", ASC).build(), aSortOrder().withField("createdAt", DESC).build()));
    return new RestResponse<>(notificationService.list(pageRequest));
  }

  /**
   * Get rest response.
   *
   * @param appId          the app id
   * @param notificationId the notification id
   * @return the rest response
   */
  @GET
  @Path("{notificationId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Notification> get(@DefaultValue(GLOBAL_APP_ID) @QueryParam("appId") String appId,
      @QueryParam("notificationId") String notificationId) {
    return new RestResponse<>(notificationService.get(appId, notificationId));
  }

  /**
   * Update rest response.
   *
   * @param appId          the app id
   * @param notificationId the notification id
   * @param actionType     the action type
   * @return the rest response
   */
  @POST
  @Path("{notificationId}/action/{type}")
  @Timed
  @ExceptionMetered
  public RestResponse<Notification> act(@DefaultValue(GLOBAL_APP_ID) @QueryParam("appId") String appId,
      @PathParam("notificationId") String notificationId, @PathParam("type") NotificationActionType actionType) {
    return new RestResponse<>(notificationService.act(appId, notificationId, actionType));
  }
}
