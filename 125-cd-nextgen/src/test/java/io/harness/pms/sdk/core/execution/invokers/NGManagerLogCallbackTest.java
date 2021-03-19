package io.harness.pms.sdk.core.execution.invokers;

import static io.harness.rule.OwnerRule.SAHIL;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.logStreaming.LogStreamingStepClientFactory;
import io.harness.logStreaming.LogStreamingStepClientImpl;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogLevel;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.rule.Owner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class NGManagerLogCallbackTest extends CategoryTest {
  private static String LOG_SUFFIX = "logSuffix";

  @Mock LogStreamingStepClientFactory logStreamingStepClientFactory;
  @Mock LogStreamingStepClientImpl logStreamingStepClient;

  private NGManagerLogCallback ngManagerLogCallback;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    Ambiance ambiance = Ambiance.newBuilder().build();
    when(logStreamingStepClientFactory.getLogStreamingStepClient(ambiance)).thenReturn(logStreamingStepClient);
    ngManagerLogCallback = new NGManagerLogCallback(logStreamingStepClientFactory, ambiance, LOG_SUFFIX, false);
  }

  @After
  public void verifyInteractions() {
    verifyNoMoreInteractions(logStreamingStepClient);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testSaveExecutionLogWithOneParam() {
    ngManagerLogCallback.saveExecutionLog("test");
    verify(logStreamingStepClient).writeLogLine(any(), eq(LOG_SUFFIX));
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testSaveExecutionLogWithTwoParam() {
    ngManagerLogCallback.saveExecutionLog("line", LogLevel.INFO);
    verify(logStreamingStepClient).writeLogLine(any(), eq(LOG_SUFFIX));
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testSaveExecutionLogWithThreeParam() {
    ngManagerLogCallback.saveExecutionLog("line", LogLevel.INFO, CommandExecutionStatus.SUCCESS);
    verify(logStreamingStepClient).writeLogLine(any(), eq(LOG_SUFFIX));
    verify(logStreamingStepClient).closeStream(eq(LOG_SUFFIX));
  }
}