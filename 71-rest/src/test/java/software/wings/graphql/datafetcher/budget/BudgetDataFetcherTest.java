package software.wings.graphql.datafetcher.budget;

import static io.harness.rule.OwnerRule.SHUBHANSHU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.ccm.budget.entities.ApplicationBudgetScope;
import io.harness.ccm.budget.entities.Budget;
import io.harness.ccm.budget.entities.Budget.BudgetBuilder;
import io.harness.ccm.budget.entities.BudgetType;
import io.harness.ccm.budget.entities.ClusterBudgetScope;
import io.harness.exception.InvalidRequestException;
import io.harness.persistence.HPersistence;
import io.harness.rule.OwnerRule.Owner;
import io.harness.timescaledb.TimeScaleDBService;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.User;
import software.wings.graphql.datafetcher.AbstractDataFetcherTest;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.BillingDataMetaDataFields;
import software.wings.graphql.schema.query.QLBudgetQueryParameters;
import software.wings.graphql.schema.type.aggregation.budget.QLBudgetTableListData;
import software.wings.security.UserThreadLocal;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

public class BudgetDataFetcherTest extends AbstractDataFetcherTest {
  @Mock TimeScaleDBService timeScaleDBService;
  @Mock HPersistence persistence;
  @Mock private DataFetcherUtils utils;
  @Inject @InjectMocks BudgetDataFetcher budgetDataFetcher;

  @Mock Statement statement;
  @Mock ResultSet resultSet;

  final int[] count = {0};
  final double[] doubleVal = {0};
  final long currentTime = System.currentTimeMillis();
  final long[] calendar = {currentTime};
  final String budgetId = "budgetId";
  final String accountId = "accountId";
  final String budgetName = "budgetName";
  final String[] clusterIds = {"clusterId"};
  final String[] appIds = {"appId"};
  final BudgetType budgetType = BudgetType.SPECIFIED_AMOUNT;
  final EnvironmentType environmentType = EnvironmentType.PROD;
  final long createdAt = System.currentTimeMillis();
  final long lastUpdatedAt = System.currentTimeMillis();
  final double budgetAmount = 25000.0;
  final QLBudgetQueryParameters queryParameters = new QLBudgetQueryParameters(budgetId);
  private static final long DAY_IN_MILLI_SECONDS = 86400000L;

  @Before
  public void setup() throws SQLException {
    User user = testUtils.createUser(testUtils.createAccount());
    UserThreadLocal.set(user);

    // Account1
    createAccount(ACCOUNT1_ID, getLicenseInfo());
    createApp(ACCOUNT1_ID, APP1_ID_ACCOUNT1, APP1_ID_ACCOUNT1, TAG_TEAM, TAG_VALUE_TEAM1);

    Connection mockConnection = mock(Connection.class);
    Statement mockStatement = mock(Statement.class);
    when(persistence.get(Budget.class, budgetId)).thenReturn(mockBudget("CLUSTER"));
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
  public void testGetBudgetDataWhenBudgetIdIsInvalid() {
    when(persistence.get(Budget.class, budgetId)).thenReturn(null);
    assertThatThrownBy(() -> budgetDataFetcher.fetch(queryParameters, accountId))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testFetchMethodInBudgetDataFetcherForClusterType() {
    QLBudgetTableListData data = budgetDataFetcher.fetch(queryParameters, accountId);
    assertThat(data.getData().get(0).getActualCost()).isEqualTo(12500.0);
    assertThat(data.getData().get(0).getBudgeted()).isEqualTo(budgetAmount);
    assertThat(data.getData().get(0).getBudgetVariance()).isEqualTo(12500.0);
    assertThat(data.getData().get(0).getBudgetVariancePercentage()).isEqualTo(50.0);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testFetchMethodInBudgetDataFetcherForApplicationType() {
    when(persistence.get(Budget.class, budgetId)).thenReturn(mockBudget("APPLICATION"));
    QLBudgetTableListData data = budgetDataFetcher.fetch(queryParameters, accountId);
    assertThat(data.getData().get(0).getActualCost()).isEqualTo(12500.0);
    assertThat(data.getData().get(0).getBudgeted()).isEqualTo(budgetAmount);
    assertThat(data.getData().get(0).getBudgetVariance()).isEqualTo(12500.0);
    assertThat(data.getData().get(0).getBudgetVariancePercentage()).isEqualTo(50.0);
  }

  private void mockResultSet() throws SQLException {
    Connection connection = mock(Connection.class);
    statement = mock(Statement.class);
    resultSet = mock(ResultSet.class);
    when(timeScaleDBService.getDBConnection()).thenReturn(connection);
    when(connection.createStatement()).thenReturn(statement);
    when(statement.executeQuery(anyString())).thenReturn(resultSet);

    when(resultSet.getDouble("COST")).thenAnswer((Answer<Double>) invocation -> 12500.0 + doubleVal[0]++);
    when(resultSet.getTimestamp(BillingDataMetaDataFields.TIME_SERIES.getFieldName(), utils.getDefaultCalendar()))
        .thenAnswer((Answer<Timestamp>) invocation -> {
          calendar[0] = calendar[0] + 3600000;
          return new Timestamp(calendar[0]);
        });
    returnResultSet(5);
  }

  private Budget mockBudget(String scope) {
    BudgetBuilder budgetBuilder = Budget.builder()
                                      .uuid(budgetId)
                                      .accountId(accountId)
                                      .name(budgetName)
                                      .createdAt(createdAt)
                                      .lastUpdatedAt(lastUpdatedAt)
                                      .type(budgetType)
                                      .budgetAmount(budgetAmount);
    if (scope.equals("CLUSTER")) {
      budgetBuilder.scope(ClusterBudgetScope.builder().clusterIds(clusterIds).build());
    } else {
      budgetBuilder.scope(ApplicationBudgetScope.builder().applicationIds(appIds).type(environmentType).build());
    }
    return budgetBuilder.build();
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
