package software.wings.graphql.datafetcher.execution;

import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.AbstractStatsDataFetcher;
import software.wings.graphql.schema.type.aggregation.QLAggregateFunction;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLDataPoint;
import software.wings.graphql.schema.type.aggregation.QLReference;
import software.wings.graphql.schema.type.aggregation.QLSinglePointData;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.deployment.QLDeploymentAggregation;
import software.wings.graphql.schema.type.aggregation.deployment.QLDeploymentFilter;

import java.util.List;

@Slf4j
public class ExecutionStatsDataFetcher extends AbstractStatsDataFetcher<QLAggregateFunction, QLDeploymentFilter,
    QLDeploymentAggregation, QLTimeSeriesAggregation> {
  @Override
  protected QLData fetch(QLAggregateFunction aggregateFunction, List<QLDeploymentFilter> filters,
      List<QLDeploymentAggregation> groupBy, QLTimeSeriesAggregation groupByTime) {
    return QLSinglePointData.builder()
        .dataPoint(QLDataPoint.builder()
                       .key(QLReference.builder().id("id1").name("name1").type("SERVICE").build())
                       .value(Integer.valueOf(100))
                       .build())
        .build();
  }
}
