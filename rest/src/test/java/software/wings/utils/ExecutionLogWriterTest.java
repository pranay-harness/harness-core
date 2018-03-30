package software.wings.utils;

import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.RUNNING;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.COMMAND_UNIT_NAME;
import static software.wings.utils.WingsTestConstants.WORKFLOW_EXECUTION_ID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import software.wings.delegatetasks.DelegateLogService;

public class ExecutionLogWriterTest {
  @Mock private DelegateLogService logService;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void smokeTest() throws java.io.IOException {
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
