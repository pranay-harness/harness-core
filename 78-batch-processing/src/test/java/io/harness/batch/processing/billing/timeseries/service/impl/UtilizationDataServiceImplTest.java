package io.harness.batch.processing.billing.timeseries.service.impl;

import static io.harness.rule.OwnerRule.ROHIT;
import static io.harness.rule.OwnerRule.SHUBHANSHU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.batch.processing.billing.service.UtilizationData;
import io.harness.batch.processing.billing.timeseries.data.InstanceUtilizationData;
import io.harness.batch.processing.ccm.InstanceType;
import io.harness.batch.processing.ccm.UtilizationJobType;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.writer.constants.InstanceMetaDataConstants;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
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
import org.mockito.stubbing.Answer;
import software.wings.beans.instance.HarnessServiceInfo;
import software.wings.graphql.datafetcher.DataFetcherUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

@RunWith(MockitoJUnitRunner.class)
public class UtilizationDataServiceImplTest extends CategoryTest {
  @InjectMocks private UtilizationDataServiceImpl utilizationDataService;
  @Mock private TimeScaleDBService timeScaleDBService;
  @Mock private PreparedStatement statement;
  @Mock private DataFetcherUtils utils;
  @Mock ResultSet resultSet;

  public static final String SERVICE_ARN = "service_arn";
  public static final String CLUSTER_ARN = "cluster_arn";
  public static final String SERVICE_NAME = "service_name";
  public static final String CLUSTER_NAME = "cluster_name";
  private static final String ACCOUNT_ID = "account_id";
  private static final String INSTANCE_ID = "instance_id";
  private static final String CLUSTER_ID = "cluster_id";
  private static final String SERVICE_ID = "service_id";
  private static final String APP_ID = "app_id";
  private static final String CLOUD_PROVIDER_ID = "cloud_provider_id";
  private static final String ENV_ID = "env_id";
  private static final String INFRA_MAPPING_ID = "infra_mapping_id";
  private static final String START_TIME = "start_time";
  private static final String END_TIME = "end_time";
  private static final double CPU_UTILIZATION = 0.5;
  private static final double MEMORY_UTILIZATION = 0.5;
  final int[] count = {0};

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

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testGetUtilizationDataForInstances() throws SQLException {
    when(timeScaleDBService.isValid()).thenReturn(true);
    when(statement.execute()).thenReturn(true);
    mockResultSet();
    Map<String, UtilizationData> utilizationDataMap =
        utilizationDataService.getUtilizationDataForInstances(instanceDataList(), START_TIME, END_TIME);
    assertThat(utilizationDataMap).isNotNull();
    assertThat(utilizationDataMap.get(INSTANCE_ID)).isNotNull();
    UtilizationData utilizationData = utilizationDataMap.get(INSTANCE_ID);
    assertThat(utilizationData.getCpuUtilization()).isEqualTo(CPU_UTILIZATION);
    assertThat(utilizationData.getMemoryUtilization()).isEqualTo(MEMORY_UTILIZATION);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testGetUtilizationDataForInstancesWhenDbIsInvalid() {
    when(timeScaleDBService.isValid()).thenReturn(false);
    assertThatThrownBy(
        () -> utilizationDataService.getUtilizationDataForInstances(instanceDataList(), START_TIME, END_TIME))
        .isInstanceOf(InvalidRequestException.class);
  }

  private InstanceUtilizationData instanceUtilizationData() {
    return InstanceUtilizationData.builder()
        .instanceId("serviceArn")
        .settingId("settingId")
        .instanceType(UtilizationJobType.ECS_SERVICE)
        .clusterName(CLUSTER_NAME)
        .clusterArn(CLUSTER_ARN)
        .serviceName(SERVICE_NAME)
        .serviceArn(SERVICE_ARN)
        .startTimestamp(1546281000000l)
        .endTimestamp(1546367400000l)
        .cpuUtilizationAvg(40.0)
        .cpuUtilizationMax(65.0)
        .memoryUtilizationAvg(1024.0)
        .memoryUtilizationMax(1650.0)
        .build();
  }

  private List<? extends InstanceData> instanceDataList() {
    Map<String, String> metaDataMap = new HashMap<>();
    metaDataMap.put(InstanceMetaDataConstants.ECS_SERVICE_ARN, SERVICE_ARN);
    InstanceData instanceData = InstanceData.builder()
                                    .instanceType(InstanceType.EC2_INSTANCE)
                                    .metaData(metaDataMap)
                                    .accountId(ACCOUNT_ID)
                                    .instanceId(INSTANCE_ID)
                                    .clusterId(CLUSTER_ID)
                                    .clusterName(CLUSTER_NAME)
                                    .harnessServiceInfo(getHarnessServiceInfo())
                                    .build();
    return Arrays.asList(instanceData);
  }

  private HarnessServiceInfo getHarnessServiceInfo() {
    return new HarnessServiceInfo(SERVICE_ID, APP_ID, CLOUD_PROVIDER_ID, ENV_ID, INFRA_MAPPING_ID);
  }

  private void mockResultSet() throws SQLException {
    Connection connection = mock(Connection.class);
    resultSet = mock(ResultSet.class);
    when(timeScaleDBService.getDBConnection()).thenReturn(connection);
    when(connection.createStatement()).thenReturn(statement);
    when(statement.executeQuery(anyString())).thenReturn(resultSet);
    when(resultSet.getDouble("CPUUTILIZATION")).thenAnswer((Answer<Double>) invocation -> CPU_UTILIZATION);
    when(resultSet.getDouble("MEMORYUTILIZATION")).thenAnswer((Answer<Double>) invocation -> MEMORY_UTILIZATION);
    when(resultSet.getString("INSTANCEID")).thenAnswer((Answer<String>) invocation -> SERVICE_ARN);
    returnResultSet(1);
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
}
