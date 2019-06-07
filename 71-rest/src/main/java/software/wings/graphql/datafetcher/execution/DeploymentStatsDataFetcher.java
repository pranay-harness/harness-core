package software.wings.graphql.datafetcher.execution;

import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.AbstractStatsDataFetcher;
import software.wings.graphql.schema.type.aggregation.QLAggregateFunction;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.deployment.QLDeploymentAggregation;
import software.wings.graphql.schema.type.aggregation.deployment.QLDeploymentFilter;

import java.util.List;

@Slf4j
public class DeploymentStatsDataFetcher extends AbstractStatsDataFetcher<QLAggregateFunction, QLDeploymentFilter,
    QLDeploymentAggregation, QLTimeSeriesAggregation> {
  @Override
  protected QLData fetch(String accountId, QLAggregateFunction aggregateFunction, List<QLDeploymentFilter> filters,
      List<QLDeploymentAggregation> groupBy, QLTimeSeriesAggregation groupByTime) {
    int groupBySize = groupBy != null ? groupBy.size() : 0;
    if (groupBySize == 0) {
      return StatsStubDataHelper.getSinglePointData();
    } else if (groupBySize == 1) {
      if (groupByTime == null) {
        return StatsStubDataHelper.getAggregatedData();
      } else {
        return StatsStubDataHelper.getTimeAggregatedData();
      }
    } else if (groupBySize == 2) {
      if (groupByTime == null) {
        return StatsStubDataHelper.getStackedAggregatedData();
      } else {
        return StatsStubDataHelper.getStackedTimeAggregatedData();
      }
    } else {
      return null;
    }
  }
}
