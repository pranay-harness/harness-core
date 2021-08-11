package io.harness.pms.sdk.core.execution;

import static io.harness.rule.OwnerRule.SAHIL;

import static org.mockito.Mockito.verify;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.sdk.core.PmsSdkCoreTestBase;
import io.harness.rule.Owner;

import java.util.HashMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

@OwnedBy(HarnessTeam.PIPELINE)
public class AsyncSdkResumeCallbackTest extends PmsSdkCoreTestBase {
  private static String NODE_EXECUTION_ID = "nodeId";
  private static String PLAN_EXECUTION_ID = "planExecutionId";

  @Mock SdkNodeExecutionService sdkNodeExecutionService;
  AsyncSdkResumeCallback asyncSdkResumeCallback;

  @Before
  public void setup() {
    asyncSdkResumeCallback = AsyncSdkResumeCallback.builder()
                                 .sdkNodeExecutionService(sdkNodeExecutionService)
                                 .nodeExecutionId(NODE_EXECUTION_ID)
                                 .planExecutionId(PLAN_EXECUTION_ID)
                                 .build();
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testNotify() {
    asyncSdkResumeCallback.notify(new HashMap<>());
    verify(sdkNodeExecutionService).resumeNodeExecution(PLAN_EXECUTION_ID, NODE_EXECUTION_ID, new HashMap<>(), false);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testNotifyError() {
    asyncSdkResumeCallback.notifyError(new HashMap<>());
    verify(sdkNodeExecutionService).resumeNodeExecution(PLAN_EXECUTION_ID, NODE_EXECUTION_ID, new HashMap<>(), true);
  }
}