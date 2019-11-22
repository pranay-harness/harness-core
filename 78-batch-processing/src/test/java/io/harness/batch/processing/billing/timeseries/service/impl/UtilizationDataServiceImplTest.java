package io.harness.batch.processing.billing.timeseries.service.impl;

import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.batch.processing.billing.timeseries.data.InstanceUtilizationData;
import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.TimeZone;

@RunWith(MockitoJUnitRunner.class)
public class UtilizationDataServiceImplTest extends CategoryTest {
  @InjectMocks private UtilizationDataServiceImpl utilizationDataService;
  @Mock private TimeScaleDBService timeScaleDBService;
  @Mock private PreparedStatement statement;
  @Mock private DataFetcherUtils utils;

  @Before
  public void setup() throws SQLException {
    MockitoAnnotations.initMocks(this);
    Connection mockConnection = mock(Connection.class);
    when(timeScaleDBService.getDBConnection()).thenReturn(mockConnection);
    when(mockConnection.prepareStatement(utilizationDataService.INSERT_STATEMENT)).thenReturn(statement);
    when(utils.getDefaultCalendar()).thenReturn(Calendar.getInstance(TimeZone.getTimeZone("UTC")));
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testCreateBillingData() throws SQLException {
    when(timeScaleDBService.isValid()).thenReturn(true);
    when(statement.execute()).thenReturn(true);
    InstanceUtilizationData instanceUtilizationData = instanceUtilizationData();
    boolean insert = utilizationDataService.create(instanceUtilizationData);
    assertThat(insert).isTrue();
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testNullCreateBillingData() throws SQLException {
    when(timeScaleDBService.isValid()).thenReturn(true);
    when(statement.execute()).thenThrow(new SQLException());
    InstanceUtilizationData instanceUtilizationData = instanceUtilizationData();
    boolean insert = utilizationDataService.create(instanceUtilizationData);
    assertThat(insert).isFalse();
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testInvalidDBService() {
    when(timeScaleDBService.isValid()).thenReturn(false);
    InstanceUtilizationData instanceUtilizationData = instanceUtilizationData();
    boolean insert = utilizationDataService.create(instanceUtilizationData);
    assertThat(insert).isFalse();
  }

  private InstanceUtilizationData instanceUtilizationData() {
    return InstanceUtilizationData.builder()
        .clusterName("clusterName")
        .clusterArn("clusterArn")
        .serviceName("serviceName")
        .serviceArn("serviceArn")
        .startTimestamp(1546281000000l)
        .endTimestamp(1546367400000l)
        .cpuUtilizationAvg(40.0)
        .cpuUtilizationMax(65.0)
        .memoryUtilizationAvg(1024.0)
        .memoryUtilizationMax(1650.0)
        .build();
  }
}
