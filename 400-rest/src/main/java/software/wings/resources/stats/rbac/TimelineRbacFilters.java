/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources.stats.rbac;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static com.google.common.collect.Sets.union;
import static java.util.Collections.emptySet;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.EntityType;
import software.wings.beans.User;
import software.wings.beans.infrastructure.instance.stats.InstanceStatsSnapshot;
import software.wings.beans.infrastructure.instance.stats.InstanceStatsSnapshot.AggregateCount;
import software.wings.beans.infrastructure.instance.stats.ServerlessInstanceStats;
import software.wings.beans.infrastructure.instance.stats.ServerlessInstanceStats.AggregateInvocationCount;
import software.wings.resources.stats.model.InstanceTimeline;
import software.wings.resources.stats.model.InstanceTimeline.Aggregate;
import software.wings.resources.stats.model.InstanceTimeline.DataPoint;
import software.wings.security.UserRequestContext;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.UserService;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.SetUtils;

@OwnedBy(PL)
@Slf4j
public class TimelineRbacFilters {
  @Nonnull private User currentUser;
  @Nonnull private String accountId;
  private AppService appService;
  private UserService userService;

  public TimelineRbacFilters(
      @Nonnull User currentUser, @Nonnull String accountId, AppService appService, UserService userService) {
    this.currentUser = currentUser;
    this.accountId = accountId;
    this.appService = appService;
    this.userService = userService;
  }

  /**
   * This method filters the stats for the allowed applications and also include
   */
  public List<InstanceStatsSnapshot> filter(List<InstanceStatsSnapshot> stats, Set<String> deletedAppIds) {
    boolean includeDeletedAppIds = false;

    if (userService.isAccountAdmin(accountId)) {
      includeDeletedAppIds = true;
    }

    UserRequestContext userRequestContext = currentUser.getUserRequestContext();

    // appId filter not required
    if (!userRequestContext.isAppIdFilterRequired()) {
      return stats;
    }

    final Set<String> allowedAppIds = getAssignedApps(currentUser);
    log.info("Allowed App Ids. Account: {} User: {} Ids: {}, includeDeletedAppIds: {}", accountId,
        currentUser.getEmail(), allowedAppIds, includeDeletedAppIds);
    final Set<String> allowedAppIdsFinal = Sets.newHashSet(allowedAppIds);
    if (includeDeletedAppIds) {
      allowedAppIdsFinal.addAll(deletedAppIds);
      log.info("Deleted App Ids. Account: {} User: {} Ids: {}", accountId, currentUser.getEmail(), deletedAppIds);
    }

    return stats.stream()
        .map(it
            -> new InstanceStatsSnapshot(
                it.getTimestamp(), it.getAccountId(), filterAggregates(it.getAggregateCounts(), allowedAppIdsFinal)))
        .collect(Collectors.toList());
  }

  public List<ServerlessInstanceStats> filterServerlessStats(
      List<ServerlessInstanceStats> serverlessInstanceStats, Set<String> deletedAppIds) {
    boolean includeDeletedAppIds = userService.isAccountAdmin(accountId);
    UserRequestContext userRequestContext = currentUser.getUserRequestContext();

    if (!userRequestContext.isAppIdFilterRequired()) {
      return serverlessInstanceStats;
    }

    final Set<String> allowedAppIds = getAssignedApps(currentUser);
    log.info("Allowed App Ids. Account: {} User: {} Ids: {}, includeDeletedAppIds: {}", accountId,
        currentUser.getEmail(), allowedAppIds, includeDeletedAppIds);
    log.info("Deleted App Ids. Account: {} User: {} Ids: {}", accountId, currentUser.getEmail(), deletedAppIds);

    final Set<String> allowedAppIdsFinal =
        union(allowedAppIds, includeDeletedAppIds ? SetUtils.emptyIfNull(deletedAppIds) : emptySet());

    return serverlessInstanceStats.stream()
        .map(it
            -> new ServerlessInstanceStats(it.getTimestamp(), it.getAccountId(),
                filterServerlessAggregates(it.getAggregateCounts(), allowedAppIdsFinal)))
        .collect(Collectors.toList());
  }

  // only show allowed appIds in aggregates
  private static List<AggregateCount> filterAggregates(List<AggregateCount> aggregates, Set<String> allowedAppIds) {
    List<AggregateCount> nonAppAggregates =
        aggregates.stream().filter(it -> it.getEntityType() != EntityType.APPLICATION).collect(Collectors.toList());

    List<AggregateCount> filteredAppAggregates = aggregates.stream()
                                                     .filter(it -> it.getEntityType() == EntityType.APPLICATION)
                                                     .filter(it -> allowedAppIds.contains(it.getId()))
                                                     .collect(Collectors.toList());

    List<AggregateCount> aggregateCounts = new ArrayList<>(nonAppAggregates);
    aggregateCounts.addAll(filteredAppAggregates);
    return aggregateCounts;
  }

  private static Collection<AggregateInvocationCount> filterServerlessAggregates(
      Collection<AggregateInvocationCount> aggregates, Set<String> allowedAppIds) {
    return aggregates.stream()
        .filter(it -> it.getEntityType() != EntityType.APPLICATION || allowedAppIds.contains(it.getId()))
        .collect(Collectors.toList());
  }

  /**
   * Remove deleted appIds from aggregates when user is not admin.
   * See {@link #removeDeletedEntities} method.
   *
   * @param timeline
   * @return updated timeline
   */
  public InstanceTimeline removeDeletedApps(InstanceTimeline timeline) {
    List<DataPoint> updatedPoints =
        timeline.getPoints().stream().map(TimelineRbacFilters::removeDeletedEntities).collect(Collectors.toList());

    return new InstanceTimeline(updatedPoints);
  }

  private static DataPoint removeDeletedEntities(DataPoint datapoint) {
    List<Aggregate> aggregates = datapoint.getAggregateCounts();
    List<Aggregate> filteredAggregates = new LinkedList<>();

    int count = 0;
    for (Aggregate aggregate : aggregates) {
      if (aggregate.isEntityDeleted()) {
        continue;
      }

      filteredAggregates.add(aggregate);
      count = count + aggregate.getCount();
    }

    return new DataPoint(datapoint.getTimestamp(), datapoint.getAccountId(), filteredAggregates, count);
  }

  // get allowed AppIds according to RBAC rules
  private Set<String> getAssignedApps(User user) {
    UserRequestContext userRequestContext = currentUser.getUserRequestContext();

    Set<String> allowedAppIds = userRequestContext.getAppIds();
    if (isEmpty(allowedAppIds)) {
      log.info("No apps assigned for user. User: {}, Account: {}", user.getEmail(), accountId);
      return emptySet();
    }

    return allowedAppIds;
  }
}
