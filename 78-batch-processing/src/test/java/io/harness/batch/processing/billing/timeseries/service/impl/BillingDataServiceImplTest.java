package io.harness.batch.processing.billing.timeseries.service.impl;

import static io.harness.rule.OwnerRule.HITESH;
import static io.harness.rule.OwnerRule.SHUBHANSHU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.batch.processing.billing.timeseries.data.InstanceBillingData;
import io.harness.batch.processing.ccm.ActualIdleCostWriterData;
import io.harness.batch.processing.ccm.InstanceType;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.timescaledb.TimeScaleDBService;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import software.wings.graphql.datafetcher.DataFetcherUtils;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.TimeZone;

@RunWith(MockitoJUnitRunner.class)
public class BillingDataServiceImplTest extends CategoryTest {
  @InjectMocks private BillingDataServiceImpl billingDataService;
  @Mock private TimeScaleDBService timeScaleDBService;
  @Mock private PreparedStatement statement;
  @Mock private DataFetcherUtils utils;

  private final Instant NOW = Instant.now();
  private final long START_TIME_MILLIS = NOW.minus(1, ChronoUnit.HOURS).toEpochMilli();
  private final long END_TIME_MILLIS = NOW.toEpochMilli();
  private static final String ACCOUNT_ID = "accountId";
  private static final String CLUSTER_ID = "clusterId";
  private static final String PARENT_INSTANCE_ID = "parentInstanceId";

  @Before
  public void setup() throws SQLException {
    MockitoAnnotations.initMocks(this);
    Connection mockConnection = mock(Connection.class);
    when(timeScaleDBService.getDBConnection()).thenReturn(mockConnection);
    when(timeScaleDBService.isValid()).thenReturn(true);
    when(mockConnection.prepareStatement(billingDataService.INSERT_STATEMENT)).thenReturn(statement);
    when(utils.getDefaultCalendar()).thenReturn(Calendar.getInstance(TimeZone.getTimeZone("UTC")));
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testCreateBillingData() throws SQLException {
    when(statement.execute()).thenReturn(true);
    InstanceBillingData instanceBillingData = instanceBillingData();
    boolean insert = billingDataService.create(instanceBillingData);
    assertThat(insert).isTrue();
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testUpdateBillingData() throws SQLException {
    Connection mockConnection = mock(Connection.class);
    when(timeScaleDBService.getDBConnection()).thenReturn(mockConnection);
    when(timeScaleDBService.isValid()).thenReturn(true);
    when(mockConnection.prepareStatement(billingDataService.UPDATE_STATEMENT)).thenReturn(statement);
    when(statement.execute()).thenReturn(true);
    ActualIdleCostWriterData actualIdleCostWriterData = mockActualIdleCostWriterData();
    boolean update = billingDataService.update(actualIdleCostWriterData);
    assertThat(update).isTrue();
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testSqlExceptionUpdateBillingData() throws SQLException {
    when(timeScaleDBService.getDBConnection()).thenThrow(SQLException.class);
    ActualIdleCostWriterData actualIdleCostWriterData = mockActualIdleCostWriterData();
    assertThat(billingDataService.update(actualIdleCostWriterData)).isEqualTo(false);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testTimescaleDbInvalidUpdateBillingData() {
    when(timeScaleDBService.isValid()).thenReturn(false);
    ActualIdleCostWriterData actualIdleCostWriterData = mockActualIdleCostWriterData();
    assertThat(billingDataService.update(actualIdleCostWriterData)).isEqualTo(false);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testNullCreateBillingData() throws SQLException {
    when(statement.execute()).thenThrow(new SQLException());
    InstanceBillingData instanceBillingData = instanceBillingData();
    boolean insert = billingDataService.create(instanceBillingData);
    assertThat(insert).isFalse();
  }

  private InstanceBillingData instanceBillingData() {
    return InstanceBillingData.builder()
        .startTimestamp(1546281000000l)
        .endTimestamp(1546367400000l)
        .accountId("ACCOUNT_ID")
        .instanceType(InstanceType.EC2_INSTANCE.name())
        .cpuUnitSeconds(1024)
        .memoryMbSeconds(1024)
        .usageDurationSeconds(3600)
        .build();
  }

  private ActualIdleCostWriterData mockActualIdleCostWriterData() {
    return ActualIdleCostWriterData.builder()
        .accountId(ACCOUNT_ID)
        .clusterId(CLUSTER_ID)
        .parentInstanceId(PARENT_INSTANCE_ID)
        .actualIdleCost(BigDecimal.valueOf(8))
        .cpuActualIdleCost(BigDecimal.valueOf(4))
        .memoryActualIdleCost(BigDecimal.valueOf(4))
        .unallocatedCost(BigDecimal.valueOf(8))
        .cpuUnallocatedCost(BigDecimal.valueOf(4))
        .memoryUnallocatedCost(BigDecimal.valueOf(4))
        .startTime(START_TIME_MILLIS)
        .endTime(END_TIME_MILLIS)
        .build();
  }
}
