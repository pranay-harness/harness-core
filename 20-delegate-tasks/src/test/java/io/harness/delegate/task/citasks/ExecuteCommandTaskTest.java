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
import io.harness.delegate.beans.ci.ExecuteCommandTaskParams;
import io.harness.delegate.beans.ci.k8s.K8sTaskExecutionResponse;
import io.harness.delegate.task.citasks.cik8handler.K8ExecuteCommandTaskHandler;
import io.harness.rule.Owner;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

public class ExecuteCommandTaskTest extends CategoryTest {
  @Mock private K8ExecuteCommandTaskHandler k8ExecuteCommandTaskHandler;

  @InjectMocks
  private final ExecuteCommandTask task =
      new ExecuteCommandTask(DelegateTaskPackage.builder()
                                 .delegateId("delegateid")
                                 .data(TaskData.builder().timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build())
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
    ExecuteCommandTaskParams params = mock(ExecuteCommandTaskParams.class);
    K8sTaskExecutionResponse response = mock(K8sTaskExecutionResponse.class);
    when(k8ExecuteCommandTaskHandler.executeTaskInternal(params)).thenReturn(response);
    assertEquals(task.run(params), response);
  }

  @Test(expected = NotImplementedException.class)
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void runWithObjectParams() {
    ExecuteCommandTaskParams taskParams = mock(ExecuteCommandTaskParams.class);
    List<Object> params = new ArrayList<>();
    params.add(taskParams);

    task.run(params.toArray());
  }
}
