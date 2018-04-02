package software.wings.service;

import static org.junit.Assert.assertEquals;
import static software.wings.service.impl.LogServiceImpl.NUM_OF_LOGS_TO_KEEP;

import com.google.inject.Inject;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.Whitebox;
import software.wings.WingsBaseTest;
import software.wings.beans.Activity;
import software.wings.beans.Log;
import software.wings.beans.Log.Builder;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.dl.WingsPersistence;
import software.wings.scheduler.DataCleanUpJob;
import software.wings.service.impl.LogServiceImpl;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.LogService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by peeyushaggarwal on 5/27/16.
 */
public class LogPurgeTest extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private LogService logService;
  @Mock private ActivityService activityService;

  @Test
  public void shouldPurgeLogs() {
    Whitebox.setInternalState(logService, "activityService", activityService);

    final int numOfActivities = 5;
    final int numOfLogLines = LogServiceImpl.NUM_OF_LOGS_TO_KEEP + 100;

    List<String> activityIds = new ArrayList<>();
    for (int activityNum = 0; activityNum < numOfActivities; activityNum++) {
      Activity activity = Activity.builder().commandName(UUID.randomUUID().toString()).build();
      String activityId = wingsPersistence.save(activity);
      activityIds.add(activityId);
      for (int logLine = 0; logLine < numOfLogLines; logLine++) {
        String logMessage = "logMessage act: " + activityNum + " line: " + logLine;
        Log log = Builder.aLog()
                      .withActivityId(activityId)
                      .withLogLine(logMessage)
                      .withLogLevel(LogLevel.INFO)
                      .withCommandUnitName(UUID.randomUUID().toString())
                      .withExecutionResult(CommandExecutionStatus.SUCCESS)
                      .withAppId(UUID.randomUUID().toString())
                      .withCreatedAt(System.currentTimeMillis() - DataCleanUpJob.LOGS_RETENTION_TIME
                          - TimeUnit.MINUTES.toMillis(10))
                      .build();
        logService.save(log);
      }
    }

    // verify logs being saved
    for (int activityNum = 0; activityNum < numOfActivities; activityNum++) {
      String activityId = activityIds.get(activityNum);
      List<Log> logs =
          wingsPersistence.createQuery(Log.class).filter("activityId", activityId).order("createdAt").asList();
      assertEquals(numOfLogLines, logs.size());
      for (int logLine = 0; logLine < numOfLogLines; logLine++) {
        String logMessage = "logMessage act: " + activityNum + " line: " + logLine;
        assertEquals(logMessage, logs.get(logLine).getLogLine());
      }
    }

    logService.purgeActivityLogs();

    // verify logs being purged
    int logsDeleted = numOfLogLines - NUM_OF_LOGS_TO_KEEP;
    for (int activityNum = 0; activityNum < numOfActivities; activityNum++) {
      String activityId = activityIds.get(activityNum);
      List<Log> logs =
          wingsPersistence.createQuery(Log.class).filter("activityId", activityId).order("createdAt").asList();
      assertEquals(NUM_OF_LOGS_TO_KEEP, logs.size());
      //      for(int logLine = 0; logLine < NUM_OF_LOGS_TO_KEEP; logLine++) {
      //        String logMessage = "logMessage act: " + activityNum + " line: " + (logsDeleted + logLine);
      //        assertEquals(logMessage, logs.get(logLine).getLogLine());
      //      }
    }
  }
}
