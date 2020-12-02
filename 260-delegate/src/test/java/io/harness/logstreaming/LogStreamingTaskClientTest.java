package io.harness.logstreaming;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.logstreaming.LogStreamingTaskClient.COMMAND_UNIT_PLACEHOLDER;
import static io.harness.rule.OwnerRule.MARKO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.logstreaming.LogLine;
import io.harness.delegate.beans.logstreaming.LogStreamingSanitizer;
import io.harness.exception.InvalidArgumentsException;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;
import io.harness.rule.Owner;

import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.DelegateLogService;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.zeroturnaround.exec.stream.LogOutputStream;

public class LogStreamingTaskClientTest extends CategoryTest {
  private final DelegateLogService logServiceMock = mock(DelegateLogService.class);
  private final DelegateAgentLogStreamingClient delegateAgentLogStreamingClientMock =
      mock(DelegateAgentLogStreamingClient.class);
  private final LogStreamingSanitizer logStreamingSanitizerMock = mock(LogStreamingSanitizer.class);

  private static final String ACCOUNT_ID = generateUuid();
  private static final String TOKEN = generateUuid();
  private static final String BASE_LOG_KEY = generateUuid();
  private static final String APP_ID = generateUuid();
  private static final String ACTIVITY_ID = generateUuid();

  private LogStreamingTaskClient completeLogStreamingTaskClient =
      LogStreamingTaskClient.builder()
          .delegateAgentLogStreamingClient(delegateAgentLogStreamingClientMock)
          .accountId(ACCOUNT_ID)
          .token(TOKEN)
          .logStreamingSanitizer(logStreamingSanitizerMock)
          .baseLogKey(BASE_LOG_KEY)
          .logService(logServiceMock)
          .appId(APP_ID)
          .activityId(ACTIVITY_ID)
          .build();

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldInvokeOpenLogStreamWithoutKeySuffix() {
    completeLogStreamingTaskClient.openStream(null);
    verify(delegateAgentLogStreamingClientMock).openLogStream(TOKEN, ACCOUNT_ID, BASE_LOG_KEY);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldInvokeOpenLogStreamWithKeySuffix() {
    completeLogStreamingTaskClient.openStream("keySuffix");
    verify(delegateAgentLogStreamingClientMock)
        .openLogStream(TOKEN, ACCOUNT_ID, BASE_LOG_KEY + String.format(COMMAND_UNIT_PLACEHOLDER, "keySuffix"));
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldInvokeCloseLogStreamWithoutKeySuffix() {
    completeLogStreamingTaskClient.closeStream(null);
    verify(delegateAgentLogStreamingClientMock).closeLogStream(TOKEN, ACCOUNT_ID, BASE_LOG_KEY, true);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldInvokeCloseLogStreamWithKeySuffix() {
    completeLogStreamingTaskClient.closeStream("keySuffix");
    verify(delegateAgentLogStreamingClientMock)
        .closeLogStream(TOKEN, ACCOUNT_ID, BASE_LOG_KEY + String.format(COMMAND_UNIT_PLACEHOLDER, "keySuffix"), true);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldFailWritingLogsWithException() {
    assertThatThrownBy(() -> completeLogStreamingTaskClient.writeLogLine(null, null))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessage("Log line parameter is mandatory.");
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldInvokePushMessageWithoutKeySuffix() {
    LogLine logLine = mock(LogLine.class);

    completeLogStreamingTaskClient.writeLogLine(logLine, null);

    verify(logStreamingSanitizerMock).sanitizeLogMessage(logLine);

    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    verify(delegateAgentLogStreamingClientMock)
        .pushMessage(eq(TOKEN), eq(ACCOUNT_ID), eq(BASE_LOG_KEY), captor.capture());
    List logLines = captor.getValue();
    assertThat(logLines).containsExactly(logLine);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldInvokePushMessageWithKeySuffix() {
    LogLine logLine = mock(LogLine.class);

    completeLogStreamingTaskClient.writeLogLine(logLine, "keySuffix");

    verify(logStreamingSanitizerMock).sanitizeLogMessage(logLine);

    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    verify(delegateAgentLogStreamingClientMock)
        .pushMessage(eq(TOKEN), eq(ACCOUNT_ID), eq(BASE_LOG_KEY + String.format(COMMAND_UNIT_PLACEHOLDER, "keySuffix")),
            captor.capture());
    List logLines = captor.getValue();
    assertThat(logLines).containsExactly(logLine);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldReturnOutputStreamInstance() {
    try (
        OutputStream outputStream = completeLogStreamingTaskClient.obtainLogOutputStream(LogLevel.ERROR, "keySuffix")) {
      assertThat(outputStream).isInstanceOf(LogOutputStream.class);

      String logMessage = "test message";
      outputStream.write(logMessage.getBytes());
      outputStream.flush();

      ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
      verify(delegateAgentLogStreamingClientMock)
          .pushMessage(eq(TOKEN), eq(ACCOUNT_ID),
              eq(BASE_LOG_KEY + String.format(COMMAND_UNIT_PLACEHOLDER, "keySuffix")), captor.capture());

      List<LogLine> logLines = (List<LogLine>) captor.getValue();
      assertThat(logLines).hasSize(1);
      assertThat(logLines.get(0).getMessage()).isEqualTo(logMessage);
      assertThat(logLines.get(0).getLevel()).isEqualTo(LogLevel.ERROR);
    } catch (IOException e) {
      fail("Unexpected failure during test execution");
    }
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldReturnExecutionLogCallbackInstance() {
    LogCallback logCallback = completeLogStreamingTaskClient.obtainLogCallback("commandName");

    assertThat(logCallback).isNotNull();
    assertThat(logCallback).isInstanceOf(ExecutionLogCallback.class);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldThrowExceptionWhenAppIdOrActivityIdAreNotPresent() {
    // Test no appId scenario
    LogStreamingTaskClient logStreamingTaskClientWithoutAppId =
        LogStreamingTaskClient.builder()
            .delegateAgentLogStreamingClient(delegateAgentLogStreamingClientMock)
            .accountId(ACCOUNT_ID)
            .token(TOKEN)
            .logStreamingSanitizer(logStreamingSanitizerMock)
            .baseLogKey(BASE_LOG_KEY)
            .logService(logServiceMock)
            .activityId(ACTIVITY_ID)
            .build();

    assertThatThrownBy(() -> logStreamingTaskClientWithoutAppId.obtainLogCallback("commandName"))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessage(
            "Application id and activity id were not available as part of task params. Please make sure that task params class implements Cd1ApplicationAccess and ActivityAccess interfaces.");

    // Test no activityId scenario
    LogStreamingTaskClient logStreamingTaskClientWithoutActivityId =
        LogStreamingTaskClient.builder()
            .delegateAgentLogStreamingClient(delegateAgentLogStreamingClientMock)
            .accountId(ACCOUNT_ID)
            .token(TOKEN)
            .logStreamingSanitizer(logStreamingSanitizerMock)
            .baseLogKey(BASE_LOG_KEY)
            .logService(logServiceMock)
            .appId(APP_ID)
            .build();

    assertThatThrownBy(() -> logStreamingTaskClientWithoutActivityId.obtainLogCallback("commandName"))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessage(
            "Application id and activity id were not available as part of task params. Please make sure that task params class implements Cd1ApplicationAccess and ActivityAccess interfaces.");
  }
}
