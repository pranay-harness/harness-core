package software.wings.service;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.RAGHU;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.service.impl.LogServiceImpl.MAX_LOG_ROWS_PER_ACTIVITY;
import static software.wings.utils.WingsTestConstants.HOST_NAME;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.rule.Owner;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
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
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void shouldListLogs() {
    List<Log> logs = getLogsToSave(100);
    logService.batchedSave(logs);

    PageRequest pageRequest = aPageRequest()
                                  .addFilter("appId", EQ, appId)
                                  .addFilter("activityId", EQ, activityId)
                                  .addFilter("commandUnitName", EQ, unitName)
                                  .build();
    assertThat(logService.list(appId, pageRequest)).hasSize(100);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void shouldNotSaveMoreThanLimit() {
    List<Log> logs = getLogsToSave(2 * MAX_LOG_ROWS_PER_ACTIVITY);
    logService.batchedSave(logs);

    PageRequest pageRequest = aPageRequest()
                                  .addFilter("appId", EQ, appId)
                                  .addFilter("activityId", EQ, activityId)
                                  .addFilter("commandUnitName", EQ, unitName)
                                  .build();
    assertThat(logService.list(appId, pageRequest)).hasSize(MAX_LOG_ROWS_PER_ACTIVITY);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testExportLogs() throws IOException {
    List<Log> logs = getLogsToSave(100);
    logService.batchedSave(logs);
    File file = logService.exportLogs(appId, activityId);
    List<String> logLines = FileUtils.readLines(file, "UTF-8");
    assertThat(logLines).hasSize(100);
    for (int i = 0; i < 100; i++) {
      assertThat(logLines.get(i).endsWith("log-" + i)).isTrue();
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
