package software.wings.graphql.datafetcher.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import de.danielbechler.util.Collections;
import io.harness.category.element.UnitTests;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.experimental.FieldNameConstants;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import software.wings.WingsBaseTest;
import software.wings.beans.EntityType;
import software.wings.graphql.datafetcher.execution.DeploymentStatsQueryMetaData.DeploymentMetaDataFields;
import software.wings.graphql.datafetcher.execution.DeploymentStatsQueryMetaData.ResultType;
import software.wings.graphql.datafetcher.tag.TagHelper;
import software.wings.graphql.schema.type.QLEnvironmentType;
import software.wings.graphql.schema.type.aggregation.QLAggregatedData;
import software.wings.graphql.schema.type.aggregation.QLCountAggregateOperation;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLDataPoint;
import software.wings.graphql.schema.type.aggregation.QLEnumOperator;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.QLSinglePointData;
import software.wings.graphql.schema.type.aggregation.QLSortOrder;
import software.wings.graphql.schema.type.aggregation.QLStackedData;
import software.wings.graphql.schema.type.aggregation.QLStackedDataPoint;
import software.wings.graphql.schema.type.aggregation.QLStackedTimeSeriesData;
import software.wings.graphql.schema.type.aggregation.QLStackedTimeSeriesDataPoint;
import software.wings.graphql.schema.type.aggregation.QLStringFilter;
import software.wings.graphql.schema.type.aggregation.QLStringOperator;
import software.wings.graphql.schema.type.aggregation.QLTimeAggregationType;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeOperator;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesData;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesDataPoint;
import software.wings.graphql.schema.type.aggregation.deployment.QLDeploymentAggregation;
import software.wings.graphql.schema.type.aggregation.deployment.QLDeploymentEntityAggregation;
import software.wings.graphql.schema.type.aggregation.deployment.QLDeploymentFilter;
import software.wings.graphql.schema.type.aggregation.deployment.QLDeploymentSortCriteria;
import software.wings.graphql.schema.type.aggregation.deployment.QLDeploymentSortType;
import software.wings.graphql.schema.type.aggregation.deployment.QLDeploymentTagAggregation;
import software.wings.graphql.schema.type.aggregation.deployment.QLDeploymentTagFilter;
import software.wings.graphql.schema.type.aggregation.deployment.QLDeploymentTagType;
import software.wings.graphql.schema.type.aggregation.environment.QLEnvironmentTypeFilter;
import software.wings.graphql.schema.type.aggregation.tag.QLTagInput;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

@FieldNameConstants(innerTypeName = "DeploymentStatsDataFetcherTestKeys")
public class DeploymentStatsDataFetcherTest extends WingsBaseTest {
  @Mock TimeScaleDBService timeScaleDBService;
  @Mock QLStatsHelper statsHelper;
  @Mock TagHelper tagHelper;
  @Inject @InjectMocks DeploymentStatsDataFetcher dataFetcher;
  @Mock ResultSet resultSet;
  final int[] count = {0};
  final int[] intVal = {0};
  final long[] longVal = {0};
  final int[] stringVal = {0};
  final long currentTime = System.currentTimeMillis();
  final long[] calendar = {currentTime};

  @Before
  public void setUp() throws SQLException {
    Connection mockConnection = mock(Connection.class);
    Statement mockStatement = mock(Statement.class);
    when(timeScaleDBService.getDBConnection()).thenReturn(mockConnection);
    when(timeScaleDBService.isValid()).thenReturn(true);
    when(mockConnection.createStatement()).thenReturn(mockStatement);
    when(mockStatement.executeQuery(anyString())).thenReturn(resultSet);
    resetValues();
    mockStatHelper();
    mockResultSet();
    mockTagHelper();
  }

  private void mockTagHelper() {
    when(tagHelper.getEntityIdsFromTags(anyString(), anyListOf(QLTagInput.class), Matchers.any(EntityType.class)))
        .thenReturn(Collections.setOf(Arrays.asList("DATA1", "DATA2")));
  }

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
        Arrays.asList(QLDeploymentEntityAggregation.Application), null, null);

    assertThat(queryMetaData.getQuery())
        .isEqualTo(
            "SELECT COUNT(*) AS COUNT,t0.APPID FROM DEPLOYMENT t0 WHERE ((t0.SERVICES @>'{SERVICE1}') OR (t0.SERVICES @>'{SERVICE2}')) AND (t0.ENDTIME >= '2019-07-31T22:36:04Z') AND (t0.ENDTIME <= '2019-07-31T22:41:09Z') AND (t0.STATUS IN ('SUCCESS','FAILURE') ) AND (t0.ENVTYPES @>'{PROD}') AND (t0.APPID IS NOT NULL) AND (t0.ACCOUNTID = 'ACCOUNTID') AND (t0.PARENT_EXECUTION IS NULL) GROUP BY t0.APPID");

    assertThat(queryMetaData.getResultType()).isEqualTo(ResultType.AGGREGATE_DATA);

    queryMetaData = dataFetcher.formQuery(DeploymentStatsDataFetcherTestKeys.ACCOUNTID, null,
        Arrays.asList(arrayIdFilter, beforeTimeFilter, afterTimeFilter, arrayStringFilter, arrayEnvTypeFilter), null,
        null, null);

    assertThat(queryMetaData.getQuery())
        .isEqualTo(
            "SELECT COUNT(*) AS COUNT FROM DEPLOYMENT t0 WHERE ((t0.SERVICES @>'{SERVICE1}') OR (t0.SERVICES @>'{SERVICE2}')) AND (t0.ENDTIME >= '2019-07-31T22:36:04Z') AND (t0.ENDTIME <= '2019-07-31T22:41:09Z') AND (t0.STATUS IN ('SUCCESS','FAILURE') ) AND (t0.ENVTYPES @>'{PROD}') AND (t0.ACCOUNTID = 'ACCOUNTID') AND (t0.PARENT_EXECUTION IS NULL)");
    assertThat(queryMetaData.getResultType()).isEqualTo(ResultType.SINGLE_POINT);

    queryMetaData = dataFetcher.formQuery(DeploymentStatsDataFetcherTestKeys.ACCOUNTID, null,
        Arrays.asList(arrayIdFilter, beforeTimeFilter, afterTimeFilter, arrayStringFilter, arrayEnvTypeFilter),
        Arrays.asList(QLDeploymentEntityAggregation.Application, QLDeploymentEntityAggregation.Environment), null,
        null);

    assertThat(queryMetaData.getQuery())
        .isEqualTo(
            "SELECT COUNT(*) AS COUNT,t0.APPID,ENVID FROM DEPLOYMENT t0, unnest(ENVIRONMENTS) ENVID WHERE ((t0.SERVICES @>'{SERVICE1}') OR (t0.SERVICES @>'{SERVICE2}')) AND (t0.ENDTIME >= '2019-07-31T22:36:04Z') AND (t0.ENDTIME <= '2019-07-31T22:41:09Z') AND (t0.STATUS IN ('SUCCESS','FAILURE') ) AND (t0.ENVTYPES @>'{PROD}') AND (t0.APPID IS NOT NULL) AND (t0.ACCOUNTID = 'ACCOUNTID') AND (t0.PARENT_EXECUTION IS NULL) GROUP BY t0.APPID,ENVID");
    assertThat(queryMetaData.getResultType()).isEqualTo(ResultType.STACKED_BAR_CHART);

    queryMetaData = dataFetcher.formQuery(DeploymentStatsDataFetcherTestKeys.ACCOUNTID, null,
        Arrays.asList(arrayIdFilter, beforeTimeFilter, afterTimeFilter, arrayStringFilter, arrayEnvTypeFilter),
        Arrays.asList(QLDeploymentEntityAggregation.Application),
        QLTimeSeriesAggregation.builder()
            .timeAggregationType(QLTimeAggregationType.HOUR)
            .timeAggregationValue(1)
            .build(),
        null);

    assertThat(queryMetaData.getQuery())
        .isEqualTo(
            "SELECT COUNT(*) AS COUNT,time_bucket('1 hours',endtime) AS TIME_BUCKET,t0.APPID FROM DEPLOYMENT t0 WHERE ((t0.SERVICES @>'{SERVICE1}') OR (t0.SERVICES @>'{SERVICE2}')) AND (t0.ENDTIME >= '2019-07-31T22:36:04Z') AND (t0.ENDTIME <= '2019-07-31T22:41:09Z') AND (t0.STATUS IN ('SUCCESS','FAILURE') ) AND (t0.ENVTYPES @>'{PROD}') AND (t0.APPID IS NOT NULL) AND (t0.ACCOUNTID = 'ACCOUNTID') AND (t0.PARENT_EXECUTION IS NULL) GROUP BY TIME_BUCKET,t0.APPID ORDER BY TIME_BUCKET ASC");
    assertThat(queryMetaData.getResultType()).isEqualTo(ResultType.STACKED_TIME_SERIES);

    queryMetaData = dataFetcher.formQuery(DeploymentStatsDataFetcherTestKeys.ACCOUNTID, null,
        Arrays.asList(arrayIdFilter, beforeTimeFilter, afterTimeFilter, arrayStringFilter, arrayEnvTypeFilter), null,
        QLTimeSeriesAggregation.builder()
            .timeAggregationType(QLTimeAggregationType.HOUR)
            .timeAggregationValue(1)
            .build(),
        null);

    assertThat(queryMetaData.getQuery())
        .isEqualTo(
            "SELECT COUNT(*) AS COUNT,time_bucket('1 hours',endtime) AS TIME_BUCKET FROM DEPLOYMENT t0 WHERE ((t0.SERVICES @>'{SERVICE1}') OR (t0.SERVICES @>'{SERVICE2}')) AND (t0.ENDTIME >= '2019-07-31T22:36:04Z') AND (t0.ENDTIME <= '2019-07-31T22:41:09Z') AND (t0.STATUS IN ('SUCCESS','FAILURE') ) AND (t0.ENVTYPES @>'{PROD}') AND (t0.ACCOUNTID = 'ACCOUNTID') AND (t0.PARENT_EXECUTION IS NULL) GROUP BY TIME_BUCKET ORDER BY TIME_BUCKET ASC");
    assertThat(queryMetaData.getResultType()).isEqualTo(ResultType.TIME_SERIES);

    QLDeploymentFilter appTagFilter =
        QLDeploymentFilter.builder()
            .tag(QLDeploymentTagFilter.builder()
                     .entityType(QLDeploymentTagType.APPLICATION)
                     .tags(Arrays.asList(QLTagInput.builder().name("TAG1").value("DATA1").build()))
                     .build())
            .build();

    QLDeploymentFilter serviceTagFilter =
        QLDeploymentFilter.builder()
            .tag(QLDeploymentTagFilter.builder()
                     .entityType(QLDeploymentTagType.SERVICE)
                     .tags(Arrays.asList(QLTagInput.builder().name("TAG1").value("DATA1").build()))
                     .build())
            .build();

    QLDeploymentFilter envTagFilter =
        QLDeploymentFilter.builder()
            .tag(QLDeploymentTagFilter.builder()
                     .entityType(QLDeploymentTagType.ENVIRONMENT)
                     .tags(Arrays.asList(QLTagInput.builder().name("TAG1").value("DATA1").build()))
                     .build())
            .build();

    queryMetaData = dataFetcher.formQuery(DeploymentStatsDataFetcherTestKeys.ACCOUNTID, null,
        Arrays.asList(arrayIdFilter, beforeTimeFilter, afterTimeFilter, arrayStringFilter, arrayEnvTypeFilter,
            appTagFilter, serviceTagFilter, envTagFilter),
        null,
        QLTimeSeriesAggregation.builder()
            .timeAggregationType(QLTimeAggregationType.HOUR)
            .timeAggregationValue(1)
            .build(),
        null);

    assertThat(queryMetaData.getQuery())
        .isEqualTo(
            "SELECT COUNT(*) AS COUNT,time_bucket('1 hours',endtime) AS TIME_BUCKET FROM DEPLOYMENT t0 WHERE ((t0.SERVICES @>'{SERVICE1}') OR (t0.SERVICES @>'{SERVICE2}')) AND (t0.ENDTIME >= '2019-07-31T22:36:04Z') AND (t0.ENDTIME <= '2019-07-31T22:41:09Z') AND (t0.STATUS IN ('SUCCESS','FAILURE') ) AND (t0.ENVTYPES @>'{PROD}') AND (t0.APPID IN ('DATA1','DATA2') ) AND ((t0.SERVICES @>'{DATA1}') OR (t0.SERVICES @>'{DATA2}')) AND ((t0.ENVIRONMENTS @>'{DATA1}') OR (t0.ENVIRONMENTS @>'{DATA2}')) AND (t0.ACCOUNTID = 'ACCOUNTID') AND (t0.PARENT_EXECUTION IS NULL) GROUP BY TIME_BUCKET ORDER BY TIME_BUCKET ASC");
  }

  @Test
  @Category(UnitTests.class)
  public void testSingleDataPoint() {
    try {
      DeploymentStatsQueryMetaData metaData =
          DeploymentStatsQueryMetaData.builder().fieldNames(Arrays.asList(DeploymentMetaDataFields.COUNT)).build();

      when(resultSet.next()).then((Answer<Boolean>) invocation -> {
        switch (count[0]) {
          case 0:
            count[0]++;
            return true;
          default:
            return false;
        }
      });

      when(resultSet.getInt(anyString())).thenReturn(10);
      when(resultSet.getLong(anyString())).thenReturn(20L);

      resetValues();

      QLData qlData = dataFetcher.fetch(DeploymentStatsDataFetcherTestKeys.ACCOUNTID,
          QLDeploymentAggregationFunction.builder().count(QLCountAggregateOperation.SUM).build(), new ArrayList<>(),
          new ArrayList<>(), new ArrayList<>());

      assertThat(qlData.getClass()).isEqualTo(QLSinglePointData.class);
      QLSinglePointData singlePointData = (QLSinglePointData) qlData;
      validateSingleDataPoint(singlePointData, 10);

      resetValues();

      metaData =
          DeploymentStatsQueryMetaData.builder().fieldNames(Arrays.asList(DeploymentMetaDataFields.DURATION)).build();
      qlData = dataFetcher.getData(DeploymentStatsDataFetcherTestKeys.ACCOUNTID,
          QLDeploymentAggregationFunction.builder().duration(QLDurationAggregateOperation.MAX).build(),
          Arrays.asList(QLDeploymentFilter.builder()
                            .tag(QLDeploymentTagFilter.builder()
                                     .entityType(QLDeploymentTagType.APPLICATION)
                                     .tags(Arrays.asList(QLTagInput.builder().name("TAG1").value("VALUE").build()))
                                     .build())
                            .build(),
              QLDeploymentFilter.builder()
                  .tag(QLDeploymentTagFilter.builder()
                           .entityType(QLDeploymentTagType.SERVICE)
                           .tags(Arrays.asList(QLTagInput.builder().name("TAG1").value("VALUE").build()))
                           .build())
                  .build(),
              QLDeploymentFilter.builder()
                  .tag(QLDeploymentTagFilter.builder()
                           .entityType(QLDeploymentTagType.ENVIRONMENT)
                           .tags(Arrays.asList(QLTagInput.builder().name("TAG1").value("VALUE").build()))
                           .build())
                  .build()),
          new ArrayList<>(), new ArrayList<>());

      assertThat(qlData.getClass()).isEqualTo(QLSinglePointData.class);
      singlePointData = (QLSinglePointData) qlData;
      validateSingleDataPoint(singlePointData, 20L);

      resetValues();
      try {
        metaData =
            DeploymentStatsQueryMetaData.builder().fieldNames(Arrays.asList(DeploymentMetaDataFields.APPID)).build();
        qlData = dataFetcher.generateSinglePointData(metaData, resultSet);
        fail("Expected an exception");
      } catch (RuntimeException e) {
        assertThat(e).hasMessage(
            "Single Data Type data type not supported " + DeploymentMetaDataFields.APPID.getDataType());
      }

    } catch (Exception e) {
      Assertions.fail(e.getMessage());
    }
  }

  @Test
  @Category(UnitTests.class)
  public void testAggregateData() {
    try {
      QLData qlData = dataFetcher.fetch(DeploymentStatsDataFetcherTestKeys.ACCOUNTID,
          QLDeploymentAggregationFunction.builder().count(QLCountAggregateOperation.SUM).build(), new ArrayList<>(),
          Arrays.asList(
              QLDeploymentAggregation.builder().entityAggregation(QLDeploymentEntityAggregation.Environment).build()),
          new ArrayList<>());

      assertThat(qlData).isInstanceOf(QLAggregatedData.class);
      QLAggregatedData aggregatedData = (QLAggregatedData) qlData;
      assertThat(aggregatedData.getDataPoints().size()).isEqualTo(5);
      validateAggregateDataPoint(aggregatedData, 0, 10, "DATA0", "DATA0NAME");
      validateAggregateDataPoint(aggregatedData, 1, 11, "DATA1", "DATA1NAME");
      validateAggregateDataPoint(aggregatedData, 2, 12, "DATA2", "DATA2NAME");
      validateAggregateDataPoint(aggregatedData, 3, 13, "DATA3", "DATA3NAME");
      validateAggregateDataPoint(aggregatedData, 4, 14, "DATA4", "DATA4NAME");

    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  private void mockStatHelper() {
    when(statsHelper.getEntityName(Matchers.any(DeploymentMetaDataFields.class), anyString()))
        .thenAnswer(new Answer<String>() {
          @Override
          public String answer(InvocationOnMock invocation) throws Throwable {
            return invocation.getArguments()[1] + "NAME";
          }
        });
  }

  @Test
  @Category(UnitTests.class)
  public void testStackedData() {
    try {
      QLStackedData qlData = (QLStackedData) dataFetcher.fetch(DeploymentStatsDataFetcherTestKeys.ACCOUNTID,
          QLDeploymentAggregationFunction.builder().count(QLCountAggregateOperation.SUM).build(), new ArrayList<>(),
          Arrays.asList(
              QLDeploymentAggregation.builder().entityAggregation(QLDeploymentEntityAggregation.Service).build(),
              QLDeploymentAggregation.builder().entityAggregation(QLDeploymentEntityAggregation.Application).build()),
          new ArrayList<>());

      assertThat(qlData.getDataPoints().size()).isEqualTo(5);

      validateStackedDataPointKey(qlData.getDataPoints().get(0), "DATA0", "DATA0NAME");
      assertThat(qlData.getDataPoints().get(0).getValues().size()).isEqualTo(1);
      validateQLDataPoint(qlData.getDataPoints().get(0).getValues().get(0), "DATA1", "DATA1NAME", 10);

      validateStackedDataPointKey(qlData.getDataPoints().get(1), "DATA2", "DATA2NAME");
      assertThat(qlData.getDataPoints().get(1).getValues().size()).isEqualTo(1);
      validateQLDataPoint(qlData.getDataPoints().get(1).getValues().get(0), "DATA3", "DATA3NAME", 11);

      validateStackedDataPointKey(qlData.getDataPoints().get(2), "DATA4", "DATA4NAME");
      assertThat(qlData.getDataPoints().get(2).getValues().size()).isEqualTo(1);
      validateQLDataPoint(qlData.getDataPoints().get(2).getValues().get(0), "DATA5", "DATA5NAME", 12);

      validateStackedDataPointKey(qlData.getDataPoints().get(3), "DATA6", "DATA6NAME");
      assertThat(qlData.getDataPoints().get(3).getValues().size()).isEqualTo(1);
      validateQLDataPoint(qlData.getDataPoints().get(3).getValues().get(0), "DATA7", "DATA7NAME", 13);

      validateStackedDataPointKey(qlData.getDataPoints().get(4), "DATA8", "DATA8NAME");
      assertThat(qlData.getDataPoints().get(4).getValues().size()).isEqualTo(1);
      validateQLDataPoint(qlData.getDataPoints().get(4).getValues().get(0), "DATA9", "DATA9NAME", 14);

      resetValues();

      qlData = (QLStackedData) dataFetcher.fetch(DeploymentStatsDataFetcherTestKeys.ACCOUNTID,
          QLDeploymentAggregationFunction.builder().count(QLCountAggregateOperation.SUM).build(), new ArrayList<>(),
          Arrays.asList(
              QLDeploymentAggregation.builder().entityAggregation(QLDeploymentEntityAggregation.Status).build(),
              QLDeploymentAggregation.builder().entityAggregation(QLDeploymentEntityAggregation.Application).build()),
          Arrays.asList(QLDeploymentSortCriteria.builder()
                            .sortOrder(QLSortOrder.DESCENDING)
                            .sortType(QLDeploymentSortType.Count)
                            .build()));

      validateStackedDataPointKey(qlData.getDataPoints().get(4), "DATA0", "DATA0NAME");
      assertThat(qlData.getDataPoints().get(4).getValues().size()).isEqualTo(1);
      validateQLDataPoint(qlData.getDataPoints().get(4).getValues().get(0), "DATA1", "DATA1NAME", 10);

      validateStackedDataPointKey(qlData.getDataPoints().get(3), "DATA2", "DATA2NAME");
      assertThat(qlData.getDataPoints().get(3).getValues().size()).isEqualTo(1);
      validateQLDataPoint(qlData.getDataPoints().get(3).getValues().get(0), "DATA3", "DATA3NAME", 11);

      validateStackedDataPointKey(qlData.getDataPoints().get(2), "DATA4", "DATA4NAME");
      assertThat(qlData.getDataPoints().get(2).getValues().size()).isEqualTo(1);
      validateQLDataPoint(qlData.getDataPoints().get(2).getValues().get(0), "DATA5", "DATA5NAME", 12);

      validateStackedDataPointKey(qlData.getDataPoints().get(1), "DATA6", "DATA6NAME");
      assertThat(qlData.getDataPoints().get(1).getValues().size()).isEqualTo(1);
      validateQLDataPoint(qlData.getDataPoints().get(1).getValues().get(0), "DATA7", "DATA7NAME", 13);

      validateStackedDataPointKey(qlData.getDataPoints().get(0), "DATA8", "DATA8NAME");
      assertThat(qlData.getDataPoints().get(0).getValues().size()).isEqualTo(1);
      validateQLDataPoint(qlData.getDataPoints().get(0).getValues().get(0), "DATA9", "DATA9NAME", 14);

    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  private void resetValues() {
    count[0] = 0;
    intVal[0] = 0;
    longVal[0] = 0;
    stringVal[0] = 0;
  }

  @Test
  @Category(UnitTests.class)
  public void testTimeSeriesData() {
    try {
      QLTimeSeriesData timeSeriesData =
          (QLTimeSeriesData) dataFetcher.fetch(DeploymentStatsDataFetcherTestKeys.ACCOUNTID,
              QLDeploymentAggregationFunction.builder().count(QLCountAggregateOperation.SUM).build(), new ArrayList<>(),
              Arrays.asList(QLDeploymentAggregation.builder()
                                .timeAggregation(QLTimeSeriesAggregation.builder()
                                                     .timeAggregationType(QLTimeAggregationType.DAY)
                                                     .timeAggregationValue(1)
                                                     .build())
                                .build()),
              new ArrayList<>());

      validateTimeSeriesDataPoint(timeSeriesData.getDataPoints().get(0), 10, currentTime + 3600000 * 1);
      validateTimeSeriesDataPoint(timeSeriesData.getDataPoints().get(1), 11, currentTime + 3600000 * 2);
      validateTimeSeriesDataPoint(timeSeriesData.getDataPoints().get(2), 12, currentTime + 3600000 * 3);
      validateTimeSeriesDataPoint(timeSeriesData.getDataPoints().get(3), 13, currentTime + 3600000 * 4);
      validateTimeSeriesDataPoint(timeSeriesData.getDataPoints().get(4), 14, currentTime + 3600000 * 5);

    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  private void returnResultSet(int limit) throws SQLException {
    when(resultSet.next()).then((Answer<Boolean>) invocation -> {
      if (count[0] < limit) {
        count[0]++;
        return true;
      }
      return false;
    });
  }

  private void mockResultSet() throws SQLException {
    when(resultSet.getInt(anyString())).thenAnswer((Answer<Integer>) invocation -> 10 + intVal[0]++);
    when(resultSet.getLong(anyString())).thenAnswer((Answer<Long>) invocation -> 20L + longVal[0]++);
    when(resultSet.getString(anyString())).thenAnswer((Answer<String>) invocation -> "DATA" + stringVal[0]++);
    when(resultSet.getTimestamp(anyString(), any(Calendar.class))).thenAnswer((Answer<Timestamp>) invocation -> {
      calendar[0] = calendar[0] + 3600000;
      return new Timestamp(calendar[0]);
    });
    returnResultSet(5);
  }

  @Test
  @Category(UnitTests.class)
  public void testStackedTimeSeriesDataForCount() {
    testStackedTimeSeriesData(QLDeploymentAggregationFunction.builder().count(QLCountAggregateOperation.SUM).build());
  }

  @Test
  @Category(UnitTests.class)
  public void testStackedTimeSeriesDataForInstancesDeployed() {
    testStackedTimeSeriesData(
        QLDeploymentAggregationFunction.builder().instancesDeployed(QLCountAggregateOperation.SUM).build());
  }

  private void testStackedTimeSeriesData(QLDeploymentAggregationFunction deploymentAggregationFunction) {
    try {
      QLStackedTimeSeriesData data = (QLStackedTimeSeriesData) dataFetcher.fetch(
          DeploymentStatsDataFetcherTestKeys.ACCOUNTID, deploymentAggregationFunction, new ArrayList<>(),
          Arrays.asList(QLDeploymentAggregation.builder()
                            .timeAggregation(QLTimeSeriesAggregation.builder()
                                                 .timeAggregationType(QLTimeAggregationType.DAY)
                                                 .timeAggregationValue(1)
                                                 .build())
                            .entityAggregation(QLDeploymentEntityAggregation.EnvironmentType)
                            .build()),
          new ArrayList<>());

      validateStackedTimeSeriesDataPoint(data.getData().get(0), currentTime + 3600000 * 1);
      validateStackedTimeSeriesDataPoint(data.getData().get(1), currentTime + 3600000 * 2);
      validateStackedTimeSeriesDataPoint(data.getData().get(2), currentTime + 3600000 * 3);
      validateStackedTimeSeriesDataPoint(data.getData().get(3), currentTime + 3600000 * 4);
      validateStackedTimeSeriesDataPoint(data.getData().get(4), currentTime + 3600000 * 5);
      validateQLDataPoint(data.getData().get(0).getValues().get(0), "DATA0", "DATA0NAME", 10);
      validateQLDataPoint(data.getData().get(1).getValues().get(0), "DATA1", "DATA1NAME", 11);
      validateQLDataPoint(data.getData().get(2).getValues().get(0), "DATA2", "DATA2NAME", 12);
      validateQLDataPoint(data.getData().get(3).getValues().get(0), "DATA3", "DATA3NAME", 13);
      validateQLDataPoint(data.getData().get(4).getValues().get(0), "DATA4", "DATA4NAME", 14);

    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  private void validateStackedTimeSeriesDataPoint(QLStackedTimeSeriesDataPoint dataPoint, long timeStamp) {
    assertThat(dataPoint.getTime()).isEqualTo(timeStamp);
  }

  private void validateTimeSeriesDataPoint(QLTimeSeriesDataPoint dataPoint, int value, long timeStamp) {
    assertThat(dataPoint.getValue()).isEqualTo(value);
    assertThat(dataPoint.getTime()).isEqualTo(timeStamp);
  }

  private void validateStackedDataPointKey(QLStackedDataPoint dataPoint, String id, String name) {
    assertThat(dataPoint.getKey().getId()).isEqualTo(id);
    assertThat(dataPoint.getKey().getName()).isEqualTo(name);
  }

  private void validateQLDataPoint(QLDataPoint dataPoint, String id, String name, int value) {
    assertThat(dataPoint.getKey().getId()).isEqualTo(id);
    assertThat(dataPoint.getKey().getName()).isEqualTo(name);
    assertThat(dataPoint.getValue()).isEqualTo(value);
  }

  private void validateAggregateDataPoint(QLAggregatedData aggregatedData, int i, int i2, String s, String s2) {
    assertThat(aggregatedData.getDataPoints().get(i).getValue()).isEqualTo(i2);
    assertThat(aggregatedData.getDataPoints().get(i).getKey().getType())
        .isEqualTo(DeploymentMetaDataFields.ENVID.name());
    assertThat(aggregatedData.getDataPoints().get(i).getKey().getId()).isEqualTo(s);
    assertThat(aggregatedData.getDataPoints().get(i).getKey().getName()).isEqualTo(s2);
  }

  private void validateSingleDataPoint(QLSinglePointData singlePointData, Number l) {
    Assertions.assertThat(singlePointData.getDataPoint().getKey().getId()).isEqualTo(dataFetcher.getEntityType());
    Assertions.assertThat(singlePointData.getDataPoint().getKey().getName()).isEqualTo(dataFetcher.getEntityType());
    Assertions.assertThat(singlePointData.getDataPoint().getKey().getType()).isEqualTo(dataFetcher.getEntityType());
    Assertions.assertThat(singlePointData.getDataPoint().getValue()).isEqualTo(l);
  }

  @Test
  @Category(UnitTests.class)
  public void assertGroupByTag() {
    assertThat(dataFetcher.getGroupByEntityFromTag(
                   QLDeploymentTagAggregation.builder().entityType(QLDeploymentTagType.APPLICATION).build()))
        .isEqualTo(QLDeploymentEntityAggregation.Application);
    assertThat(dataFetcher.getGroupByEntityFromTag(
                   QLDeploymentTagAggregation.builder().entityType(QLDeploymentTagType.SERVICE).build()))
        .isEqualTo(QLDeploymentEntityAggregation.Service);

    assertThat(dataFetcher.getGroupByEntityFromTag(
                   QLDeploymentTagAggregation.builder().entityType(QLDeploymentTagType.ENVIRONMENT).build()))
        .isEqualTo(QLDeploymentEntityAggregation.Environment);
  }
}
