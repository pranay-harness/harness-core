/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import static io.harness.annotations.dev.HarnessTeam.DX;

import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.PageResponse;
import io.harness.rest.RestResponse;

import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.instance.dashboard.InstanceStatsByEnvironment;
import software.wings.beans.instance.dashboard.InstanceStatsByService;
import software.wings.beans.instance.dashboard.InstanceSummaryStats;
import software.wings.beans.instance.dashboard.InstanceSummaryStatsByService;
import software.wings.beans.instance.dashboard.service.ServiceInstanceDashboard;
import software.wings.resources.stats.model.InstanceTimeline;
import software.wings.resources.stats.model.TimeRange;
import software.wings.resources.stats.service.TimeRangeProvider;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.instance.CompareEnvironmentAggregationResponseInfo;
import software.wings.service.impl.instance.InstanceHelper;
import software.wings.service.intfc.instance.DashboardStatisticsService;
import software.wings.service.intfc.instance.stats.InstanceStatService;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.DefaultValue;
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
@OwnedBy(DX)
public class DashboardStatisticsResource {
  public static final double DEFAULT_PERCENTILE = 95.0D;

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
    return new RestResponse<>(
        dashboardStatsService.getAppInstanceSummaryStats(accountId, appIds, groupByEntityTypes, timestamp));
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
        dashboardStatsService.getServiceInstanceSummaryStats(accountId, serviceId, groupByEntityTypes, timestamp));
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
    return new RestResponse<>(dashboardStatsService.getAppInstanceStatsByService(accountId, appIds, timestamp));
  }

  /**
   * Get instance stats for the given service
   *
   * @return the rest response
   */
  @GET
  @Path("service-instance-stats")
  @Timed
  @ExceptionMetered
  public RestResponse<List<InstanceStatsByEnvironment>> getServiceInstanceStats(
      @QueryParam("accountId") String accountId, @QueryParam("serviceId") String serviceId,
      @QueryParam("timestamp") long timestamp) {
    return new RestResponse<>(dashboardStatsService.getServiceInstances(accountId, serviceId, timestamp));
  }

  /**
   * Get instance stats by given applications and group the results by the given entity types
   *
   * @return the rest response
   */
  @GET
  @Path("app-instance-count-stats")
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<InstanceSummaryStatsByService>> getAppInstanceCountStats(
      @QueryParam("accountId") String accountId, @QueryParam("appId") List<String> appIds,
      @QueryParam("timestamp") long timestamp, @QueryParam("offset") @DefaultValue("-1") int offset,
      @QueryParam("limit") @DefaultValue("-1") int limit) {
    return new RestResponse<>(
        dashboardStatsService.getAppInstanceSummaryStatsByService(accountId, appIds, timestamp, offset, limit));
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
  @AuthRule(permissionType = PermissionType.SERVICE, action = Action.READ)
  public RestResponse<ServiceInstanceDashboard> getServiceInstanceDashboard(@QueryParam("accountId") String accountId,
      @QueryParam("appId") String appId, @QueryParam("serviceId") String serviceId) {
    return new RestResponse<>(dashboardStatsService.getServiceInstanceDashboard(accountId, appId, serviceId));
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
    return new RestResponse<>(instanceHelper.getManualSyncJobsStatus(accountId, manualSyncJobIdSet));
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
    return new RestResponse<>(instanceStatService.aggregate(accountId, fromTsMillis, toTsMillis));
  }

  /**
   * Used to provide dropdown information for instance history graph.
   */
  @GET
  @Path("instance-history-ranges")
  @Timed
  @ExceptionMetered
  public RestResponse<List<TimeRange>> getTimeRanges(@QueryParam("accountId") String accountId) {
    Instant firstTs = instanceStatService.getFirstSnapshotTime(accountId);
    if (null == firstTs) {
      return new RestResponse<>(Collections.emptyList());
    }

    List<TimeRange> ranges = new TimeRangeProvider(ZoneOffset.UTC)
                                 .monthlyRanges(firstTs, Instant.now())
                                 .stream()
                                 .sorted(Comparator.comparing(TimeRange::getFrom).reversed())
                                 .limit(6)
                                 .collect(toList());

    return new RestResponse<>(ranges);
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

  @GET
  @Path("service-compare-environment")
  @ExceptionMetered
  public RestResponse<PageResponse<CompareEnvironmentAggregationResponseInfo>> getCompareServicesByEnvironment(
      @QueryParam("accountId") String accountId, @QueryParam("appId") String appId, @QueryParam("envId1") String envId1,
      @QueryParam("envId2") String envId2, @QueryParam("offset") @DefaultValue("-1") int offset,
      @QueryParam("limit") @DefaultValue("-1") int limit) {
    return new RestResponse<>(
        dashboardStatsService.getCompareServicesByEnvironment(accountId, appId, envId1, envId2, offset, limit));
  }
}
