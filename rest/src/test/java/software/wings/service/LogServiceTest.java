package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.SearchFilter.Builder.aSearchFilter;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.COMMAND_UNIT_NAME;
import static software.wings.utils.WingsTestConstants.HOST_NAME;
import static software.wings.utils.WingsTestConstants.LOG_ID;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.mongodb.BasicDBObject;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.beans.Log;
import software.wings.beans.Log.Builder;
import software.wings.dl.PageRequest;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.LogService;

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
                                             .withLogLevel(INFO);

  @Mock private WingsPersistence wingsPersistence;
  @Inject @InjectMocks private LogService logService;

  @Inject @Named("primaryDatastore") private Datastore datastore;

  /**
   * Should listStateMachines logs.
   */
  @Test
  public void shouldListLogs() {
    PageRequest pageRequest = aPageRequest()
                                  .addFilter(aSearchFilter()
                                                 .withField("appId", EQ, APP_ID)
                                                 .withField("activityId", EQ, ACTIVITY_ID)
                                                 .withField("commandUnitName", EQ, COMMAND_UNIT_NAME)
                                                 .build())
                                  .build();
    logService.list(pageRequest);
    verify(wingsPersistence).query(eq(Log.class), eq(pageRequest));
  }

  /**
   * Should save log.
   */
  @Test
  public void shouldSaveLog() {
    when(wingsPersistence.saveAndGet(eq(Log.class), eq(BUILDER.build()))).thenReturn(BUILDER.withUuid(LOG_ID).build());
    logService.save(BUILDER.build());
    verify(wingsPersistence).saveAndGet(eq(Log.class), eq(BUILDER.build()));
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
