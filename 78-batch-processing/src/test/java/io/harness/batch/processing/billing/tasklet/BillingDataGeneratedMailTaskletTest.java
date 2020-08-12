package io.harness.batch.processing.billing.tasklet;

import static io.harness.rule.OwnerRule.SHUBHANSHU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.batch.processing.billing.tasklet.dao.intfc.DataGeneratedNotificationDao;
import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.mail.CEMailNotificationService;
import io.harness.category.element.UnitTests;
import io.harness.ccm.cluster.entities.CEUserInfo;
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
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.test.context.ActiveProfiles;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.service.intfc.instance.CloudToHarnessMappingService;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

@ActiveProfiles("test")
@RunWith(MockitoJUnitRunner.class)
public class BillingDataGeneratedMailTaskletTest extends CategoryTest {
  @Mock private CloudToHarnessMappingService cloudToHarnessMappingService;
  @Mock private DataGeneratedNotificationDao notificationDao;
  @Mock private TimeScaleDBService timeScaleDBService;
  @Mock private DataFetcherUtils utils;
  @Mock private CEMailNotificationService emailNotificationService;
  @Mock private JobParameters parameters;
  @InjectMocks private BillingDataGeneratedMailTasklet tasklet;

  @Mock Statement statement;
  @Mock ResultSet resultSet;

  private static final long TIME = System.currentTimeMillis();
  private static final String ACCOUNT_ID = "ACCOUNT_ID";
  final int[] count = {0};

  @Before
  public void setup() throws SQLException {
    MockitoAnnotations.initMocks(this);
    Connection connection = mock(Connection.class);
    statement = mock(Statement.class);
    resultSet = mock(ResultSet.class);
    when(timeScaleDBService.isValid()).thenReturn(true);
    when(timeScaleDBService.getDBConnection()).thenReturn(connection);
    when(connection.createStatement()).thenReturn(statement);
    when(statement.executeQuery(anyString())).thenReturn(resultSet);
    when(resultSet.getString("CLUSTERID")).thenAnswer((Answer<String>) invocation -> "CLUSTERID");
    when(resultSet.getString("CLUSTERNAME")).thenAnswer((Answer<String>) invocation -> "CLUSTERNAME");
    when(resultSet.getTimestamp("STARTTIME", utils.getDefaultCalendar()))
        .thenAnswer((Answer<Timestamp>) invocation -> new Timestamp(TIME));
    returnResultSet(1);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testExecute() throws Exception {
    ChunkContext chunkContext = mock(ChunkContext.class);
    StepContext stepContext = mock(StepContext.class);
    StepExecution stepExecution = mock(StepExecution.class);
    JobParameters parameters = mock(JobParameters.class);

    when(chunkContext.getStepContext()).thenReturn(stepContext);
    when(stepContext.getStepExecution()).thenReturn(stepExecution);
    when(stepExecution.getJobParameters()).thenReturn(parameters);

    when(parameters.getString(CCMJobConstants.ACCOUNT_ID)).thenReturn(ACCOUNT_ID);
    when(notificationDao.isMailSent(ACCOUNT_ID)).thenReturn(true);
    when(cloudToHarnessMappingService.getUserForCluster("CLUSTERID"))
        .thenReturn(CEUserInfo.builder().name("user").email("user@harness.io").build());
    RepeatStatus repeatStatus = tasklet.execute(null, chunkContext);
    assertThat(repeatStatus).isNull();

    when(notificationDao.isMailSent(ACCOUNT_ID)).thenReturn(false);
    repeatStatus = tasklet.execute(null, chunkContext);
    assertThat(repeatStatus).isNull();
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
}
