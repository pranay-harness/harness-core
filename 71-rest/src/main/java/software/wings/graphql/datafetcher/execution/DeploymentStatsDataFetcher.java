package software.wings.graphql.datafetcher.execution;

import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.AbstractStatsDataFetcher;
import software.wings.graphql.schema.type.aggregation.QLAggregateFunction;
import software.wings.graphql.schema.type.aggregation.QLAggregatedData;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLDataPoint;
import software.wings.graphql.schema.type.aggregation.QLReference;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.deployment.QLDeploymentAggregation;
import software.wings.graphql.schema.type.aggregation.deployment.QLDeploymentFilter;

import java.util.Arrays;
import java.util.List;

@Slf4j
public class DeploymentStatsDataFetcher extends AbstractStatsDataFetcher<QLAggregateFunction, QLDeploymentFilter,
    QLDeploymentAggregation, QLTimeSeriesAggregation> {
  @Override
  protected QLData fetch(QLAggregateFunction aggregateFunction, List<QLDeploymentFilter> filters,
      List<QLDeploymentAggregation> groupBy, QLTimeSeriesAggregation groupByTime) {
    QLDataPoint dataPoint1 = QLDataPoint.builder()
                                 .key(QLReference.builder().id("id1").name("name1").type("SERVICE").build())
                                 .value(Integer.valueOf(100))
                                 .build();
    QLDataPoint dataPoint2 = QLDataPoint.builder()
                                 .key(QLReference.builder().id("id2").name("name2").type("SERVICE").build())
                                 .value(Integer.valueOf(200))
                                 .build();
    QLDataPoint dataPoint3 = QLDataPoint.builder()
                                 .key(QLReference.builder().id("id3").name("name3").type("SERVICE").build())
                                 .value(Integer.valueOf(300))
                                 .build();
    QLDataPoint dataPoint4 = QLDataPoint.builder()
                                 .key(QLReference.builder().id("id4").name("name4").type("SERVICE").build())
                                 .value(Integer.valueOf(400))
                                 .build();
    return QLAggregatedData.builder().dataPoints(Arrays.asList(dataPoint1, dataPoint2, dataPoint3, dataPoint4)).build();
  }
}
