package io.harness.batch.processing.billing.timeseries.service.impl;

import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.batch.processing.ccm.ClusterCostData;
import io.harness.batch.processing.ccm.UnallocatedCostData;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class UnallocatedBillingDataServiceImplTest {
  @InjectMocks private UnallocatedBillingDataServiceImpl unallocatedBillingDataService;
  @Mock private TimeScaleDBService timeScaleDBService;
  @Mock private PreparedStatement statement;
  @Mock ResultSet resultSet;
  final int[] count = {0};

  private final Instant NOW = Instant.now();
  private static final String CLUSTER_ID = "clusterId";
  private static final String INSTANCE_TYPE = "Pod";
  private static final double COST = 0.5;
  private final long START_TIME = NOW.minus(1, ChronoUnit.HOURS).toEpochMilli();
  private final long END_TIME = NOW.toEpochMilli();
  private static final String BILLING_ACCOUNT_ID = "billingAccountId";
  private static final String ACCOUNT_ID = "accountId";
  private static final String CLUSTER_NAME = "clusterName";
  private static final String SETTING_ID = "settingId";
  private static final String REGION = "region";
  private static final String CLOUD_PROVIDER = "cloudProvider";
  private static final String CLUSTER_TYPE = "clusterType";
  private static final String WORKLOAD_TYPE = "workloadType";

  @Before
  public void setup() throws SQLException {
    MockitoAnnotations.initMocks(this);
    Connection mockConnection = mock(Connection.class);
    when(timeScaleDBService.getDBConnection()).thenReturn(mockConnection);
    when(mockConnection.prepareStatement(unallocatedBillingDataService.GET_UNALLOCATED_COST_DATA))
        .thenReturn(statement);
    mockResultSet();
  }

  private void mockResultSet() throws SQLException {
    Connection connection = mock(Connection.class);
    resultSet = mock(ResultSet.class);
    when(timeScaleDBService.getDBConnection()).thenReturn(connection);
    when(connection.createStatement()).thenReturn(statement);
    when(statement.executeQuery(anyString())).thenReturn(resultSet);
    when(resultSet.getString("CLUSTERID")).thenAnswer((Answer<String>) invocation -> CLUSTER_ID);
    when(resultSet.getString("INSTANCETYPE")).thenAnswer((Answer<String>) invocation -> INSTANCE_TYPE);
    when(resultSet.getDouble("COST")).thenAnswer((Answer<Double>) invocation -> COST);
    when(resultSet.getDouble("CPUCOST")).thenAnswer((Answer<Double>) invocation -> COST / 2);
    when(resultSet.getDouble("MEMORYCOST")).thenAnswer((Answer<Double>) invocation -> COST / 2);
    when(resultSet.getString("BILLINGACCOUNTID")).thenAnswer((Answer<String>) invocation -> BILLING_ACCOUNT_ID);
    when(resultSet.getString("ACCOUNTID")).thenAnswer((Answer<String>) invocation -> ACCOUNT_ID);
    when(resultSet.getString("CLUSTERNAME")).thenAnswer((Answer<String>) invocation -> CLUSTER_NAME);
    when(resultSet.getString("SETTINGID")).thenAnswer((Answer<String>) invocation -> SETTING_ID);
    when(resultSet.getString("REGION")).thenAnswer((Answer<String>) invocation -> REGION);
    when(resultSet.getString("CLOUDPROVIDER")).thenAnswer((Answer<String>) invocation -> CLOUD_PROVIDER);
    when(resultSet.getString("CLUSTERTYPE")).thenAnswer((Answer<String>) invocation -> CLUSTER_TYPE);
    when(resultSet.getString("WORKLOADTYPE")).thenAnswer((Answer<String>) invocation -> WORKLOAD_TYPE);

    returnResultSet(1);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testUnallocatedCostDataService() throws SQLException {
    when(timeScaleDBService.isValid()).thenReturn(true);
    when(statement.execute()).thenReturn(true);
    List<UnallocatedCostData> unallocatedCostDataList =
        unallocatedBillingDataService.getUnallocatedCostData(ACCOUNT_ID, START_TIME, END_TIME);
    assertThat(unallocatedCostDataList.get(0).getClusterId()).isEqualTo(CLUSTER_ID);
    assertThat(unallocatedCostDataList.get(0).getInstanceType()).isEqualTo(INSTANCE_TYPE);
    assertThat(unallocatedCostDataList.get(0).getCost()).isEqualTo(COST);
    assertThat(unallocatedCostDataList.get(0).getCpuCost()).isEqualTo(COST / 2);
    assertThat(unallocatedCostDataList.get(0).getMemoryCost()).isEqualTo(COST / 2);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testNullUnallocatedCostData() {
    when(timeScaleDBService.getDBConnection()).thenThrow(SQLException.class);
    assertThat(unallocatedBillingDataService.getUnallocatedCostData(ACCOUNT_ID, START_TIME, END_TIME))
        .isEqualTo(Collections.emptyList());
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testGetCommonDataService() throws SQLException {
    when(timeScaleDBService.isValid()).thenReturn(true);
    when(statement.execute()).thenReturn(true);
    ClusterCostData clusterCostData =
        unallocatedBillingDataService.getCommonFields(ACCOUNT_ID, CLUSTER_ID, START_TIME, END_TIME);
    assertThat(clusterCostData.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(clusterCostData.getBillingAccountId()).isEqualTo(BILLING_ACCOUNT_ID);
    assertThat(clusterCostData.getClusterName()).isEqualTo(CLUSTER_NAME);
    assertThat(clusterCostData.getSettingId()).isEqualTo(SETTING_ID);
    assertThat(clusterCostData.getRegion()).isEqualTo(REGION);
    assertThat(clusterCostData.getCloudProvider()).isEqualTo(CLOUD_PROVIDER);
    assertThat(clusterCostData.getClusterType()).isEqualTo(CLUSTER_TYPE);
    assertThat(clusterCostData.getWorkloadType()).isEqualTo(WORKLOAD_TYPE);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testNullCommonData() {
    when(timeScaleDBService.getDBConnection()).thenThrow(SQLException.class);
    assertThat(unallocatedBillingDataService.getCommonFields(ACCOUNT_ID, CLUSTER_ID, START_TIME, END_TIME))
        .isEqualTo(ClusterCostData.builder().build());
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
