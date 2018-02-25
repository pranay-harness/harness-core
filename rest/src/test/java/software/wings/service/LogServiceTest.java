package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.COMMAND_UNIT_NAME;
import static software.wings.utils.WingsTestConstants.HOST_NAME;
import static software.wings.utils.WingsTestConstants.LOG_ID;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.mongodb.BasicDBObject;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.beans.Log;
import software.wings.beans.Log.Builder;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.dl.PageRequest;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.LogService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by peeyushaggarwal on 5/27/16.
 */
public class LogServiceTest extends WingsBaseTest {
  private static final Builder BUILDER = aLog()
                                             .withAppId(APP_ID)
                                             .withActivityId(ACTIVITY_ID)
                                             .withHostName(HOST_NAME)
                                             .withLogLine("INFO 1 2 3")
                                             .withCommandUnitName(COMMAND_UNIT_NAME)
                                             .withLogLevel(INFO)
                                             .withExecutionResult(CommandExecutionStatus.RUNNING);

  @Mock private WingsPersistence wingsPersistence;
  @Mock private ActivityService activityService;
  @Inject @InjectMocks private LogService logService;

  @Inject @Named("primaryDatastore") private AdvancedDatastore datastore;

  private String appId = UUID.randomUUID().toString();
  private String activityId = UUID.randomUUID().toString();
  private String unitName = UUID.randomUUID().toString();
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
    logService.list(appId, activityId, unitName, pageRequest);
    verify(wingsPersistence).query(eq(Log.class), eq(pageRequest));
  }

  /**
   * Should save log.
   */
  @Test
  @Ignore
  public void shouldSaveLog() {
    when(wingsPersistence.save(any(List.class))).thenReturn(ImmutableList.of(LOG_ID));
    logService.save(BUILDER.build());
    verify(wingsPersistence).save(any(List.class));
    verify(activityService).updateCommandUnitStatus(any(Map.class));
  }

  /**
   * Should get unit execution result.
   */
  @Test
  public void shouldGetUnitExecutionResult() {
    Query<Log> logQuery = datastore.createQuery(Log.class);
    when(wingsPersistence.createQuery(Log.class)).thenReturn(logQuery);
    logService.getUnitExecutionResult(APP_ID, ACTIVITY_ID, COMMAND_UNIT_NAME);
    assertThat(logQuery.getQueryObject().get("appId")).isEqualTo(APP_ID);
    assertThat(logQuery.getQueryObject().get("activityId")).isEqualTo(ACTIVITY_ID);
    assertThat(logQuery.getQueryObject().get("commandExecutionStatus")).isEqualTo(new BasicDBObject("$exists", true));
    assertThat(logQuery.getSortObject().get("lastUpdatedAt")).isEqualTo(-1);
  }
}
