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
import software.wings.beans.ci.CICleanupTaskParams;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;

import java.util.ArrayList;
import java.util.List;

public class CICleanupTaskTest extends WingsBaseTest {
  @Mock private CICleanupTaskHandler ciCleanupTaskHandler;

  @InjectMocks
  private CICleanupTask task = (CICleanupTask) TaskType.CI_CLEANUP.getDelegateRunnableTask(
      DelegateTaskPackage.builder()
          .delegateId("delegateid")
          .delegateTask(DelegateTask.builder()
                            .data(TaskData.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build())
                            .build())
          .build(),
      notifyResponseData -> {}, () -> true);

  @Before
  public void setUp() throws Exception {
    on(task).set("ciCleanupTaskHandler", ciCleanupTaskHandler);
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void runWithTaskParams() {
    CICleanupTaskParams params = mock(CICleanupTaskParams.class);
    K8sTaskExecutionResponse response = mock(K8sTaskExecutionResponse.class);
    when(ciCleanupTaskHandler.executeTaskInternal(params)).thenReturn(response);
    assertEquals(task.run(params), response);
  }

  @Test(expected = NotImplementedException.class)
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void runWithObjectParams() {
    CICleanupTaskParams taskParams = mock(CICleanupTaskParams.class);
    List<Object> params = new ArrayList<>();
    params.add(taskParams);

    task.run(params.toArray());
  }
}