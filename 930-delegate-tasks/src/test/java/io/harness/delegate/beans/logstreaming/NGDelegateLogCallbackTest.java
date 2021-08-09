package io.harness.delegate.beans.logstreaming;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.MockableTestMixin;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.taskprogress.ITaskProgressClient;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class NGDelegateLogCallbackTest extends CategoryTest implements MockableTestMixin {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock private ITaskProgressClient taskProgressClient;
  @Mock private ILogStreamingTaskClient logStreamingTaskClient;
  private CommandUnitsProgress commandUnitsProgress = CommandUnitsProgress.builder().build();

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testSendTaskProgressUpdate() {
    // should not throw Exception and should log command unit progress map
    commandUnitsProgress.getCommandUnitProgressMap().put(
        "key1", CommandUnitProgress.builder().status(CommandExecutionStatus.SUCCESS).build());
    NGDelegateLogCallback ngDelegateLogCallback =
        new NGDelegateLogCallback(logStreamingTaskClient, null, false, commandUnitsProgress);
    doThrow(new RuntimeException("failed")).when(taskProgressClient).sendTaskProgressUpdate(any());

    ngDelegateLogCallback.sendTaskProgressUpdate(taskProgressClient);
    verify(taskProgressClient, Mockito.times(1)).sendTaskProgressUpdate(any());
  }
}