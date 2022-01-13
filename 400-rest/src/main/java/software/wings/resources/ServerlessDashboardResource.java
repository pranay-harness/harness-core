/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;

import static java.util.stream.Collectors.toList;

import io.harness.beans.PageResponse;
import io.harness.rest.RestResponse;

import software.wings.beans.infrastructure.instance.InvocationCount.InvocationCountKey;
import software.wings.beans.infrastructure.instance.ServerlessInstance;
import software.wings.beans.instance.dashboard.InstanceStatsByEnvironment;
import software.wings.beans.instance.dashboard.InstanceSummaryStats;
import software.wings.beans.instance.dashboard.InstanceSummaryStatsByService;
import software.wings.resources.stats.model.ServerlessInstanceTimeline;
import software.wings.resources.stats.model.TimeRange;
import software.wings.resources.stats.service.TimeRangeProvider;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.instance.InstanceHelper;
import software.wings.service.intfc.instance.ServerlessDashboardService;
import software.wings.service.intfc.instance.stats.ServerlessInstanceStatService;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.extern.slf4j.Slf4j;

@Api("serverless-dashboard")
@Path("/serverless-dashboard")
@Produces("application/json")
@Scope(ResourceType.APPLICATION)
@Slf4j
public class ServerlessDashboardResource {
  @Inject private ServerlessDashboardService serverlessDashboardService;
  @Inject private InstanceHelper instanceHelper;
  @Inject private ServerlessInstanceStatService serverlessInstanceStatService;

  @GET
  @Path("app-instance-summary-stats")
  @Timed
  @ExceptionMetered
  public RestResponse<InstanceSummaryStats> getAppInstanceSummaryStats(@QueryParam("accountId") String accountId,
      @QueryParam("appId") List<String> appIds, @QueryParam("groupBy") List<String> groupByEntityTypes,
      @QueryParam("timestamp") long timestamp) {
    return new RestResponse<>(
        serverlessDashboardService.getAppInstanceSummaryStats(accountId, appIds, groupByEntityTypes, timestamp));
  }

  @GET
  @Path("app-instance-count-stats")
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<InstanceSummaryStatsByService>> getAppInstanceCountStats(
      @QueryParam("accountId") String accountId, @QueryParam("appId") List<String> appIds,
      @QueryParam("timestamp") long timestamp, @QueryParam("offset") @DefaultValue("-1") int offset,
      @QueryParam("limit") @DefaultValue("-1") int limit) {
    return new RestResponse<>(
        serverlessDashboardService.getAppInstanceSummaryStatsByService(accountId, appIds, timestamp, offset, limit));
  }

  @GET
  @Path("service-instance-stats")
  @Timed
  @ExceptionMetered
  public RestResponse<List<InstanceStatsByEnvironment>> getServiceInstanceStats(
      @QueryParam("accountId") String accountId, @QueryParam("serviceId") String serviceId,
      @QueryParam("timestamp") long timestamp) {
    return new RestResponse<>(serverlessDashboardService.getServiceInstances(accountId, serviceId, timestamp));
  }

  @GET
  @Path("instance-details")
  @Timed
  @ExceptionMetered
  public RestResponse<ServerlessInstance> getInstanceDetails(
      @QueryParam("accountId") String accountId, @QueryParam("instanceId") String instanceId) {
    return new RestResponse<>(serverlessDashboardService.getInstanceDetails(instanceId));
  }

  @PUT
  @Path("manual-sync")
  @Scope(value = ResourceType.USER, scope = LOGGED_IN)
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<String> manualSyncServerlessInfraMapping(@QueryParam("accountId") String accountId,
      @QueryParam("appId") String appId, @QueryParam("inframappingId") String infraMappingId) {
    return new RestResponse<>(instanceHelper.manualSync(appId, infraMappingId));
  }

  @GET
  @Path("instance-history-ranges")
  @Timed
  @ExceptionMetered
  public RestResponse<List<TimeRange>> getTimeRanges(@QueryParam("accountId") String accountId) {
    Instant firstTs = serverlessInstanceStatService.getFirstSnapshotTime(accountId);
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
  @Path("timeline")
  @Timed
  @ExceptionMetered
  public RestResponse<ServerlessInstanceTimeline> getInstanceStatsForGivenTime(
      @QueryParam("accountId") String accountId, @QueryParam("fromTsMillis") long fromTsMillis,
      @QueryParam("toTsMillis") long toTsMillis,
      @QueryParam("invocation_count_key") @DefaultValue(value = "LAST_30_DAYS") InvocationCountKey invocationCountKey) {
    return new RestResponse<>(
        serverlessInstanceStatService.aggregate(accountId, fromTsMillis, toTsMillis, invocationCountKey));
  }

  @GET
  @Path("manual-sync-job")
  @Timed
  @ExceptionMetered
  public RestResponse<List<Boolean>> getManualSyncJobStatus(
      @QueryParam("accountId") String accountId, @QueryParam("jobs") Set<String> manualSyncJobIdSet) {
    return new RestResponse<>(instanceHelper.getManualSyncJobsStatus(accountId, manualSyncJobIdSet));
  }
}
