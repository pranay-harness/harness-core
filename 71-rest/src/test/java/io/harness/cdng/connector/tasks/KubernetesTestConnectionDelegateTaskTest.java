package io.harness.cdng.connector.tasks;

import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.DEEPAK;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.cdng.connector.service.KubernetesConnectorDelegateService;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesConnectionTaskParams;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.DelegateTaskPackage;
import software.wings.beans.TaskType;

public class KubernetesTestConnectionDelegateTaskTest extends WingsBaseTest {
  @Mock KubernetesConnectorDelegateService kubernetesConnectorDelegateService;

  @InjectMocks
  private KubernetesTestConnectionDelegateTask kubernetesTestConnectionDelegateTask =
      (KubernetesTestConnectionDelegateTask) TaskType.VALIDATE_KUBERNETES_CONFIG.getDelegateRunnableTask(
          DelegateTaskPackage.builder()
              .delegateId("delegateid")
              .delegateTask(DelegateTask.builder()
                                .data((TaskData.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT))
                                          .parameters(new Object[] {
                                              KubernetesConnectionTaskParams.builder()
                                                  .kubernetesClusterConfig(KubernetesClusterConfigDTO.builder().build())
                                                  .build()})
                                          .build())
                                .build())
              .build(),
          notifyResponseData -> {}, () -> true);

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void run() {
    kubernetesTestConnectionDelegateTask.run();
    verify(kubernetesConnectorDelegateService, times(1)).validate(any());
  }
}