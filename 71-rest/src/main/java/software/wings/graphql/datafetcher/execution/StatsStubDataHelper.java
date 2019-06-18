package software.wings.graphql.datafetcher.execution;

import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.schema.type.aggregation.QLAggregatedData;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLDataPoint;
import software.wings.graphql.schema.type.aggregation.QLReference;
import software.wings.graphql.schema.type.aggregation.QLSinglePointData;
import software.wings.graphql.schema.type.aggregation.QLStackedData;
import software.wings.graphql.schema.type.aggregation.QLStackedDataPoint;
import software.wings.graphql.schema.type.aggregation.QLStackedTimeSeriesData;
import software.wings.graphql.schema.type.aggregation.QLStackedTimeSeriesDataPoint;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesData;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesDataPoint;

import java.util.Arrays;

@Slf4j
public class StatsStubDataHelper {
  public static QLData getStackedTimeAggregatedData() {
    QLDataPoint dataPoint1 = QLDataPoint.builder()
                                 .key(QLReference.builder().id("id1").name("name1").type("SERVICEID").build())
                                 .value(Integer.valueOf(100))
                                 .build();
    QLDataPoint dataPoint2 = QLDataPoint.builder()
                                 .key(QLReference.builder().id("id2").name("name2").type("SERVICEID").build())
                                 .value(Integer.valueOf(200))
                                 .build();
    QLStackedTimeSeriesDataPoint stackedTimeDataPoint1 = QLStackedTimeSeriesDataPoint.builder()
                                                             .time(System.currentTimeMillis() - 2 * 60 * 60 * 1000)
                                                             .values(Arrays.asList(dataPoint1, dataPoint2))
                                                             .build();

    QLDataPoint dataPoint3 = QLDataPoint.builder()
                                 .key(QLReference.builder().id("id3").name("name3").type("SERVICEID").build())
                                 .value(Integer.valueOf(450))
                                 .build();
    QLDataPoint dataPoint4 = QLDataPoint.builder()
                                 .key(QLReference.builder().id("id4").name("name4").type("SERVICEID").build())
                                 .value(Integer.valueOf(150))
                                 .build();
    QLDataPoint dataPoint5 = QLDataPoint.builder()
                                 .key(QLReference.builder().id("id5").name("name5").type("SERVICEID").build())
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

  public static QLData getStackedAggregatedData() {
    QLDataPoint dataPoint1 = QLDataPoint.builder()
                                 .key(QLReference.builder().id("id1").name("name1").type("SERVICEID").build())
                                 .value(Integer.valueOf(100))
                                 .build();
    QLDataPoint dataPoint2 = QLDataPoint.builder()
                                 .key(QLReference.builder().id("id2").name("name2").type("SERVICEID").build())
                                 .value(Integer.valueOf(200))
                                 .build();
    QLStackedDataPoint stackedDataPoint1 =
        QLStackedDataPoint.builder()
            .key(QLReference.builder().id("id1").name("env1").type("ENVIRONMENT").build())
            .values(Arrays.asList(dataPoint1, dataPoint2))
            .build();

    QLDataPoint dataPoint3 = QLDataPoint.builder()
                                 .key(QLReference.builder().id("id3").name("name3").type("SERVICEID").build())
                                 .value(Integer.valueOf(450))
                                 .build();
    QLDataPoint dataPoint4 = QLDataPoint.builder()
                                 .key(QLReference.builder().id("id4").name("name4").type("SERVICEID").build())
                                 .value(Integer.valueOf(150))
                                 .build();
    QLDataPoint dataPoint5 = QLDataPoint.builder()
                                 .key(QLReference.builder().id("id5").name("name5").type("SERVICEID").build())
                                 .value(Integer.valueOf(35))
                                 .build();
    QLStackedDataPoint stackedDataPoint2 =
        QLStackedDataPoint.builder()
            .key(QLReference.builder().id("id2").name("env2").type("ENVIRONMENT").build())
            .values(Arrays.asList(dataPoint3, dataPoint4, dataPoint5))
            .build();
    return QLStackedData.builder().dataPoints(Arrays.asList(stackedDataPoint1, stackedDataPoint2)).build();
  }

  public static QLData getTimeAggregatedData() {
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

  public static QLData getAggregatedData() {
    QLDataPoint dataPoint1 = QLDataPoint.builder()
                                 .key(QLReference.builder().id("id1").name("name1").type("SERVICEID").build())
                                 .value(100)
                                 .build();
    QLDataPoint dataPoint2 = QLDataPoint.builder()
                                 .key(QLReference.builder().id("id2").name("name2").type("SERVICEID").build())
                                 .value(200.223)
                                 .build();
    QLDataPoint dataPoint3 = QLDataPoint.builder()
                                 .key(QLReference.builder().id("id3").name("name3").type("SERVICEID").build())
                                 .value(300L)
                                 .build();
    QLDataPoint dataPoint4 = QLDataPoint.builder()
                                 .key(QLReference.builder().id("id4").name("name4").type("SERVICEID").build())
                                 .value(400)
                                 .build();
    return QLAggregatedData.builder().dataPoints(Arrays.asList(dataPoint1, dataPoint2, dataPoint3, dataPoint4)).build();
  }

  public static QLData getSinglePointData() {
    QLDataPoint dataPoint1 = QLDataPoint.builder()
                                 .key(QLReference.builder().id("id1").name("name1").type("SERVICEID").build())
                                 .value(Integer.valueOf(100))
                                 .build();
    return QLSinglePointData.builder().dataPoint(dataPoint1).build();
  }
}
