package software.wings.delegatetasks.citasks;

import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.SHUBHAM;
import static junit.framework.TestCase.assertEquals;
import static org.joor.Reflect.on;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.rule.Owner;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.DelegateTaskPackage;
import software.wings.beans.TaskType;
import software.wings.beans.ci.ExecuteCommandTaskParams;
import software.wings.delegatetasks.citasks.cik8handler.K8ExecuteCommandTaskHandler;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;

import java.util.ArrayList;
import java.util.List;

public class ExecuteCommandTaskTest extends WingsBaseTest {
  @Mock private K8ExecuteCommandTaskHandler k8ExecuteCommandTaskHandler;

  @InjectMocks
  private ExecuteCommandTask task = (ExecuteCommandTask) TaskType.EXECUTE_COMMAND.getDelegateRunnableTask(
      DelegateTaskPackage.builder()
          .delegateId("delegateid")
          .delegateTask(
              DelegateTask.builder().data(TaskData.builder().timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build()).build())
          .build(),
      notifyResponseData -> {}, () -> true);

  @Before
  public void setUp() throws Exception {
    on(task).set("executeCommandTaskHandler", k8ExecuteCommandTaskHandler);
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