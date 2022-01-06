/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import static io.harness.beans.SearchFilter.Operator.EQ;

import static software.wings.beans.CGConstants.GLOBAL_APP_ID;
import static software.wings.security.PermissionAttribute.ResourceType.NOTIFICATION_GROUP;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.rest.RestResponse;

import software.wings.beans.NotificationGroup;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.NotificationSetupService;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
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
@Scope(NOTIFICATION_GROUP)
public class NotificationSetupResource {
  private NotificationSetupService notificationSetupService;

  /**
   * Instantiates a new Notification setup resource.
   *
   * @param notificationSetupService the notification setup service
   */
  @Inject
  public NotificationSetupResource(NotificationSetupService notificationSetupService) {
    this.notificationSetupService = notificationSetupService;
  }

  /**
   * List.
   *
   * @param accountId   the account id
   * @param pageRequest the page request
   * @return the rest response
   */
  @GET
  @Path("notification-groups")
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<NotificationGroup>> listNotificationGroups(
      @QueryParam("accountId") String accountId, @BeanParam PageRequest<NotificationGroup> pageRequest) {
    if (pageRequest.getFilters() == null
        || pageRequest.getFilters().stream().noneMatch(
            searchFilter -> searchFilter.getFieldName().equals("accountId"))) {
      pageRequest.addFilter("accountId", EQ, accountId);
    }
    return new RestResponse<>(notificationSetupService.listNotificationGroups(pageRequest));
  }

  /**
   * Get.
   *
   * @param accountId           the account id
   * @param notificationGroupId the notificationGroupId
   * @return the rest response
   */
  @GET
  @Path("notification-groups/{notificationGroupId}")
  @Timed
  @ExceptionMetered
  public RestResponse<NotificationGroup> readNotificationGroup(
      @QueryParam("accountId") String accountId, @PathParam("notificationGroupId") String notificationGroupId) {
    return new RestResponse<>(notificationSetupService.readNotificationGroup(accountId, notificationGroupId));
  }

  /**
   * Create.
   *
   * @param accountId         the account id
   * @param notificationGroup the notificationGroup
   * @return the rest response
   */
  @POST
  @Path("notification-groups")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.MANAGE_ALERT_NOTIFICATION_RULES)
  public RestResponse<NotificationGroup> createNotificationGroups(
      @QueryParam("accountId") String accountId, NotificationGroup notificationGroup) {
    notificationGroup.setAccountId(accountId);
    notificationGroup.setAppId(GLOBAL_APP_ID);
    return new RestResponse<>(notificationSetupService.createNotificationGroup(notificationGroup));
  }

  /**
   * Update notification groups rest response.
   *
   * @param accountId         the account id
   * @param notificationGroup the notification group
   * @return the rest response
   */
  @PUT
  @Path("notification-groups/{notificationGroupId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.MANAGE_ALERT_NOTIFICATION_RULES)
  public RestResponse<NotificationGroup> updateNotificationGroups(@QueryParam("accountId") String accountId,
      @PathParam("notificationGroupId") String notificationGroupId, NotificationGroup notificationGroup) {
    notificationGroup.setAccountId(accountId);
    notificationGroup.setAppId(GLOBAL_APP_ID);
    notificationGroup.setUuid(notificationGroupId);
    return new RestResponse<>(notificationSetupService.updateNotificationGroup(notificationGroup));
  }

  /**
   * Delete notification groups rest response.
   *
   * @param accountId           the account id
   * @param notificationGroupId the notification group id
   * @return the rest response
   */
  @DELETE
  @Path("notification-groups/{notificationGroupId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.MANAGE_ALERT_NOTIFICATION_RULES)
  public RestResponse deleteNotificationGroups(
      @QueryParam("accountId") String accountId, @PathParam("notificationGroupId") String notificationGroupId) {
    notificationSetupService.deleteNotificationGroups(accountId, notificationGroupId);
    return new RestResponse<>();
  }
}
