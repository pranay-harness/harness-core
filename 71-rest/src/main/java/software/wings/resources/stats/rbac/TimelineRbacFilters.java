package software.wings.resources.stats.rbac;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.eraro.ErrorCode.NO_APPS_ASSIGNED;

import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.EntityType;
import software.wings.beans.User;
import software.wings.beans.infrastructure.instance.stats.InstanceStatsSnapshot;
import software.wings.beans.infrastructure.instance.stats.InstanceStatsSnapshot.AggregateCount;
import software.wings.resources.stats.model.InstanceTimeline;
import software.wings.resources.stats.model.InstanceTimeline.Aggregate;
import software.wings.resources.stats.model.InstanceTimeline.DataPoint;
import software.wings.security.UserRequestContext;
import software.wings.security.UserRequestInfo;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.UsageRestrictionsService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

public class TimelineRbacFilters {
  private static final Logger log = LoggerFactory.getLogger(TimelineRbacFilters.class);

  @Nonnull private User currentUser;
  @Nonnull private String accountId;
  private AppService appService;
  private UsageRestrictionsService usageRestrictionsService;

  public TimelineRbacFilters(@Nonnull User currentUser, @Nonnull String accountId, AppService appService,
      UsageRestrictionsService usageRestrictionsService) {
    this.currentUser = currentUser;
    this.accountId = accountId;
    this.appService = appService;
    this.usageRestrictionsService = usageRestrictionsService;
  }

  /**
   * remove apps from aggregateCounts for which user does not have permissions
   */
  public List<InstanceStatsSnapshot> filter(List<InstanceStatsSnapshot> stats) {
    // No filtering for admins
    if (usageRestrictionsService.isAccountAdmin(accountId)) {
      return stats;
    }

    UserRequestContext userRequestContext = currentUser.getUserRequestContext();

    // appId filter not required
    if (!userRequestContext.isAppIdFilterRequired()) {
      return stats;
    }

    Set<String> allowedAppIds = getAssignedApps(currentUser);
    log.info("Allowed App Ids. Account: {} User: {} Ids: {}", accountId, currentUser.getEmail(), allowedAppIds);
    return stats.stream()
        .map(it
            -> new InstanceStatsSnapshot(
                it.getTimestamp(), it.getAccountId(), filterAggregates(it.getAggregateCounts(), allowedAppIds)))
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

  /**
   * Remove deleted appIds from aggregates when user is not admin.
   * See {@link #removeDeletedEntities} method.
   *
   * @param timeline
   * @return updated timeline
   */
  public InstanceTimeline removeDeletedApps(InstanceTimeline timeline) {
    if (usageRestrictionsService.isAccountAdmin(accountId)) {
      return timeline;
    }

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
    if (user.isUseNewRbac()) {
      UserRequestContext userRequestContext = currentUser.getUserRequestContext();

      Set<String> allowedAppIds = userRequestContext.getAppIds();
      if (isEmpty(allowedAppIds)) {
        log.info("No apps assigned for user. User: {}, Account: {}", user.getEmail(), accountId);
        return Collections.emptySet();
      }

      return allowedAppIds;
    } else {
      UserRequestInfo userRequestInfo = user.getUserRequestInfo();
      if (userRequestInfo == null) {
        throw new WingsException(NO_APPS_ASSIGNED);
      }

      Set<String> allowedAppIds;
      if (userRequestInfo.isAllAppsAllowed()) {
        allowedAppIds = new HashSet<>(userRequestInfo.getAllowedAppIds());
      } else {
        allowedAppIds = new HashSet<>(appService.getAppIdsByAccountId(userRequestInfo.getAccountId()));
      }

      if (isEmpty(allowedAppIds)) {
        throw new WingsException(NO_APPS_ASSIGNED);
      }

      return allowedAppIds;
    }
  }
}