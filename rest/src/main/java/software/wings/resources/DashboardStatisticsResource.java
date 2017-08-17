package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.beans.stats.dashboard.InstanceStatsByService;
import software.wings.beans.stats.dashboard.InstanceSummaryStats;
import software.wings.beans.stats.dashboard.service.ServiceInstanceDashboard;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.DashboardStatisticsService;

import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * @author rktummala  on 8/11/17.
 */
@Api("dash-stats")
@Path("/dash-stats")
@Produces("application/json")
@AuthRule(ResourceType.APPLICATION)
public class DashboardStatisticsResource {
  @Inject private DashboardStatisticsService dashboardStatsService;

  /**
   * Get instance summary stats by given applications and group the results by the given entity types
   *
   * @return the rest response
   */
  @GET
  @Path("app-instance-summary-stats")
  @Timed
  @ExceptionMetered
  public RestResponse<InstanceSummaryStats> getAppInstanceSummaryStats(
      @QueryParam("appId") List<String> appIds, @QueryParam("groupBy") List<String> groupByEntityTypes) {
    return new RestResponse<>(dashboardStatsService.getAppInstanceSummaryStats(appIds, groupByEntityTypes));
  }

  /**
   * Get instance summary stats by given service and group the results by the given entity types
   *
   * @return the rest response
   */
  @GET
  @Path("service-instance-summary-stats")
  @Timed
  @ExceptionMetered
  public RestResponse<InstanceSummaryStats> getServiceInstanceSummaryStats(
      @QueryParam("serviceId") String serviceId, @QueryParam("groupBy") List<String> groupByEntityTypes) {
    return new RestResponse<>(dashboardStatsService.getServiceInstanceSummaryStats(serviceId, groupByEntityTypes));
  }

  /**
   * Get instance stats by given applications and group the results by the given entity types
   *
   * @return the rest response
   */
  @GET
  @Path("app-instance-stats")
  @Timed
  @ExceptionMetered
  public RestResponse<List<InstanceStatsByService>> getAppInstanceStats(@QueryParam("appId") List<String> appIds) {
    return new RestResponse<>(dashboardStatsService.getAppInstanceStats(appIds));
  }

  /**
   * Get instance stats by given applications and group the results by the given entity types
   *
   * @return the rest response
   */
  @GET
  @Path("service-instance-dash")
  @Timed
  @ExceptionMetered
  public RestResponse<ServiceInstanceDashboard> getServiceInstanceDashboard(@QueryParam("serviceId") String serviceId) {
    return new RestResponse<>(dashboardStatsService.getServiceInstanceDashboard(serviceId));
  }
}
