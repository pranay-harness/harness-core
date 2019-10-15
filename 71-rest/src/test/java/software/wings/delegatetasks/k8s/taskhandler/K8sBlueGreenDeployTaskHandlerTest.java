package software.wings.delegatetasks.k8s.taskhandler;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.delegatetasks.k8s.K8sDelegateTaskParams;
import software.wings.delegatetasks.k8s.K8sTaskHelper;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sBlueGreenDeployTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;

import java.util.Collections;

public class K8sBlueGreenDeployTaskHandlerTest extends WingsBaseTest {
  @Mock private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Mock private KubernetesContainerService kubernetesContainerService;
  @Mock private K8sTaskHelper k8sTaskHelper;

  @InjectMocks private K8sBlueGreenDeployTaskHandler k8sBlueGreenDeployTaskHandler;

  @Test
  @Category(UnitTests.class)
  public void testDryRunIsSkipped() throws Exception {
    K8sBlueGreenDeployTaskParameters blueGreenDeployTaskParams =
        K8sBlueGreenDeployTaskParameters.builder().skipDryRun(true).build();
    K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().build();
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback();

    when(containerDeploymentDelegateHelper.getKubernetesConfig(any(K8sClusterConfig.class)))
        .thenReturn(KubernetesConfig.builder().build());
    doNothing().when(k8sTaskHelper).deleteSkippedManifestFiles(any(), any());
    when(kubernetesContainerService.fetchReleaseHistory(any(), any(), any())).thenReturn(null);
    when(k8sTaskHelper.renderTemplate(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(Collections.emptyList());
    doNothing().when(k8sTaskHelper).setNamespaceToKubernetesResourcesIfRequired(any(), any());
    when(k8sTaskHelper.readManifests(any(), any())).thenReturn(Collections.emptyList());

    k8sBlueGreenDeployTaskHandler.init(blueGreenDeployTaskParams, delegateTaskParams, executionLogCallback);
    verify(k8sTaskHelper, times(0)).dryRunManifests(any(), any(), any(), any());
    verify(k8sTaskHelper, times(1)).readManifests(any(), any());
    verify(k8sTaskHelper, times(1)).renderTemplate(any(), any(), any(), any(), any(), any(), any());
    verify(k8sTaskHelper, times(1)).setNamespaceToKubernetesResourcesIfRequired(any(), any());
    verify(k8sTaskHelper, times(1)).deleteSkippedManifestFiles(any(), any());
    verify(kubernetesContainerService, times(1)).fetchReleaseHistory(any(), any(), any());
    verify(containerDeploymentDelegateHelper, times(1)).getKubernetesConfig(any(K8sClusterConfig.class));
  }

  @Test
  @Category(UnitTests.class)
  public void testDryRunIsNotSkipped() throws Exception {
    K8sBlueGreenDeployTaskParameters blueGreenDeployTaskParams =
        K8sBlueGreenDeployTaskParameters.builder().skipDryRun(false).build();
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback();
    K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().build();

    when(containerDeploymentDelegateHelper.getKubernetesConfig(any(K8sClusterConfig.class)))
        .thenReturn(KubernetesConfig.builder().build());
    doNothing().when(k8sTaskHelper).deleteSkippedManifestFiles(any(), any());
    when(kubernetesContainerService.fetchReleaseHistory(any(), any(), any())).thenReturn(null);
    doNothing().when(k8sTaskHelper).setNamespaceToKubernetesResourcesIfRequired(any(), any());
    when(k8sTaskHelper.renderTemplate(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(Collections.emptyList());
    when(k8sTaskHelper.readManifests(any(), any())).thenReturn(Collections.emptyList());

    k8sBlueGreenDeployTaskHandler.init(blueGreenDeployTaskParams, delegateTaskParams, executionLogCallback);
    verify(k8sTaskHelper, times(1)).dryRunManifests(any(), any(), any(), any());
    verify(k8sTaskHelper, times(1)).readManifests(any(), any());
    verify(k8sTaskHelper, times(1)).renderTemplate(any(), any(), any(), any(), any(), any(), any());
    verify(k8sTaskHelper, times(1)).setNamespaceToKubernetesResourcesIfRequired(any(), any());
    verify(k8sTaskHelper, times(1)).deleteSkippedManifestFiles(any(), any());
    verify(kubernetesContainerService, times(1)).fetchReleaseHistory(any(), any(), any());
    verify(containerDeploymentDelegateHelper, times(1)).getKubernetesConfig(any(K8sClusterConfig.class));
  }
}
