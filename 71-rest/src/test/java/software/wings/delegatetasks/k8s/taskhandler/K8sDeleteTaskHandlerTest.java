package software.wings.delegatetasks.k8s.taskhandler;

import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static software.wings.delegatetasks.k8s.K8sTestHelper.deployment;
import static software.wings.delegatetasks.k8s.K8sTestHelper.deploymentConfig;
import static software.wings.delegatetasks.k8s.K8sTestHelper.namespace;
import static software.wings.delegatetasks.k8s.K8sTestHelper.service;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidArgumentsException;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.k8s.K8sDelegateTaskParams;
import software.wings.delegatetasks.k8s.K8sTaskHelper;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sApplyTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;
import software.wings.helpers.ext.k8s.request.K8sDeleteTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sDeleteResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;

import java.util.List;
import java.util.stream.Collectors;

public class K8sDeleteTaskHandlerTest extends WingsBaseTest {
  @Mock private K8sTaskHelper k8sTaskHelper;
  @Mock private ContainerDeploymentDelegateHelper deploymentDelegateHelper;
  @InjectMocks private K8sDeleteTaskHandler handler;

  private final K8sDeleteTaskParameters deleteTaskParameters =
      K8sDeleteTaskParameters.builder().resources("default/Deployment/test,default/Service/test-svc").build();
  private final K8sDelegateTaskParams taskParams =
      K8sDelegateTaskParams.builder().kubectlPath("kubectl").kubeconfigPath("kubeconfig").workingDirectory(".").build();
  private final KubernetesConfig kubernetesConfig = KubernetesConfig.builder().build();

  @Before
  public void setUp() throws Exception {
    doReturn(kubernetesConfig).when(deploymentDelegateHelper).getKubernetesConfig(any(K8sClusterConfig.class));
    doReturn(asList(deployment().getResourceId(), service().getResourceId(), deploymentConfig().getResourceId(),
                 namespace().getResourceId()))
        .when(k8sTaskHelper)
        .getResourceIdsForDeletion(
            any(K8sDeleteTaskParameters.class), eq(kubernetesConfig), any(ExecutionLogCallback.class));
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void executeTaskNoResources() throws Exception {
    K8sDeleteTaskParameters deleteAllParams = K8sDeleteTaskParameters.builder().build();
    final K8sTaskExecutionResponse taskResponse = handler.executeTaskInternal(deleteAllParams, taskParams);

    verify(k8sTaskHelper, times(1)).getK8sTaskExecutionResponse(K8sDeleteResponse.builder().build(), SUCCESS);
    verify(k8sTaskHelper, never()).delete(any(Kubectl.class), any(), any(), any(ExecutionLogCallback.class));
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void exceptionInReadingResources() throws Exception {
    K8sDeleteTaskParameters deleteAllParams = K8sDeleteTaskParameters.builder().resources("random").build();
    final K8sTaskExecutionResponse taskResponse = handler.executeTaskInternal(deleteAllParams, taskParams);

    verify(k8sTaskHelper, never()).delete(any(Kubectl.class), any(), any(), any(ExecutionLogCallback.class));
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void deleteSpecificResources() throws Exception {
    final K8sTaskExecutionResponse taskResponse = handler.executeTaskInternal(deleteTaskParameters, taskParams);

    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    verify(k8sTaskHelper, times(1))
        .delete(any(Kubectl.class), eq(taskParams), captor.capture(), any(ExecutionLogCallback.class));

    @SuppressWarnings("unchecked") List<KubernetesResourceId> deletedResources = captor.getValue();

    assertThat(deletedResources).hasSize(2);
    assertThat(deletedResources.stream().map(KubernetesResourceId::getKind).collect(Collectors.toSet()))
        .containsExactlyInAnyOrder("Deployment", "Service");
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void deleteIfStarGiveInResources() throws Exception {
    K8sDeleteTaskParameters deleteAllParams = K8sDeleteTaskParameters.builder().resources("*").build();
    final K8sTaskExecutionResponse taskResponse = handler.executeTaskInternal(deleteAllParams, taskParams);

    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    verify(k8sTaskHelper, times(1))
        .delete(any(Kubectl.class), eq(taskParams), captor.capture(), any(ExecutionLogCallback.class));

    @SuppressWarnings("unchecked") List<KubernetesResourceId> deletedResources = captor.getValue();

    assertThat(deletedResources).hasSize(4);
    assertThat(deletedResources.stream().map(KubernetesResourceId::getKind).collect(Collectors.toSet()))
        .containsExactlyInAnyOrder("Deployment", "DeploymentConfig", "Service", "Namespace");
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void invalidTaskParameters() {
    assertThatExceptionOfType(InvalidArgumentsException.class)
        .isThrownBy(() -> handler.executeTaskInternal(null, null));
    assertThatExceptionOfType(InvalidArgumentsException.class)
        .isThrownBy(() -> handler.executeTaskInternal(K8sApplyTaskParameters.builder().build(), null));
    assertThatExceptionOfType(InvalidArgumentsException.class)
        .isThrownBy(()
                        -> handler.executeTaskInternal(
                            K8sApplyTaskParameters.builder().build(), K8sDelegateTaskParams.builder().build()));
  }
}