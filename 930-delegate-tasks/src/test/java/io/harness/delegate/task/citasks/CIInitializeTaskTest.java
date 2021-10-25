package io.harness.delegate.task.citasks;

import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.SHUBHAM;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.ci.CIInitializeTaskParams;
import io.harness.delegate.beans.ci.k8s.K8sTaskExecutionResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.rule.Owner;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class CIInitializeTaskTest extends CategoryTest {
  @Mock private CIInitializeTaskHandler ciInitializeTaskHandler;
  @Mock private ILogStreamingTaskClient logStreamingTaskClient;

  @InjectMocks
  private CIInitializeTask task =
      new CIInitializeTask(DelegateTaskPackage.builder()
                               .delegateId("delegateid")
                               .data(TaskData.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build())
                               .build(),
          null, notifyResponseData -> {}, () -> true);

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void runWithTaskParams() {
    CIInitializeTaskParams params = mock(CIInitializeTaskParams.class);
    K8sTaskExecutionResponse response = mock(K8sTaskExecutionResponse.class);
    when(ciInitializeTaskHandler.executeTaskInternal(params, logStreamingTaskClient)).thenReturn(response);
    assertEquals(task.run(params), response);
  }

  @Test(expected = NotImplementedException.class)
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void runWithObjectParams() {
    CIInitializeTaskParams taskParams = mock(CIInitializeTaskParams.class);
    List<Object> params = new ArrayList<>();
    params.add(taskParams);

    task.run(params.toArray());
  }
}
