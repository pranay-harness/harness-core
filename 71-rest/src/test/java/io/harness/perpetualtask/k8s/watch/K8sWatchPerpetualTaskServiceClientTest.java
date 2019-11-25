package io.harness.perpetualtask.k8s.watch;

import static io.harness.rule.OwnerRule.HANTANG;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class K8sWatchPerpetualTaskServiceClientTest extends CategoryTest {
  private String accountId = "ACCOUNT_ID";
  private String cloudProviderId = "CLOUD_PROVIDER_ID";
  private String taskId = "TASK_ID";

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock PerpetualTaskService perpetualTaskService;
  @InjectMocks K8sWatchPerpetualTaskServiceClient k8SWatchPerpetualTaskServiceClient;

  @Before
  public void setUp() {
    when(perpetualTaskService.createTask(eq(PerpetualTaskType.K8S_WATCH), eq(accountId),
             isA(PerpetualTaskClientContext.class), isA(PerpetualTaskSchedule.class), eq(false)))
        .thenReturn(taskId);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testCreate() {
    K8WatchPerpetualTaskClientParams params =
        new K8WatchPerpetualTaskClientParams(cloudProviderId, "Pod", "clusterId", "clusterName");
    k8SWatchPerpetualTaskServiceClient.create(accountId, params);
    verify(perpetualTaskService)
        .createTask(eq(PerpetualTaskType.K8S_WATCH), eq(accountId), isA(PerpetualTaskClientContext.class),
            isA(PerpetualTaskSchedule.class), eq(false));
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testDelete() {
    k8SWatchPerpetualTaskServiceClient.delete(accountId, taskId);
    verify(perpetualTaskService).deleteTask(accountId, taskId);
  }
}
