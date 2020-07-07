package software.wings.delegatetasks.k8s.taskhandler;

import static io.harness.rule.OwnerRule.BOJANA;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.category.element.UnitTests;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.exception.InvalidArgumentsException;
import io.harness.k8s.model.K8sPod;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.KubernetesConfig;
import software.wings.delegatetasks.k8s.K8sDelegateTaskParams;
import software.wings.delegatetasks.k8s.K8sTaskHelper;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;
import software.wings.helpers.ext.k8s.request.K8sInstanceSyncTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sTaskResponse;

import java.util.Arrays;
import java.util.List;

public class K8sInstanceSyncTaskHandlerTest extends WingsBaseTest {
  @Mock private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Mock private K8sTaskHelper k8sTaskHelper;
  @InjectMocks private K8sInstanceSyncTaskHandler k8sInstanceSyncTaskHandler;

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void invalidTypeOfTaskParams() {
    assertThatExceptionOfType(InvalidArgumentsException.class)
        .isThrownBy(() -> k8sInstanceSyncTaskHandler.executeTaskInternal(null, null))
        .withMessageContaining("INVALID_ARGUMENT");
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void executeTaskInternalSuccess() throws Exception {
    doReturn(KubernetesConfig.builder().build())
        .when(containerDeploymentDelegateHelper)
        .getKubernetesConfig(any(K8sClusterConfig.class));

    List<K8sPod> podsList = Arrays.asList(K8sPod.builder().build());
    doReturn(podsList)
        .when(k8sTaskHelper)
        .getPodDetails(any(KubernetesConfig.class), anyString(), anyString(), anyLong());

    k8sInstanceSyncTaskHandler.executeTaskInternal(
        K8sInstanceSyncTaskParameters.builder().build(), K8sDelegateTaskParams.builder().build());
    verify(k8sTaskHelper, times(1)).getPodDetails(any(KubernetesConfig.class), anyString(), anyString(), anyLong());
    verify(k8sTaskHelper, times(1))
        .getK8sTaskExecutionResponse(
            any(K8sTaskResponse.class), any(CommandExecutionResult.CommandExecutionStatus.class));
    verify(k8sTaskHelper, times(1)).getPodDetails(any(KubernetesConfig.class), anyString(), anyString(), anyLong());
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void executeTaskInternalNoPods() throws Exception {
    doReturn(KubernetesConfig.builder().build())
        .when(containerDeploymentDelegateHelper)
        .getKubernetesConfig(any(K8sClusterConfig.class));

    k8sInstanceSyncTaskHandler.executeTaskInternal(
        K8sInstanceSyncTaskParameters.builder().build(), K8sDelegateTaskParams.builder().build());
    verify(k8sTaskHelper, times(1)).getPodDetails(any(KubernetesConfig.class), anyString(), anyString(), anyLong());
    verify(k8sTaskHelper, times(1))
        .getK8sTaskExecutionResponse(
            any(K8sTaskResponse.class), any(CommandExecutionResult.CommandExecutionStatus.class));
    verify(k8sTaskHelper, times(1)).getPodDetails(any(KubernetesConfig.class), anyString(), anyString(), anyLong());
  }
}