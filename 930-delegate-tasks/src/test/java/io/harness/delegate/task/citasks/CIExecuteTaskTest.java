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
import io.harness.delegate.beans.ci.CIExecuteStepTaskParams;
import io.harness.delegate.beans.ci.k8s.CIK8ExecuteStepTaskParams;
import io.harness.delegate.beans.ci.k8s.K8sTaskExecutionResponse;
import io.harness.rule.Owner;

import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class CIExecuteTaskTest extends CategoryTest {
  @Mock @Named(CITaskConstants.EXECUTE_STEP_K8) private CIExecuteStepTaskHandler ciK8ExecuteStepTaskHandler;
  @Mock @Named(CITaskConstants.EXECUTE_STEP_AWS_VM) private CIExecuteStepTaskHandler ciAwsVmExecuteStepTaskHandler;

  @InjectMocks
  private final CIExecuteStepTask task =
      new CIExecuteStepTask(DelegateTaskPackage.builder()
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
    CIExecuteStepTaskParams params = CIK8ExecuteStepTaskParams.builder().build();
    K8sTaskExecutionResponse response = mock(K8sTaskExecutionResponse.class);
    when(ciK8ExecuteStepTaskHandler.executeTaskInternal(params)).thenReturn(response);
    assertEquals(task.run(params), response);
  }

  @Test(expected = NotImplementedException.class)
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void runWithObjectParams() {
    CIExecuteStepTaskParams taskParams = mock(CIExecuteStepTaskParams.class);
    List<Object> params = new ArrayList<>();
    params.add(taskParams);

    task.run(params.toArray());
  }
}
