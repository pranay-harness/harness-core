package software.wings.graphql.datafetcher.execution;

import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.AbstractStatsDataFetcher;
import software.wings.graphql.schema.type.aggregation.QLAggregateFunction;
import software.wings.graphql.schema.type.aggregation.QLAggregatedData;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLDataPoint;
import software.wings.graphql.schema.type.aggregation.QLReference;
import software.wings.graphql.schema.type.aggregation.QLSinglePointData;
import software.wings.graphql.schema.type.aggregation.QLStackedData;
import software.wings.graphql.schema.type.aggregation.QLStackedDataPoint;
import software.wings.graphql.schema.type.aggregation.QLStackedTimeSeriesData;
import software.wings.graphql.schema.type.aggregation.QLStackedTimeSeriesDataPoint;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesData;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesDataPoint;
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
    int groupBySize = groupBy != null ? groupBy.size() : 0;
    if (groupBySize == 0) {
      return getSinglePointData();
    } else if (groupBySize == 1) {
      if (groupByTime == null) {
        return getAggregatedData();
      } else {
        return getTimeAggregatedData();
      }
    } else if (groupBySize == 2) {
      if (groupByTime == null) {
        return getStackedAggregatedData();
      } else {
        return getStackedTimeAggregatedData();
      }
    } else {
      return null;
    }
  }

  private QLData getStackedTimeAggregatedData() {
    QLDataPoint dataPoint1 = QLDataPoint.builder()
                                 .key(QLReference.builder().id("id1").name("name1").type("SERVICE").build())
                                 .value(Integer.valueOf(100))
                                 .build();
    QLDataPoint dataPoint2 = QLDataPoint.builder()
                                 .key(QLReference.builder().id("id2").name("name2").type("SERVICE").build())
                                 .value(Integer.valueOf(200))
                                 .build();
    QLStackedTimeSeriesDataPoint stackedTimeDataPoint1 = QLStackedTimeSeriesDataPoint.builder()
                                                             .time(System.currentTimeMillis() - 2 * 60 * 60 * 1000)
                                                             .values(Arrays.asList(dataPoint1, dataPoint2))
                                                             .build();

    QLDataPoint dataPoint3 = QLDataPoint.builder()
                                 .key(QLReference.builder().id("id3").name("name3").type("SERVICE").build())
                                 .value(Integer.valueOf(450))
                                 .build();
    QLDataPoint dataPoint4 = QLDataPoint.builder()
                                 .key(QLReference.builder().id("id4").name("name4").type("SERVICE").build())
                                 .value(Integer.valueOf(150))
                                 .build();
    QLDataPoint dataPoint5 = QLDataPoint.builder()
                                 .key(QLReference.builder().id("id5").name("name5").type("SERVICE").build())
                                 .value(Integer.valueOf(35))
                                 .build();
    QLStackedTimeSeriesDataPoint stackedTimeDataPoint2 = QLStackedTimeSeriesDataPoint.builder()
                                                             .time(System.currentTimeMillis() - 1 * 60 * 60 * 1000)
                                                             .values(Arrays.asList(dataPoint3, dataPoint4, dataPoint5))
                                                             .build();

    return QLStackedTimeSeriesData.builder()
        .label("label1")
        .data(Arrays.asList(stackedTimeDataPoint1, stackedTimeDataPoint2))
        .build();
  }

  private QLData getStackedAggregatedData() {
    QLDataPoint dataPoint1 = QLDataPoint.builder()
                                 .key(QLReference.builder().id("id1").name("name1").type("SERVICE").build())
                                 .value(Integer.valueOf(100))
                                 .build();
    QLDataPoint dataPoint2 = QLDataPoint.builder()
                                 .key(QLReference.builder().id("id2").name("name2").type("SERVICE").build())
                                 .value(Integer.valueOf(200))
                                 .build();
    QLStackedDataPoint stackedDataPoint1 =
        QLStackedDataPoint.builder()
            .key(QLReference.builder().id("id1").name("env1").type("ENVIRONMENT").build())
            .values(Arrays.asList(dataPoint1, dataPoint2))
            .build();

    QLDataPoint dataPoint3 = QLDataPoint.builder()
                                 .key(QLReference.builder().id("id3").name("name3").type("SERVICE").build())
                                 .value(Integer.valueOf(450))
                                 .build();
    QLDataPoint dataPoint4 = QLDataPoint.builder()
                                 .key(QLReference.builder().id("id4").name("name4").type("SERVICE").build())
                                 .value(Integer.valueOf(150))
                                 .build();
    QLDataPoint dataPoint5 = QLDataPoint.builder()
                                 .key(QLReference.builder().id("id5").name("name5").type("SERVICE").build())
                                 .value(Integer.valueOf(35))
                                 .build();
    QLStackedDataPoint stackedDataPoint2 =
        QLStackedDataPoint.builder()
            .key(QLReference.builder().id("id2").name("env2").type("ENVIRONMENT").build())
            .values(Arrays.asList(dataPoint3, dataPoint4, dataPoint5))
            .build();
    return QLStackedData.builder().dataPoints(Arrays.asList(stackedDataPoint1, stackedDataPoint2)).build();
  }

  private QLData getTimeAggregatedData() {
    QLTimeSeriesDataPoint dataPoint1 = QLTimeSeriesDataPoint.builder()
                                           .time(System.currentTimeMillis() - 3 * 60 * 60 * 1000)
                                           .data(Integer.valueOf(100))
                                           .build();
    QLTimeSeriesDataPoint dataPoint2 = QLTimeSeriesDataPoint.builder()
                                           .time(System.currentTimeMillis() - 2 * 60 * 60 * 1000)
                                           .data(Integer.valueOf(50))
                                           .build();
    QLTimeSeriesDataPoint dataPoint3 = QLTimeSeriesDataPoint.builder()
                                           .time(System.currentTimeMillis() - 1 * 60 * 60 * 1000)
                                           .data(Integer.valueOf(200))
                                           .build();
    return QLTimeSeriesData.builder()
        .dataPoints(Arrays.asList(dataPoint1, dataPoint2, dataPoint3))
        .label("label1")
        .build();
  }

  private QLData getAggregatedData() {
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

  private QLData getSinglePointData() {
    QLDataPoint dataPoint1 = QLDataPoint.builder()
                                 .key(QLReference.builder().id("id1").name("name1").type("SERVICE").build())
                                 .value(Integer.valueOf(100))
                                 .build();
    return QLSinglePointData.builder().dataPoint(dataPoint1).build();
  }
}
