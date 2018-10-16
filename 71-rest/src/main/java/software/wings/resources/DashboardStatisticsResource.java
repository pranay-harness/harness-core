package software.wings.resources;

import static java.util.stream.Collectors.toList;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.EntityType;
import software.wings.beans.RestResponse;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.stats.InstanceStatsSnapshot;
import software.wings.beans.instance.dashboard.InstanceStatsByService;
import software.wings.beans.instance.dashboard.InstanceSummaryStats;
import software.wings.beans.instance.dashboard.service.ServiceInstanceDashboard;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.instance.InstanceHelper;
import software.wings.service.intfc.instance.DashboardStatisticsService;
import software.wings.service.intfc.instance.stats.InstanceStatService;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * @author rktummala  on 8/11/17.
 */
@Api("dash-stats")
@Path("/dash-stats")
@Produces("application/json")
@Scope(ResourceType.APPLICATION)
public class DashboardStatisticsResource {
  private static final double DEFAULT_PERCENTILE = 95.0D;

  @Inject private DashboardStatisticsService dashboardStatsService;
  @Inject private InstanceHelper instanceHelper;
  @Inject private InstanceStatService instanceStatService;

  /**
   * Get instance summary stats by given applications and group the results by the given entity types
   *
   * @return the rest response
   */
  @GET
  @Path("app-instance-summary-stats")
  @Timed
  @ExceptionMetered
  public RestResponse<InstanceSummaryStats> getAppInstanceSummaryStats(@QueryParam("accountId") String accountId,
      @QueryParam("appId") List<String> appIds, @QueryParam("groupBy") List<String> groupByEntityTypes,
      @QueryParam("timestamp") long timestamp) {
    return new RestResponse<>(dashboardStatsService.getAppInstanceSummaryStats(appIds, groupByEntityTypes, timestamp));
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
  public RestResponse<InstanceSummaryStats> getServiceInstanceSummaryStats(@QueryParam("accountId") String accountId,
      @QueryParam("serviceId") String serviceId, @QueryParam("groupBy") List<String> groupByEntityTypes,
      @QueryParam("timestamp") long timestamp) {
    return new RestResponse<>(
        dashboardStatsService.getServiceInstanceSummaryStats(serviceId, groupByEntityTypes, timestamp));
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
  public RestResponse<List<InstanceStatsByService>> getAppInstanceStats(@QueryParam("accountId") String accountId,
      @QueryParam("appId") List<String> appIds, @QueryParam("timestamp") long timestamp) {
    return new RestResponse<>(dashboardStatsService.getAppInstanceStatsByService(appIds, timestamp));
  }

  /**
   * Get instance details for the given instance
   *
   * @return the rest response
   */
  @GET
  @Path("instance-details")
  @Timed
  @ExceptionMetered
  public RestResponse<Instance> getInstanceDetails(
      @QueryParam("accountId") String accountId, @QueryParam("instanceId") String instanceId) {
    return new RestResponse<>(dashboardStatsService.getInstanceDetails(instanceId));
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
  public RestResponse<ServiceInstanceDashboard> getServiceInstanceDashboard(@QueryParam("accountId") String accountId,
      @QueryParam("appId") String appId, @QueryParam("serviceId") String serviceId) {
    return new RestResponse<>(dashboardStatsService.getServiceInstanceDashboard(appId, serviceId));
  }

  /**
   * Manual sync request for all infra mappings for a given service and environment
   *
   * @return the rest response
   */
  @PUT
  @Path("manual-sync")
  @Scope(value = ResourceType.USER, scope = PermissionType.LOGGED_IN)
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  public RestResponse<String> manualSync(@QueryParam("accountId") String accountId, @QueryParam("appId") String appId,
      @QueryParam("inframappingId") String infraMappingId) {
    return new RestResponse<>(instanceHelper.manualSync(appId, infraMappingId));
  }

  /**
   * Get instance stats by given applications and group the results by the given entity types
   *
   * @return the rest response
   */
  @GET
  @Path("manual-sync-job")
  @Timed
  @ExceptionMetered
  public RestResponse<List<Boolean>> getManualSyncJobStatus(
      @QueryParam("accountId") String accountId, @QueryParam("jobs") Set<String> manualSyncJobIdSet) {
    return new RestResponse<>(instanceHelper.getManualSyncJobsStatus(manualSyncJobIdSet));
  }

  /**
   * Used to render timeline and provide hover information.
   */
  @GET
  @Path("timeline")
  @Timed
  @ExceptionMetered
  public RestResponse<InstanceTimeline> getInstanceStatsForGivenTime(@QueryParam("accountId") String accountId,
      @QueryParam("fromTsMillis") long fromTsMillis, @QueryParam("toTsMillis") long toTsMillis) {
    Instant from = Instant.ofEpochMilli(fromTsMillis);
    Instant to = Instant.ofEpochMilli(toTsMillis);

    List<InstanceStatsSnapshot> timeline = instanceStatService.aggregate(accountId, from, to);

    // this filtering can be done at DB level using $unwind and $aggregation but that would complicate what is
    // currently a simple find() query. So, keeping this logic at controller layer.
    List<InstanceStatsSnapshot> timelineWithOnlyApps =
        timeline.stream().map(snapshot -> filterEntities(snapshot, EntityType.APPLICATION)).collect(toList());

    return new RestResponse<>(InstanceTimeline.from(timelineWithOnlyApps));
  }

  @GET
  @Path("percentile")
  @Timed
  @ExceptionMetered
  public RestResponse<Map<String, Object>> percentile(@QueryParam("accountId") String accountId,
      @QueryParam("fromTsMillis") long fromTsMillis, @QueryParam("toTsMillis") long toTsMillis,
      @QueryParam("percentile") Double percentile) {
    Instant from = Instant.ofEpochMilli(fromTsMillis);
    Instant to = Instant.ofEpochMilli(toTsMillis);
    final double p = null != percentile ? percentile : DEFAULT_PERCENTILE;

    double percentileValue = instanceStatService.percentile(accountId, from, to, p);

    Map<String, Object> response = new HashMap<>();
    response.put("value", percentileValue);
    response.put("percentile", p);

    return new RestResponse<>(response);
  }

  /**
   * filters aggregations only for given entityType
   */
  private static InstanceStatsSnapshot filterEntities(InstanceStatsSnapshot snapshot, EntityType entityType) {
    return new InstanceStatsSnapshot(snapshot.getTimestamp(), snapshot.getAccountId(),
        snapshot.getAggregateCounts()
            .stream()
            .filter(ac -> ac.getEntityType() == EntityType.APPLICATION)
            .collect(toList()));
  }
}
