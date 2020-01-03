package software.wings.graphql.datafetcher.billing;

import static io.harness.rule.OwnerRule.SHUBHANSHU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import io.harness.timescaledb.TimeScaleDBService;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import software.wings.beans.User;
import software.wings.graphql.datafetcher.AbstractDataFetcherTest;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.QLSortOrder;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingSortCriteria;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingSortType;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMEntityGroupBy;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMGroupBy;
import software.wings.graphql.schema.type.aggregation.billing.QLFilterValuesListData;
import software.wings.security.UserThreadLocal;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BillingStatsFilterValuesDataFetcherTest extends AbstractDataFetcherTest {
  @Mock TimeScaleDBService timeScaleDBService;
  @Mock private DataFetcherUtils utils;
  @Inject @InjectMocks BillingStatsFilterValuesDataFetcher billingStatsFilterValuesDataFetcher;

  @Mock Statement statement;
  @Mock ResultSet resultSet;

  final int[] count = {0};
  final double[] doubleVal = {0};

  @Before
  public void setup() throws SQLException {
    User user = testUtils.createUser(testUtils.createAccount());
    UserThreadLocal.set(user);

    // Account1
    createAccount(ACCOUNT1_ID, getLicenseInfo());
    createApp(ACCOUNT1_ID, APP1_ID_ACCOUNT1, APP1_ID_ACCOUNT1, TAG_TEAM, TAG_VALUE_TEAM1);

    Connection mockConnection = mock(Connection.class);
    Statement mockStatement = mock(Statement.class);
    when(timeScaleDBService.getDBConnection()).thenReturn(mockConnection);
    when(timeScaleDBService.isValid()).thenReturn(true);
    when(mockConnection.createStatement()).thenReturn(mockStatement);
    when(mockStatement.executeQuery(anyString())).thenReturn(resultSet);
    resetValues();
    mockResultSet();
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testGetBillingStatsFiltersWhenDbIsInvalid() {
    when(timeScaleDBService.isValid()).thenReturn(false);
    assertThatThrownBy(()
                           -> billingStatsFilterValuesDataFetcher.fetch(ACCOUNT1_ID, Collections.EMPTY_LIST,
                               Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testFetchMethodInBillingStatsFilterValuesDataFetcher() {
    List<QLBillingDataFilter> filters = new ArrayList<>();
    List<QLCCMGroupBy> groupBy =
        Arrays.asList(makeClusterEntityGroupBy(), makeClusterNameEntityGroupBy(), makeCloudProviderEntityGroupBy());
    List<QLBillingSortCriteria> sortCriteria = Arrays.asList(makeDescByTimeSortingCriteria());

    QLFilterValuesListData data = (QLFilterValuesListData) billingStatsFilterValuesDataFetcher.fetch(
        ACCOUNT1_ID, Collections.EMPTY_LIST, filters, groupBy, sortCriteria);

    assertThat(sortCriteria.get(0).getSortType()).isEqualTo(QLBillingSortType.Time);
    assertThat(sortCriteria.get(0).getSortOrder()).isEqualTo(QLSortOrder.DESCENDING);
    assertThat(data).isNotNull();
    assertThat(data.getData().get(0).getClusters().get(0).getId()).isEqualTo(CLUSTER1_ID);
    assertThat(data.getData().get(0).getClusters().get(0).getName()).isEqualTo(CLUSTER1_NAME);
    assertThat(data.getData().get(0).getClusters().get(0).getType()).isEqualTo(CLUSTER_TYPE1);
    assertThat(data.getData().get(0).getCloudProviders().get(0).getName()).isEqualTo(CLOUD_PROVIDER1_ID_ACCOUNT1);
    assertThat(data.getData().get(0).getCloudServiceNames().size()).isEqualTo(0);
    assertThat(data.getData().get(0).getInstanceIds().size()).isEqualTo(0);
    assertThat(data.getData().get(0).getLaunchTypes().size()).isEqualTo(0);
    assertThat(data.getData().get(0).getWorkloadNames().size()).isEqualTo(0);
    assertThat(data.getData().get(0).getNamespaces().size()).isEqualTo(0);
    assertThat(data.getData().get(0).getApplications().size()).isEqualTo(0);
    assertThat(data.getData().get(0).getEnvironments().size()).isEqualTo(0);
    assertThat(data.getData().get(0).getServices().size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testFetchMethodInBillingStatsFilterValuesDataFetcherWithoutClusterNameGroupBy() {
    List<QLBillingDataFilter> filters = new ArrayList<>();
    List<QLCCMGroupBy> groupBy =
        Arrays.asList(makeClusterEntityGroupBy(), makeClusterTypeEntityGroupBy(), makeCloudProviderEntityGroupBy());
    List<QLBillingSortCriteria> sortCriteria = Arrays.asList(makeDescByTimeSortingCriteria());

    QLFilterValuesListData data = (QLFilterValuesListData) billingStatsFilterValuesDataFetcher.fetch(
        ACCOUNT1_ID, Collections.EMPTY_LIST, filters, groupBy, sortCriteria);

    assertThat(sortCriteria.get(0).getSortType()).isEqualTo(QLBillingSortType.Time);
    assertThat(sortCriteria.get(0).getSortOrder()).isEqualTo(QLSortOrder.DESCENDING);
    assertThat(data).isNotNull();
    assertThat(data.getData().get(0).getClusters().get(0).getId()).isEqualTo(CLUSTER1_ID);
    assertThat(data.getData().get(0).getClusters().get(0).getName()).isEqualTo(CLUSTER1_NAME);
    assertThat(data.getData().get(0).getClusters().get(0).getType()).isEqualTo(CLUSTER_TYPE1);
    assertThat(data.getData().get(0).getCloudProviders().get(0).getName()).isEqualTo(CLOUD_PROVIDER1_ID_ACCOUNT1);
    assertThat(data.getData().get(0).getCloudServiceNames().size()).isEqualTo(0);
    assertThat(data.getData().get(0).getInstanceIds().size()).isEqualTo(0);
    assertThat(data.getData().get(0).getLaunchTypes().size()).isEqualTo(0);
    assertThat(data.getData().get(0).getWorkloadNames().size()).isEqualTo(0);
    assertThat(data.getData().get(0).getNamespaces().size()).isEqualTo(0);
    assertThat(data.getData().get(0).getApplications().size()).isEqualTo(0);
    assertThat(data.getData().get(0).getEnvironments().size()).isEqualTo(0);
    assertThat(data.getData().get(0).getServices().size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testFetchMethodInBillingStatsFilterValuesDataFetcherForApplications() {
    List<QLBillingDataFilter> filters = new ArrayList<>();
    List<QLCCMGroupBy> groupBy =
        Arrays.asList(makeApplicationEntityGroupBy(), makeServiceEntityGroupBy(), makeEnvironmentEntityGroupBy());
    List<QLBillingSortCriteria> sortCriteria = Arrays.asList(makeDescByTimeSortingCriteria());

    QLFilterValuesListData data = (QLFilterValuesListData) billingStatsFilterValuesDataFetcher.fetch(
        ACCOUNT1_ID, Collections.EMPTY_LIST, filters, groupBy, sortCriteria);

    assertThat(sortCriteria.get(0).getSortType()).isEqualTo(QLBillingSortType.Time);
    assertThat(sortCriteria.get(0).getSortOrder()).isEqualTo(QLSortOrder.DESCENDING);
    assertThat(data).isNotNull();
    assertThat(data.getData().get(0).getApplications().get(0).getName()).isEqualTo(APP1_ID_ACCOUNT1);
    assertThat(data.getData().get(0).getApplications().get(0).getId()).isEqualTo(APP1_ID_ACCOUNT1);
    assertThat(data.getData().get(0).getEnvironments().get(0).getName()).isEqualTo(ENV1_ID_APP1_ACCOUNT1);
    assertThat(data.getData().get(0).getEnvironments().get(0).getId()).isEqualTo(ENV1_ID_APP1_ACCOUNT1);
    assertThat(data.getData().get(0).getServices().get(0).getName()).isEqualTo(SERVICE1_ID_APP1_ACCOUNT1);
    assertThat(data.getData().get(0).getServices().get(0).getId()).isEqualTo(SERVICE1_ID_APP1_ACCOUNT1);
    assertThat(data.getData().get(0).getNamespaces().size()).isEqualTo(0);
    assertThat(data.getData().get(0).getInstanceIds().size()).isEqualTo(0);
    assertThat(data.getData().get(0).getLaunchTypes().size()).isEqualTo(0);
    assertThat(data.getData().get(0).getWorkloadNames().size()).isEqualTo(0);
    assertThat(data.getData().get(0).getNamespaces().size()).isEqualTo(0);
    assertThat(data.getData().get(0).getCloudProviders().size()).isEqualTo(0);
    assertThat(data.getData().get(0).getClusters().size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testFetchMethodInBillingStatsFilterValuesDataFetcherForKubernetes() {
    String[] clusterValues = new String[] {CLUSTER1_ID};

    List<QLBillingDataFilter> filters = Arrays.asList(makeClusterFilter(clusterValues));
    List<QLCCMGroupBy> groupBy = Arrays.asList(makeWorkloadNameEntityGroupBy(), makeNamespaceEntityGroupBy());
    List<QLBillingSortCriteria> sortCriteria = Arrays.asList(makeDescByTimeSortingCriteria());

    QLFilterValuesListData data = (QLFilterValuesListData) billingStatsFilterValuesDataFetcher.fetch(
        ACCOUNT1_ID, Collections.EMPTY_LIST, filters, groupBy, sortCriteria);

    assertThat(filters.get(0).getCluster().getOperator()).isEqualTo(QLIdOperator.EQUALS);
    assertThat(filters.get(0).getCluster().getValues()).isEqualTo(clusterValues);
    assertThat(sortCriteria.get(0).getSortType()).isEqualTo(QLBillingSortType.Time);
    assertThat(sortCriteria.get(0).getSortOrder()).isEqualTo(QLSortOrder.DESCENDING);
    assertThat(data).isNotNull();
    assertThat(data.getData().get(0).getWorkloadNames().get(0).getName()).isEqualTo(WORKLOAD_NAME_ACCOUNT1);
    assertThat(data.getData().get(0).getNamespaces().get(0).getName()).isEqualTo(NAMESPACE1);
    assertThat(data.getData().get(0).getCloudServiceNames().size()).isEqualTo(0);
    assertThat(data.getData().get(0).getInstanceIds().size()).isEqualTo(0);
    assertThat(data.getData().get(0).getLaunchTypes().size()).isEqualTo(0);
    assertThat(data.getData().get(0).getClusters().size()).isEqualTo(0);
    assertThat(data.getData().get(0).getApplications().size()).isEqualTo(0);
    assertThat(data.getData().get(0).getEnvironments().size()).isEqualTo(0);
    assertThat(data.getData().get(0).getServices().size()).isEqualTo(0);
    assertThat(data.getData().get(0).getCloudProviders().size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testFetchMethodInBillingStatsFilterValuesDataFetcherForEcs() {
    String[] clusterValues = new String[] {CLUSTER1_ID};

    List<QLBillingDataFilter> filters = Arrays.asList(makeClusterFilter(clusterValues));
    List<QLCCMGroupBy> groupBy = Arrays.asList(
        makeCloudServiceNameEntityGroupBy(), makeInstanceIdEntityGroupBy(), makeLaunchTypeEntityGroupBy());
    List<QLBillingSortCriteria> sortCriteria = Arrays.asList(makeDescByTimeSortingCriteria());

    QLFilterValuesListData data = (QLFilterValuesListData) billingStatsFilterValuesDataFetcher.fetch(
        ACCOUNT1_ID, Collections.EMPTY_LIST, filters, groupBy, sortCriteria);

    assertThat(filters.get(0).getCluster().getOperator()).isEqualTo(QLIdOperator.EQUALS);
    assertThat(filters.get(0).getCluster().getValues()).isEqualTo(clusterValues);
    assertThat(sortCriteria.get(0).getSortType()).isEqualTo(QLBillingSortType.Time);
    assertThat(sortCriteria.get(0).getSortOrder()).isEqualTo(QLSortOrder.DESCENDING);
    assertThat(data).isNotNull();
    assertThat(data.getData().get(0).getCloudServiceNames().get(0).getName()).isEqualTo(CLOUD_SERVICE_NAME_ACCOUNT1);
    assertThat(data.getData().get(0).getInstanceIds().get(0).getName())
        .isEqualTo(INSTANCE1_SERVICE1_ENV1_APP1_ACCOUNT1);
    assertThat(data.getData().get(0).getLaunchTypes().get(0).getName()).isEqualTo(LAUNCH_TYPE1);
    assertThat(data.getData().get(0).getWorkloadNames().size()).isEqualTo(0);
    assertThat(data.getData().get(0).getNamespaces().size()).isEqualTo(0);
    assertThat(data.getData().get(0).getClusters().size()).isEqualTo(0);
    assertThat(data.getData().get(0).getApplications().size()).isEqualTo(0);
    assertThat(data.getData().get(0).getEnvironments().size()).isEqualTo(0);
    assertThat(data.getData().get(0).getServices().size()).isEqualTo(0);
    assertThat(data.getData().get(0).getCloudProviders().size()).isEqualTo(0);
  }

  public QLBillingSortCriteria makeDescByTimeSortingCriteria() {
    return QLBillingSortCriteria.builder().sortOrder(QLSortOrder.DESCENDING).sortType(QLBillingSortType.Time).build();
  }

  public QLCCMGroupBy makeWorkloadNameEntityGroupBy() {
    QLCCMEntityGroupBy workloadNameGroupBy = QLCCMEntityGroupBy.WorkloadName;
    return QLCCMGroupBy.builder().entityGroupBy(workloadNameGroupBy).build();
  }

  public QLCCMGroupBy makeLaunchTypeEntityGroupBy() {
    QLCCMEntityGroupBy launchTypeGroupBy = QLCCMEntityGroupBy.LaunchType;
    return QLCCMGroupBy.builder().entityGroupBy(launchTypeGroupBy).build();
  }

  public QLCCMGroupBy makeNamespaceEntityGroupBy() {
    QLCCMEntityGroupBy namespaceGroupBy = QLCCMEntityGroupBy.Namespace;
    return QLCCMGroupBy.builder().entityGroupBy(namespaceGroupBy).build();
  }

  public QLCCMGroupBy makeInstanceIdEntityGroupBy() {
    QLCCMEntityGroupBy instanceIdGroupBy = QLCCMEntityGroupBy.InstanceId;
    return QLCCMGroupBy.builder().entityGroupBy(instanceIdGroupBy).build();
  }

  public QLCCMGroupBy makeCloudServiceNameEntityGroupBy() {
    QLCCMEntityGroupBy cloudServiceNameGroupBy = QLCCMEntityGroupBy.CloudServiceName;
    return QLCCMGroupBy.builder().entityGroupBy(cloudServiceNameGroupBy).build();
  }

  public QLCCMGroupBy makeClusterEntityGroupBy() {
    QLCCMEntityGroupBy clusterGroupBy = QLCCMEntityGroupBy.Cluster;
    return QLCCMGroupBy.builder().entityGroupBy(clusterGroupBy).build();
  }

  public QLCCMGroupBy makeClusterNameEntityGroupBy() {
    QLCCMEntityGroupBy clusterNameGroupBy = QLCCMEntityGroupBy.ClusterName;
    return QLCCMGroupBy.builder().entityGroupBy(clusterNameGroupBy).build();
  }

  public QLCCMGroupBy makeClusterTypeEntityGroupBy() {
    QLCCMEntityGroupBy clusterTypeGroupBy = QLCCMEntityGroupBy.ClusterType;
    return QLCCMGroupBy.builder().entityGroupBy(clusterTypeGroupBy).build();
  }

  public QLCCMGroupBy makeCloudProviderEntityGroupBy() {
    QLCCMEntityGroupBy cloudProviderGroupBy = QLCCMEntityGroupBy.CloudProvider;
    return QLCCMGroupBy.builder().entityGroupBy(cloudProviderGroupBy).build();
  }

  public QLCCMGroupBy makeApplicationEntityGroupBy() {
    QLCCMEntityGroupBy applicationGroupBy = QLCCMEntityGroupBy.Application;
    return QLCCMGroupBy.builder().entityGroupBy(applicationGroupBy).build();
  }

  public QLCCMGroupBy makeServiceEntityGroupBy() {
    QLCCMEntityGroupBy serviceGroupBy = QLCCMEntityGroupBy.Service;
    return QLCCMGroupBy.builder().entityGroupBy(serviceGroupBy).build();
  }

  public QLCCMGroupBy makeEnvironmentEntityGroupBy() {
    QLCCMEntityGroupBy environmentGroupBy = QLCCMEntityGroupBy.Environment;
    return QLCCMGroupBy.builder().entityGroupBy(environmentGroupBy).build();
  }

  public QLBillingDataFilter makeClusterFilter(String[] values) {
    QLIdFilter clusterFilter = QLIdFilter.builder().operator(QLIdOperator.EQUALS).values(values).build();
    return QLBillingDataFilter.builder().cluster(clusterFilter).build();
  }

  private void mockResultSet() throws SQLException {
    Connection connection = mock(Connection.class);
    statement = mock(Statement.class);
    resultSet = mock(ResultSet.class);
    when(timeScaleDBService.getDBConnection()).thenReturn(connection);
    when(connection.createStatement()).thenReturn(statement);
    when(statement.executeQuery(anyString())).thenReturn(resultSet);

    when(resultSet.getString("CLUSTERID")).thenAnswer((Answer<String>) invocation -> CLUSTER1_ID);
    when(resultSet.getString("CLUSTERNAME")).thenAnswer((Answer<String>) invocation -> CLUSTER1_NAME);
    when(resultSet.getString("CLUSTERTYPE")).thenAnswer((Answer<String>) invocation -> CLUSTER_TYPE1);
    when(resultSet.getString("APPID")).thenAnswer((Answer<String>) invocation -> APP1_ID_ACCOUNT1);
    when(resultSet.getString("ENVID")).thenAnswer((Answer<String>) invocation -> ENV1_ID_APP1_ACCOUNT1);
    when(resultSet.getString("CLOUDPROVIDERID")).thenAnswer((Answer<String>) invocation -> CLOUD_PROVIDER1_ID_ACCOUNT1);
    when(resultSet.getString("SERVICEID")).thenAnswer((Answer<String>) invocation -> SERVICE1_ID_APP1_ACCOUNT1);
    when(resultSet.getString("WORKLOADNAME")).thenAnswer((Answer<String>) invocation -> WORKLOAD_NAME_ACCOUNT1);
    when(resultSet.getString("NAMESPACE")).thenAnswer((Answer<String>) invocation -> NAMESPACE1);
    when(resultSet.getString("LAUNCHTYPE")).thenAnswer((Answer<String>) invocation -> LAUNCH_TYPE1);
    when(resultSet.getString("INSTANCEID"))
        .thenAnswer((Answer<String>) invocation -> INSTANCE1_SERVICE1_ENV1_APP1_ACCOUNT1);
    when(resultSet.getString("CLOUDSERVICENAME"))
        .thenAnswer((Answer<String>) invocation -> CLOUD_SERVICE_NAME_ACCOUNT1);
    returnResultSet(5);
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

  private void resetValues() {
    count[0] = 0;
    doubleVal[0] = 0;
  }
}
