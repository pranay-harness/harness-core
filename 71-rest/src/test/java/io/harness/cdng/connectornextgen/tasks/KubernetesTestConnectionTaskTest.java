package io.harness.cdng.connectornextgen.tasks;

import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.DEEPAK;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.cdng.connectornextgen.service.KubernetesConnectorService;
import io.harness.connector.apis.dtos.K8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.TaskData;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.DelegateTaskPackage;
import software.wings.beans.TaskType;

public class KubernetesTestConnectionTaskTest extends WingsBaseTest {
  @Mock KubernetesConnectorService kubernetesConnectorService;

  @InjectMocks
  private KubernetesTestConnectionTask kubernetesTestConnectionTask =
      (KubernetesTestConnectionTask) TaskType.VALIDATE_KUBERNETES_CONFIG.getDelegateRunnableTask(
          DelegateTaskPackage.builder()
              .delegateId("delegateid")
              .delegateTask(
                  DelegateTask.builder()
                      .data((TaskData.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT))
                                .parameters(new Object[] {null, null, KubernetesClusterConfigDTO.builder().build()})
                                .build())
                      .build())
              .build(),
          notifyResponseData -> {}, () -> true);

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void run() {
    kubernetesTestConnectionTask.run();
    verify(kubernetesConnectorService, times(1)).validate(any());
  }
}