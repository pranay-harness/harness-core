package software.wings.service;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.COMMAND_UNIT_NAME;
import static software.wings.utils.WingsTestConstants.HOST_NAME;
import static software.wings.utils.WingsTestConstants.LOG_ID;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import io.harness.beans.PageRequest;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.beans.Log;
import software.wings.beans.Log.Builder;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.LogServiceImpl;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.LogService;

import java.util.List;
import java.util.Map;

public class LogServiceTest extends WingsBaseTest {
  private static final Builder BUILDER = aLog()
                                             .withAppId(APP_ID)
                                             .withActivityId(ACTIVITY_ID)
                                             .withHostName(HOST_NAME)
                                             .withLogLine("INFO 1 2 3")
                                             .withCommandUnitName(COMMAND_UNIT_NAME)
                                             .withLogLevel(INFO)
                                             .withExecutionResult(CommandExecutionStatus.RUNNING);

  @Inject private WingsPersistence wingsPersistence;
  @Mock private WingsPersistence mockWingsPersistence;
  @Mock private ActivityService activityService;
  @Inject @InjectMocks private LogService logService;

  @Mock Query<Log> query;
  @Mock FieldEnd end;

  @Before
  public void setUp() throws Exception {
    when(mockWingsPersistence.createQuery(Log.class)).thenReturn(query);
    when(query.filter(any(), any())).thenReturn(query);
  }

  /**
   * Should list logs.
   */
  @Test
  public void shouldListLogs() {
    PageRequest pageRequest = aPageRequest()
                                  .addFilter("appId", EQ, APP_ID)
                                  .addFilter("activityId", EQ, ACTIVITY_ID)
                                  .addFilter("commandUnitName", EQ, COMMAND_UNIT_NAME)
                                  .build();
    logService.list(pageRequest);
    verify(mockWingsPersistence).query(eq(Log.class), eq(pageRequest));
  }

  /**
   * Should save log.
   */
  @Test
  @Ignore
  public void shouldSaveLog() {
    when(mockWingsPersistence.save(any(List.class))).thenReturn(ImmutableList.of(LOG_ID));
    logService.save(BUILDER.build());
    verify(mockWingsPersistence).save(any(List.class));
    verify(activityService).updateCommandUnitStatus(any(Map.class));
  }

  /**
   * Should get unit execution result.
   */
  @Test
  public void shouldGetUnitExecutionResult() {
    Query<Log> logQuery = wingsPersistence.createQuery(Log.class);
    when(mockWingsPersistence.createQuery(Log.class)).thenReturn(logQuery);
    logService.getUnitExecutionResult(APP_ID, ACTIVITY_ID, COMMAND_UNIT_NAME);
    assertThat(logQuery.getQueryObject().get("appId")).isEqualTo(APP_ID);
    assertThat(logQuery.getQueryObject().get("activityId")).isEqualTo(ACTIVITY_ID);
    assertThat(logQuery.getQueryObject().get("commandExecutionStatus")).isEqualTo(new BasicDBObject("$exists", true));
    assertThat(logQuery.getSortObject().get("lastUpdatedAt")).isEqualTo(-1);
  }

  @Test
  public void shouldIgnoreLogsOverMaximumLogThreshold() {
    when(query.count()).thenReturn((long) (LogServiceImpl.MAX_LOG_ROWS_PER_ACTIVITY + 1));
    when(mockWingsPersistence.save(any(Log.class))).thenReturn(LOG_ID);
    String logId = logService.batchedSaveCommandUnitLogs(ACTIVITY_ID, COMMAND_UNIT_NAME, BUILDER.build());
    assertThat(logId).isEqualTo(null);
    verify(query).filter("appId", APP_ID);
    verify(query).filter("activityId", ACTIVITY_ID);
    verify(query).count();
  }
}
