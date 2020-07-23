package software.wings.utils;

import static io.harness.logging.CommandExecutionStatus.RUNNING;
import static io.harness.logging.LogLevel.INFO;
import static io.harness.rule.OwnerRule.PUNEET;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.COMMAND_UNIT_NAME;
import static software.wings.utils.WingsTestConstants.WORKFLOW_EXECUTION_ID;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import software.wings.delegatetasks.DelegateLogService;

public class ExecutionLogWriterTest extends CategoryTest {
  @Mock private DelegateLogService logService;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void smokeTest() {
    ExecutionLogWriter testWriter = ExecutionLogWriter.builder()
                                        .accountId(ACCOUNT_ID)
                                        .appId(APP_ID)
                                        .commandUnitName(COMMAND_UNIT_NAME)
                                        .executionId(WORKFLOW_EXECUTION_ID)
                                        .hostName("localhost")
                                        .logService(logService)
                                        .stringBuilder(new StringBuilder(1024))
                                        .logLevel(INFO)
                                        .build();

    String logLineFirstSegment = "Hello ";
    String logLineSecondSegment = "World";
    testWriter.write(logLineFirstSegment.toCharArray(), 0, logLineFirstSegment.length());
    testWriter.write(logLineSecondSegment.toCharArray(), 0, logLineSecondSegment.length());

    String newLine = "\n";
    testWriter.write(newLine.toCharArray(), 0, 1);

    Mockito.verify(logService)
        .save(ACCOUNT_ID,
            aLog()
                .withAppId(APP_ID)
                .withActivityId(WORKFLOW_EXECUTION_ID)
                .withLogLevel(INFO)
                .withCommandUnitName(COMMAND_UNIT_NAME)
                .withHostName("localhost")
                .withLogLine(logLineFirstSegment + logLineSecondSegment)
                .withExecutionResult(RUNNING)
                .build());
  }
}
