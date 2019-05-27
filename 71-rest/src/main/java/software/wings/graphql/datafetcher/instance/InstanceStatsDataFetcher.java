package software.wings.graphql.datafetcher.instance;

import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.AbstractStatsDataFetcher;
import software.wings.graphql.schema.type.aggregation.QLAggregateFunction;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLDataPoint;
import software.wings.graphql.schema.type.aggregation.QLReference;
import software.wings.graphql.schema.type.aggregation.QLSinglePointData;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.instance.QLInstanceAggregation;
import software.wings.graphql.schema.type.aggregation.instance.QLInstanceFilter;

import java.util.List;

@Slf4j
public class InstanceStatsDataFetcher extends AbstractStatsDataFetcher<QLAggregateFunction, QLInstanceFilter,
    QLInstanceAggregation, QLTimeSeriesAggregation> {
  @Override
  protected QLData fetch(QLAggregateFunction aggregateFunction, List<QLInstanceFilter> filters,
      List<QLInstanceAggregation> groupBy, QLTimeSeriesAggregation groupByTime) {
    return QLSinglePointData.builder()
        .dataPoint(QLDataPoint.builder()
                       .key(QLReference.builder().id("id1").name("name1").type("SERVICE").build())
                       .value(Integer.valueOf(100))
                       .build())
        .build();
  }
}
