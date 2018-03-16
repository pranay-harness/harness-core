package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.beans.stats.AppKeyStatistics;
import software.wings.beans.stats.DeploymentStatistics;
import software.wings.beans.stats.NotificationCount;
import software.wings.beans.stats.ServiceInstanceStatistics;
import software.wings.beans.stats.UserStatistics;
import software.wings.beans.stats.WingsStatistics;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.StatisticsService;

import java.util.List;
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
@Scope(ResourceType.APPLICATION)
public class StatisticsResource {
  @Inject private StatisticsService statisticsService;

  /**
   * Top consumers rest response.
   *
   * @return the rest response
   */
  @GET
  @Path("top-consumers")
  @Timed
  @ExceptionMetered
  public RestResponse<WingsStatistics> topConsumers(
      @QueryParam("accountId") String accountId, @QueryParam("appId") List<String> appIds) {
    return new RestResponse<>(statisticsService.getTopConsumers(accountId, appIds));
  }

  /**
   * Top consumers rest response.
   *
   * @return the rest response
   */
  @GET
  @Path("top-consumers-services")
  @Timed
  @ExceptionMetered
  public RestResponse<WingsStatistics> topConsumerServices(
      @QueryParam("accountId") String accountId, @QueryParam("appId") List<String> appIds) {
    return new RestResponse<>(statisticsService.getTopConsumerServices(accountId, appIds));
  }

  /**
   * User statistics rest response.
   *
   * @return the rest response
   */
  @GET
  @Path("user-stats/{accountId}")
  @Timed
  @ExceptionMetered
  public RestResponse<UserStatistics> userStatistics(
      @PathParam("accountId") String accountId, @QueryParam("appId") List<String> appIds) {
    return new RestResponse<>(statisticsService.getUserStats(accountId, appIds));
  }

  @GET
  @Path("deployment-stats")
  @Timed
  @ExceptionMetered
  public RestResponse<DeploymentStatistics> deploymentStats(@QueryParam("accountId") String accountId,
      @DefaultValue("30") @QueryParam("numOfDays") Integer numOfDays, @QueryParam("appId") List<String> appIds) {
    return new RestResponse<>(statisticsService.getDeploymentStatistics(accountId, appIds, numOfDays));
  }

  @GET
  @Path("service-instance-stats")
  @Timed
  @ExceptionMetered
  public RestResponse<ServiceInstanceStatistics> instanceStats(@QueryParam("accountId") String accountId,
      @DefaultValue("30") @QueryParam("numOfDays") Integer numOfDays, @QueryParam("appId") List<String> appIds) {
    return new RestResponse<>(statisticsService.getServiceInstanceStatistics(accountId, appIds, numOfDays));
  }

  @GET
  @Path("notification-count")
  @Timed
  @ExceptionMetered
  public RestResponse<NotificationCount> notificationCount(@QueryParam("accountId") String accountId,
      @DefaultValue("60") @QueryParam("minutesFromNow") Integer minutesFromNow,
      @QueryParam("appId") List<String> appIds) {
    return new RestResponse<>(statisticsService.getNotificationCount(accountId, appIds, minutesFromNow));
  }

  @GET
  @Path("app-keystats")
  @Timed
  @ExceptionMetered
  public RestResponse<AppKeyStatistics> singleApplicationKeyStats(
      @QueryParam("appId") String appId, @DefaultValue("30") @QueryParam("numOfDays") Integer numOfDays) {
    return new RestResponse<>(statisticsService.getSingleApplicationKeyStats(appId, numOfDays));
  }
}
