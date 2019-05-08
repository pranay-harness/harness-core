package software.wings.graphql.datafetcher.instance;

import static io.harness.govern.Switch.unhandled;

import com.google.inject.Inject;

import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.instance.dashboard.InstanceStatsUtil;
import software.wings.graphql.datafetcher.AbstractDataFetcher;
import software.wings.graphql.schema.query.QLInstancesCountQueryParameters;
import software.wings.graphql.schema.type.QLInstanceCount;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.instance.DashboardStatisticsService;
import software.wings.service.intfc.instance.stats.InstanceStatService;

import java.util.List;

@Slf4j
public class InstanceCountDataFetcher extends AbstractDataFetcher<QLInstanceCount, QLInstancesCountQueryParameters> {
  @Inject DashboardStatisticsService dashboardService;
  @Inject InstanceStatService instanceStatService;
  @Inject AppService appService;
  @Inject HPersistence persistence;

  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  public QLInstanceCount fetch(QLInstancesCountQueryParameters qlQuery) {
    switch (qlQuery.getInstanceCountType()) {
      case NINETY_FIVE_PERCENTILE:
        return QLInstanceCount.builder()
            .count((int) (InstanceStatsUtil.actualUsage(qlQuery.getAccountId(), instanceStatService)))
            .instanceCountType(qlQuery.getInstanceCountType())
            .build();

      case TOTAL:
        List<String> appIds = appService.getAppIdsByAccountId(qlQuery.getAccountId());
        return QLInstanceCount.builder()
            .count((int) (dashboardService.getTotalInstancesForAccount(qlQuery.getAccountId(), appIds)))
            .instanceCountType(qlQuery.getInstanceCountType())
            .build();

      default:
        unhandled(qlQuery.getInstanceCountType());
    }
    return null;
  }
}
