package software.wings.service;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.service.impl.LogServiceImpl.MAX_LOG_ROWS_PER_ACTIVITY;
import static software.wings.utils.WingsTestConstants.HOST_NAME;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.Log;
import software.wings.service.intfc.LogService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LogServiceTest extends WingsBaseTest {
  @Inject private LogService logService;

  private String appId;
  private String activityId;
  private String unitName;

  @Before
  public void setUp() throws Exception {
    appId = generateUuid();
    activityId = generateUuid();
    unitName = generateUuid();
  }

  /**
   * Should list logs.
   */
  @Test
  public void shouldListLogs() {
    List<Log> logs = getLogsToSave(100);
    logService.batchedSave(logs);

    PageRequest pageRequest = aPageRequest()
                                  .addFilter("appId", EQ, appId)
                                  .addFilter("activityId", EQ, activityId)
                                  .addFilter("commandUnitName", EQ, unitName)
                                  .build();
    assertEquals(100, logService.list(appId, pageRequest).size());
  }

  @Test
  public void shouldNotSaveMoreThanLimit() {
    List<Log> logs = getLogsToSave(2 * MAX_LOG_ROWS_PER_ACTIVITY);
    logService.batchedSave(logs);

    PageRequest pageRequest = aPageRequest()
                                  .addFilter("appId", EQ, appId)
                                  .addFilter("activityId", EQ, activityId)
                                  .addFilter("commandUnitName", EQ, unitName)
                                  .build();
    assertEquals(MAX_LOG_ROWS_PER_ACTIVITY, logService.list(appId, pageRequest).size());
  }

  @Test
  public void testExportLogs() throws IOException {
    List<Log> logs = getLogsToSave(100);
    logService.batchedSave(logs);
    File file = logService.exportLogs(appId, activityId);
    List<String> logLines = FileUtils.readLines(file, "UTF-8");
    assertEquals(100, logLines.size());
    for (int i = 0; i < 100; i++) {
      assertTrue(logLines.get(i).endsWith("log-" + i));
    }
  }

  private List<Log> getLogsToSave(int numOfLogLines) {
    List<Log> logs = new ArrayList<>();
    for (int i = 0; i < numOfLogLines; i++) {
      logs.add(aLog()
                   .withAppId(appId)
                   .withActivityId(activityId)
                   .withHostName(HOST_NAME)
                   .withLogLine("log-" + i)
                   .withCommandUnitName(unitName)
                   .withLogLevel(INFO)
                   .withExecutionResult(CommandExecutionStatus.RUNNING)
                   .build());
    }
    return logs;
  }
}
