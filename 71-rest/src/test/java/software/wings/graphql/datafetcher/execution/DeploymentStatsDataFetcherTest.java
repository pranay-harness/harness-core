package software.wings.graphql.datafetcher.execution;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.experimental.FieldNameConstants;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.graphql.datafetcher.execution.DeploymentStatsQueryMetaData.ResultType;
import software.wings.graphql.schema.type.QLEnvironmentType;
import software.wings.graphql.schema.type.aggregation.QLEnumOperator;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.QLStringFilter;
import software.wings.graphql.schema.type.aggregation.QLStringOperator;
import software.wings.graphql.schema.type.aggregation.QLTimeAggregationType;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeOperator;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.deployment.QLDeploymentAggregation;
import software.wings.graphql.schema.type.aggregation.deployment.QLDeploymentFilter;
import software.wings.graphql.schema.type.aggregation.environment.QLEnvironmentTypeFilter;

import java.util.Arrays;

@FieldNameConstants(innerTypeName = "DeploymentStatsDataFetcherTestKeys")
public class DeploymentStatsDataFetcherTest extends WingsBaseTest {
  @Mock TimeScaleDBService timeScaleDBService;
  @Inject @InjectMocks DeploymentStatsDataFetcher dataFetcher;

  private String ACCOUNTID;
  private String SERVICE1;
  private String SERVICE2;

  @Test
  @Category(UnitTests.class)
  public void testQueries() {
    QLDeploymentFilter arrayIdFilter =
        QLDeploymentFilter.builder()
            .service(QLIdFilter.builder()
                         .operator(QLIdOperator.IN)
                         .values(new String[] {
                             DeploymentStatsDataFetcherTestKeys.SERVICE1, DeploymentStatsDataFetcherTestKeys.SERVICE2})
                         .build())
            .build();
    QLDeploymentFilter beforeTimeFilter =
        QLDeploymentFilter.builder()
            .endTime(QLTimeFilter.builder().operator(QLTimeOperator.AFTER).value(1564612564000L).build())
            .build();

    QLDeploymentFilter afterTimeFilter =
        QLDeploymentFilter.builder()
            .endTime(QLTimeFilter.builder().operator(QLTimeOperator.BEFORE).value(1564612869000L).build())
            .build();
    QLDeploymentFilter arrayStringFilter = QLDeploymentFilter.builder()
                                               .status(QLStringFilter.builder()
                                                           .operator(QLStringOperator.IN)
                                                           .values(new String[] {"SUCCESS", "FAILURE"})
                                                           .build())
                                               .build();

    QLDeploymentFilter arrayEnvTypeFilter =
        QLDeploymentFilter.builder()
            .environmentType(QLEnvironmentTypeFilter.builder()
                                 .operator(QLEnumOperator.IN)
                                 .values(new QLEnvironmentType[] {QLEnvironmentType.PROD})
                                 .build())
            .build();
    DeploymentStatsQueryMetaData queryMetaData = dataFetcher.formQuery(DeploymentStatsDataFetcherTestKeys.ACCOUNTID,
        null, Arrays.asList(arrayIdFilter, beforeTimeFilter, afterTimeFilter, arrayStringFilter, arrayEnvTypeFilter),
        Arrays.asList(QLDeploymentAggregation.Application), null, null);

    Assertions.assertThat(queryMetaData.getQuery())
        .isEqualTo(
            "SELECT COUNT(*) AS COUNT,t0.APPID FROM DEPLOYMENT t0 WHERE ((t0.SERVICES @>'{SERVICE1}') OR (t0.SERVICES @>'{SERVICE2}')) AND (t0.ENDTIME >= '2019-07-31T22:36:04Z') AND (t0.ENDTIME <= '2019-07-31T22:41:09Z') AND (t0.STATUS IN ('SUCCESS','FAILURE') ) AND (t0.ENVTYPES @>'{PROD}') AND (t0.APPID IS NOT NULL) AND (t0.ACCOUNTID = 'ACCOUNTID') AND (t0.PARENT_EXECUTION IS NULL) GROUP BY t0.APPID");

    Assertions.assertThat(queryMetaData.getResultType()).isEqualTo(ResultType.AGGREGATE_DATA);

    queryMetaData = dataFetcher.formQuery(DeploymentStatsDataFetcherTestKeys.ACCOUNTID, null,
        Arrays.asList(arrayIdFilter, beforeTimeFilter, afterTimeFilter, arrayStringFilter, arrayEnvTypeFilter), null,
        null, null);

    Assertions.assertThat(queryMetaData.getQuery())
        .isEqualTo(
            "SELECT COUNT(*) AS COUNT FROM DEPLOYMENT t0 WHERE ((t0.SERVICES @>'{SERVICE1}') OR (t0.SERVICES @>'{SERVICE2}')) AND (t0.ENDTIME >= '2019-07-31T22:36:04Z') AND (t0.ENDTIME <= '2019-07-31T22:41:09Z') AND (t0.STATUS IN ('SUCCESS','FAILURE') ) AND (t0.ENVTYPES @>'{PROD}') AND (t0.ACCOUNTID = 'ACCOUNTID') AND (t0.PARENT_EXECUTION IS NULL)");
    Assertions.assertThat(queryMetaData.getResultType()).isEqualTo(ResultType.SINGLE_POINT);

    queryMetaData = dataFetcher.formQuery(DeploymentStatsDataFetcherTestKeys.ACCOUNTID, null,
        Arrays.asList(arrayIdFilter, beforeTimeFilter, afterTimeFilter, arrayStringFilter, arrayEnvTypeFilter),
        Arrays.asList(QLDeploymentAggregation.Application, QLDeploymentAggregation.Environment), null, null);

    Assertions.assertThat(queryMetaData.getQuery())
        .isEqualTo(
            "SELECT COUNT(*) AS COUNT,t0.APPID,ENVID FROM DEPLOYMENT t0, unnest(ENVIRONMENTS) ENVID WHERE ((t0.SERVICES @>'{SERVICE1}') OR (t0.SERVICES @>'{SERVICE2}')) AND (t0.ENDTIME >= '2019-07-31T22:36:04Z') AND (t0.ENDTIME <= '2019-07-31T22:41:09Z') AND (t0.STATUS IN ('SUCCESS','FAILURE') ) AND (t0.ENVTYPES @>'{PROD}') AND (t0.APPID IS NOT NULL) AND (t0.ACCOUNTID = 'ACCOUNTID') AND (t0.PARENT_EXECUTION IS NULL) GROUP BY t0.APPID,ENVID");
    Assertions.assertThat(queryMetaData.getResultType()).isEqualTo(ResultType.STACKED_BAR_CHART);

    queryMetaData = dataFetcher.formQuery(DeploymentStatsDataFetcherTestKeys.ACCOUNTID, null,
        Arrays.asList(arrayIdFilter, beforeTimeFilter, afterTimeFilter, arrayStringFilter, arrayEnvTypeFilter),
        Arrays.asList(QLDeploymentAggregation.Application),
        QLTimeSeriesAggregation.builder()
            .timeAggregationType(QLTimeAggregationType.HOUR)
            .timeAggregationValue(1)
            .build(),
        null);

    Assertions.assertThat(queryMetaData.getQuery())
        .isEqualTo(
            "SELECT COUNT(*) AS COUNT,time_bucket('1 hours',endtime) AS TIME_BUCKET,t0.APPID FROM DEPLOYMENT t0 WHERE ((t0.SERVICES @>'{SERVICE1}') OR (t0.SERVICES @>'{SERVICE2}')) AND (t0.ENDTIME >= '2019-07-31T22:36:04Z') AND (t0.ENDTIME <= '2019-07-31T22:41:09Z') AND (t0.STATUS IN ('SUCCESS','FAILURE') ) AND (t0.ENVTYPES @>'{PROD}') AND (t0.APPID IS NOT NULL) AND (t0.ACCOUNTID = 'ACCOUNTID') AND (t0.PARENT_EXECUTION IS NULL) GROUP BY TIME_BUCKET,t0.APPID ORDER BY TIME_BUCKET ASC");
    Assertions.assertThat(queryMetaData.getResultType()).isEqualTo(ResultType.STACKED_TIME_SERIES);

    queryMetaData = dataFetcher.formQuery(DeploymentStatsDataFetcherTestKeys.ACCOUNTID, null,
        Arrays.asList(arrayIdFilter, beforeTimeFilter, afterTimeFilter, arrayStringFilter, arrayEnvTypeFilter), null,
        QLTimeSeriesAggregation.builder()
            .timeAggregationType(QLTimeAggregationType.HOUR)
            .timeAggregationValue(1)
            .build(),
        null);

    Assertions.assertThat(queryMetaData.getQuery())
        .isEqualTo(
            "SELECT COUNT(*) AS COUNT,time_bucket('1 hours',endtime) AS TIME_BUCKET FROM DEPLOYMENT t0 WHERE ((t0.SERVICES @>'{SERVICE1}') OR (t0.SERVICES @>'{SERVICE2}')) AND (t0.ENDTIME >= '2019-07-31T22:36:04Z') AND (t0.ENDTIME <= '2019-07-31T22:41:09Z') AND (t0.STATUS IN ('SUCCESS','FAILURE') ) AND (t0.ENVTYPES @>'{PROD}') AND (t0.ACCOUNTID = 'ACCOUNTID') AND (t0.PARENT_EXECUTION IS NULL) GROUP BY TIME_BUCKET ORDER BY TIME_BUCKET ASC");
    Assertions.assertThat(queryMetaData.getResultType()).isEqualTo(ResultType.TIME_SERIES);
  }
}
