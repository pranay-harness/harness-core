package software.wings.graphql.datafetcher.instance;

/*
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
*/