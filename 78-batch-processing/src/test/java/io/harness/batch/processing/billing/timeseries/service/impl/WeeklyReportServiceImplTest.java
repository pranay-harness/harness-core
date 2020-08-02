package io.harness.batch.processing.billing.timeseries.service.impl;

import static io.harness.rule.OwnerRule.SHUBHANSHU;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.batch.processing.billing.timeseries.helper.WeeklyReportTemplateHelper;
import io.harness.batch.processing.mail.CEMailNotificationService;
import io.harness.batch.processing.shard.AccountShardService;
import io.harness.category.element.UnitTests;
import io.harness.ccm.communication.CECommunicationsServiceImpl;
import io.harness.ccm.communication.CESlackWebhookService;
import io.harness.ccm.communication.entities.CECommunications;
import io.harness.ccm.communication.entities.CESlackWebhook;
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
import org.mockito.stubbing.Answer;
import software.wings.beans.Account;
import software.wings.service.impl.instance.CloudToHarnessMappingServiceImpl;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

@RunWith(MockitoJUnitRunner.class)
public class WeeklyReportServiceImplTest extends CategoryTest {
  @Mock private TimeScaleDBService timeScaleDBService;
  @Mock private CEMailNotificationService emailNotificationService;
  @Mock private CloudToHarnessMappingServiceImpl cloudToHarnessMappingService;
  @Mock private WeeklyReportTemplateHelper templateHelper;
  @Mock private CECommunicationsServiceImpl ceCommunicationsService;
  @Mock private AccountShardService accountShardService;
  @Mock private CESlackWebhookService ceSlackWebhookService;
  @InjectMocks private WeeklyReportServiceImpl weeklyReportService;

  @Mock Statement statement;
  @Mock ResultSet resultSet;

  final int[] count = {0};
  // Result set fields
  private static final String COST_CHANGE = "COST_CHANGE";
  private static final String COST_DIFF = "COST_DIFF";
  private static final String CURRENT_WEEK_COST = "CURRENT_WEEK_COST";
  private static final String IDLE_COST_CHANGE = "IDLE_COST_CHANGE";
  private static final String IDLE_COST_DIFF = "IDLE_COST_DIFF";
  private static final String CURRENT_WEEK_IDLE_COST = "CURRENT_WEEK_IDLE_COST";
  private static final String UNALLOCATED_COST_CHANGE = "UNALLOCATED_COST_CHANGE";
  private static final String UNALLOCATED_COST_DIFF = "UNALLOCATED_COST_DIFF";
  private static final String ENTITY = "ENTITY";
  private static final String ACCOUNT_ID = "ACCOUNT_ID";

  @Before
  public void setup() throws SQLException {
    MockitoAnnotations.initMocks(this);
    Connection mockConnection = mock(Connection.class);
    Statement mockStatement = mock(Statement.class);
    when(timeScaleDBService.getDBConnection()).thenReturn(mockConnection);
    when(timeScaleDBService.isValid()).thenReturn(true);
    when(mockConnection.createStatement()).thenReturn(mockStatement);
    when(mockStatement.executeQuery(anyString())).thenReturn(resetCountAndReturnResultSet());
    mockResultSet();
    when(emailNotificationService.send(any())).thenReturn(true);
    when(accountShardService.getCeEnabledAccounts())
        .thenReturn(Arrays.asList(Account.Builder.anAccount().withUuid(ACCOUNT_ID).build()));
    when(ceCommunicationsService.getEnabledEntries(any(), any()))
        .thenReturn(Arrays.asList(CECommunications.builder().emailId("mailId").build()));
    when(ceSlackWebhookService.getByAccountId(ACCOUNT_ID))
        .thenReturn(CESlackWebhook.builder().webhookUrl("URL").sendCostReport(false).build());
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void shouldHandle() {
    weeklyReportService.generateAndSendWeeklyReport();
    verify(emailNotificationService).send(any());
  }

  private void mockResultSet() throws SQLException {
    Connection connection = mock(Connection.class);
    statement = mock(Statement.class);
    resultSet = mock(ResultSet.class);
    when(timeScaleDBService.getDBConnection()).thenReturn(connection);
    when(connection.createStatement()).thenReturn(statement);
    when(statement.executeQuery(anyString())).thenReturn(resetCountAndReturnResultSet());

    when(resultSet.getString(ENTITY)).thenAnswer((Answer<String>) invocation -> ENTITY);
    when(resultSet.getString(ACCOUNT_ID)).thenAnswer((Answer<String>) invocation -> ACCOUNT_ID);
    when(resultSet.getDouble(COST_DIFF)).thenAnswer((Answer<Double>) invocation -> 100.0);
    when(resultSet.getDouble(COST_CHANGE)).thenAnswer((Answer<Double>) invocation -> 10.0);
    when(resultSet.getDouble(CURRENT_WEEK_COST)).thenAnswer((Answer<Double>) invocation -> 1100.0);
    when(resultSet.getDouble(IDLE_COST_DIFF)).thenAnswer((Answer<Double>) invocation -> 100.0);
    when(resultSet.getDouble(IDLE_COST_CHANGE)).thenAnswer((Answer<Double>) invocation -> 10.0);
    when(resultSet.getDouble(CURRENT_WEEK_IDLE_COST)).thenAnswer((Answer<Double>) invocation -> 1100.0);
    when(resultSet.getDouble(UNALLOCATED_COST_DIFF)).thenAnswer((Answer<Double>) invocation -> 100.0);
    when(resultSet.getDouble(UNALLOCATED_COST_CHANGE)).thenAnswer((Answer<Double>) invocation -> 10.0);
    when(resultSet.getDouble(CURRENT_WEEK_COST)).thenAnswer((Answer<Double>) invocation -> 1100.0);
    returnResultSet(1);
  }

  private void returnResultSet(int limit) throws SQLException {
    when(resultSet.next()).then((Answer<Boolean>) invocation -> {
      if (count[0] < limit) {
        count[0]++;
        return true;
      }
      count[0] = 0;
      return false;
    });
  }

  private ResultSet resetCountAndReturnResultSet() {
    count[0] = 0;
    return resultSet;
  }
}
