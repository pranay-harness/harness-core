package io.harness.batch.processing.billing.timeseries.service.impl;

import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.batch.processing.billing.timeseries.data.K8sGranularUtilizationData;
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
public class K8sUtilizationGranularDataServiceImplTest extends CategoryTest {
  @InjectMocks private K8sUtilizationGranularDataServiceImpl k8sUtilizationGranularDataService;
  @Mock private TimeScaleDBService timeScaleDBService;
  @Mock private PreparedStatement statement;
  @Mock private DataFetcherUtils utils;

  @Before
  public void setup() throws SQLException {
    MockitoAnnotations.initMocks(this);
    Connection mockConnection = mock(Connection.class);
    when(timeScaleDBService.getDBConnection()).thenReturn(mockConnection);
    when(mockConnection.prepareStatement(k8sUtilizationGranularDataService.INSERT_STATEMENT)).thenReturn(statement);
    when(utils.getDefaultCalendar()).thenReturn(Calendar.getInstance(TimeZone.getTimeZone("UTC")));
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testCreateBillingData() throws SQLException {
    when(timeScaleDBService.isValid()).thenReturn(true);
    when(statement.execute()).thenReturn(true);
    K8sGranularUtilizationData k8sGranularUtilizationData = K8sGranularUtilizationData();
    boolean insert = k8sUtilizationGranularDataService.create(k8sGranularUtilizationData);
    assertThat(insert).isTrue();
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testNullCreateBillingData() throws SQLException {
    when(timeScaleDBService.isValid()).thenReturn(true);
    when(statement.execute()).thenThrow(new SQLException());
    K8sGranularUtilizationData k8sGranularUtilizationData = K8sGranularUtilizationData();
    boolean insert = k8sUtilizationGranularDataService.create(k8sGranularUtilizationData);
    assertThat(insert).isFalse();
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testInvalidDBService() {
    when(timeScaleDBService.isValid()).thenReturn(false);
    K8sGranularUtilizationData k8sGranularUtilizationData = K8sGranularUtilizationData();
    boolean insert = k8sUtilizationGranularDataService.create(k8sGranularUtilizationData);
    assertThat(insert).isFalse();
  }

  private K8sGranularUtilizationData K8sGranularUtilizationData() {
    return K8sGranularUtilizationData.builder()
        .settingId("settingId")
        .instanceType("instanceType")
        .instanceId("instanceId")
        .memory(2.0)
        .cpu(2.0)
        .endTimestamp(12000000000L)
        .startTimestamp(10000000000L)
        .build();
  }
}
