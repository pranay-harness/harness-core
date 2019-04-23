package software.wings.graphql.datafetcher.instance;

import com.google.inject.Inject;

import graphql.schema.DataFetchingEnvironment;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.instance.dashboard.InstanceStatsUtil;
import software.wings.graphql.datafetcher.AbstractDataFetcher;
import software.wings.graphql.schema.type.QLInstanceCount;
import software.wings.graphql.utils.GraphQLConstants;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.instance.DashboardStatisticsService;
import software.wings.service.intfc.instance.stats.InstanceStatService;

import java.util.List;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InstanceCountDataFetcher extends AbstractDataFetcher<QLInstanceCount> {
  public enum InstanceCountType { TOTAL, NINETY_FIVE_PERCENTILE }

  DashboardStatisticsService dashboardService;

  InstanceStatService instanceStatService;

  AppService appService;

  @Inject
  public InstanceCountDataFetcher(AuthHandler authHandler, DashboardStatisticsService dashboardService,
      InstanceStatService instanceStatService, AppService appService) {
    super(authHandler);
    this.dashboardService = dashboardService;
    this.instanceStatService = instanceStatService;
    this.appService = appService;
  }

  @Override
  public QLInstanceCount fetch(DataFetchingEnvironment environment) {
    String accountId = environment.getArgument(GraphQLConstants.ACCOUNT_ID_ARG);
    String countType = environment.getArgument(GraphQLConstants.INSTANCE_COUNT_TYPE_ARG);
    InstanceCountType instanceCountType = InstanceCountType.valueOf(countType);

    switch (instanceCountType) {
      case NINETY_FIVE_PERCENTILE:
        return QLInstanceCount.builder()
            .count((int) (InstanceStatsUtil.actualUsage(accountId, instanceStatService)))
            .instanceCountType(instanceCountType)
            .build();

      default:
        List<String> appIds = appService.getAppIdsByAccountId(accountId);
        return QLInstanceCount.builder()
            .count((int) (dashboardService.getTotalInstancesForAccount(accountId, appIds)))
            .instanceCountType(instanceCountType)
            .build();
    }
  }
}
