package software.wings.delegatetasks.k8s.taskhandler;

import static io.harness.rule.OwnerRule.ANSHUL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import io.harness.category.element.UnitTests;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.k8s.K8sDelegateTaskParams;
import software.wings.delegatetasks.k8s.K8sTaskHelper;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;
import software.wings.helpers.ext.k8s.request.K8sScaleTaskParameters;

public class K8sScaleTaskHandlerTest extends WingsBaseTest {
  @Mock private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Mock private K8sTaskHelper k8sTaskHelper;
  @InjectMocks private K8sScaleTaskHandler k8sScaleTaskHandler;

  private K8sScaleTaskParameters k8sScaleTaskParameters;
  private KubernetesConfig kubernetesConfig;
  private K8sDelegateTaskParams k8sDelegateTaskParams;
  private static final String kubeConfigNamespace = "kubeConfigNamespace";
  private static final String releaseName = "releaseName";
  private static final String workload = "deployment/workload";

  @Before
  public void setup() {
    k8sScaleTaskParameters = K8sScaleTaskParameters.builder()
                                 .accountId(ACCOUNT_ID)
                                 .instanceUnitType(InstanceUnitType.COUNT)
                                 .instances(1)
                                 .releaseName(releaseName)
                                 .workload(workload)
                                 .build();
    kubernetesConfig = KubernetesConfig.builder().namespace(kubeConfigNamespace).accountId(ACCOUNT_ID).build();
    k8sDelegateTaskParams = K8sDelegateTaskParams.builder().build();
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testExecuteForNamespaceFromKubeConfig() throws Exception {
    when(containerDeploymentDelegateHelper.getKubernetesConfig(any(K8sClusterConfig.class)))
        .thenReturn(kubernetesConfig);
    when(k8sTaskHelper.getPodDetails(kubernetesConfig, kubeConfigNamespace, releaseName)).thenReturn(null);
    when(k8sTaskHelper.scale(any(Kubectl.class), any(K8sDelegateTaskParams.class), any(KubernetesResourceId.class),
             anyInt(), any(ExecutionLogCallback.class)))
        .thenReturn(false);

    k8sScaleTaskHandler.executeTaskInternal(k8sScaleTaskParameters, k8sDelegateTaskParams);
    verify(k8sTaskHelper, times(1))
        .scale(any(Kubectl.class), any(K8sDelegateTaskParams.class), any(KubernetesResourceId.class), anyInt(),
            any(ExecutionLogCallback.class));
    ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(k8sTaskHelper, times(1)).getPodDetails(any(KubernetesConfig.class), argumentCaptor.capture(), anyString());
    assertThat(argumentCaptor.getValue()).isEqualTo(kubeConfigNamespace);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testExecuteForNamespaceFromWorkload() throws Exception {
    String namespace = "namespace";
    String namespacedWorkload = "namespace/deployment/workload";
    k8sScaleTaskParameters.setWorkload(namespacedWorkload);

    when(containerDeploymentDelegateHelper.getKubernetesConfig(any(K8sClusterConfig.class)))
        .thenReturn(kubernetesConfig);
    when(k8sTaskHelper.getPodDetails(kubernetesConfig, namespace, releaseName)).thenReturn(null);
    when(k8sTaskHelper.scale(any(Kubectl.class), any(K8sDelegateTaskParams.class), any(KubernetesResourceId.class),
             anyInt(), any(ExecutionLogCallback.class)))
        .thenReturn(false);

    k8sScaleTaskHandler.executeTaskInternal(k8sScaleTaskParameters, k8sDelegateTaskParams);
    verify(k8sTaskHelper, times(1))
        .scale(any(Kubectl.class), any(K8sDelegateTaskParams.class), any(KubernetesResourceId.class), anyInt(),
            any(ExecutionLogCallback.class));
    ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(k8sTaskHelper, times(1)).getPodDetails(any(KubernetesConfig.class), argumentCaptor.capture(), anyString());
    assertThat(argumentCaptor.getValue()).isEqualTo(namespace);
  }
}
