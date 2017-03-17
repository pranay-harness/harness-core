package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.beans.stats.DeploymentStatistics;
import software.wings.beans.stats.NotificationCount;
import software.wings.beans.stats.UserStatistics;
import software.wings.beans.stats.WingsStatistics;
import software.wings.service.intfc.StatisticsService;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by anubhaw on 8/15/16.
 */
@Api("/statistics")
@Path("/statistics")
@Produces("application/json")
@Timed
@ExceptionMetered
public class StatisticsResource {
  @Inject private StatisticsService statisticsService;

  /**
   * Top consumers rest response.
   *
   * @return the rest response
   */
  @GET
  @Path("top-consumers")
  public RestResponse<WingsStatistics> topConsumers(@QueryParam("accountId") String accountId) {
    return new RestResponse<>(statisticsService.getTopConsumers(accountId));
  }

  /**
   * User statistics rest response.
   *
   * @return the rest response
   */
  @GET
  @Path("user-stats/{accountId}")
  public RestResponse<UserStatistics> userStatistics(@PathParam("accountId") String accountId) {
    return new RestResponse<>(statisticsService.getUserStats(accountId));
  }

  @GET
  @Path("deployment-stats")
  public RestResponse<DeploymentStatistics> deploymentStats(@QueryParam("accountId") String accountId,
      @DefaultValue("30") @QueryParam("numOfDays") Integer numOfDays, @QueryParam("appId") String appId) {
    return new RestResponse<>(statisticsService.getDeploymentStatistics(accountId, appId, numOfDays));
  }

  @GET
  @Path("notification-count")
  public RestResponse<NotificationCount> notificationCount(@QueryParam("accountId") String accountId,
      @DefaultValue("60") @QueryParam("minutesFromNow") Integer minutesFromNow, @QueryParam("appId") String appId) {
    return new RestResponse<>(statisticsService.getNotificationCount(accountId, appId, minutesFromNow));
  }
}
