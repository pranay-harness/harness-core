package software.wings.delegatetasks.k8s.taskhandler;

import static io.harness.rule.OwnerRule.ANSHUL;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.delegatetasks.k8s.K8sTestConstants.DEPLOYMENT_YAML;
import static software.wings.delegatetasks.k8s.K8sTestConstants.SERVICE_YAML;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.harness.category.element.UnitTests;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.manifest.ManifestHelper;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.Release;
import io.harness.k8s.model.Release.KubernetesResourceIdRevision;
import io.harness.k8s.model.ReleaseHistory;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.delegatetasks.k8s.K8sDelegateTaskParams;
import software.wings.delegatetasks.k8s.K8sTaskHelper;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sBlueGreenDeployTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class K8sBlueGreenDeployTaskHandlerTest extends WingsBaseTest {
  @Mock private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Mock private KubernetesContainerService kubernetesContainerService;
  @Mock private K8sTaskHelper k8sTaskHelper;
  @Mock private ExecutionLogCallback executionLogCallback;
  @Mock private ReleaseHistory releaseHistory;
  @Mock private Kubectl client;

  @InjectMocks private K8sBlueGreenDeployTaskHandler k8sBlueGreenDeployTaskHandler;

  @Test
  @Owner(developers = ANSHUL)
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
    when(k8sTaskHelper.renderTemplate(any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(Collections.emptyList());
    doNothing().when(k8sTaskHelper).setNamespaceToKubernetesResourcesIfRequired(any(), any());
    when(k8sTaskHelper.readManifests(any(), any())).thenReturn(Collections.emptyList());

    k8sBlueGreenDeployTaskHandler.init(blueGreenDeployTaskParams, delegateTaskParams, executionLogCallback);
    verify(k8sTaskHelper, times(0)).dryRunManifests(any(), any(), any(), any());
    verify(k8sTaskHelper, times(1)).readManifests(any(), any());
    verify(k8sTaskHelper, times(1)).renderTemplate(any(), any(), any(), any(), any(), any(), any(), any());
    verify(k8sTaskHelper, times(1)).setNamespaceToKubernetesResourcesIfRequired(any(), any());
    verify(k8sTaskHelper, times(1)).deleteSkippedManifestFiles(any(), any());
    verify(kubernetesContainerService, times(1)).fetchReleaseHistory(any(), any(), any());
    verify(containerDeploymentDelegateHelper, times(1)).getKubernetesConfig(any(K8sClusterConfig.class));
  }

  @Test
  @Owner(developers = ANSHUL)
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
    when(k8sTaskHelper.renderTemplate(any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(Collections.emptyList());
    when(k8sTaskHelper.readManifests(any(), any())).thenReturn(Collections.emptyList());

    k8sBlueGreenDeployTaskHandler.init(blueGreenDeployTaskParams, delegateTaskParams, executionLogCallback);
    verify(k8sTaskHelper, times(1)).dryRunManifests(any(), any(), any(), any());
    verify(k8sTaskHelper, times(1)).readManifests(any(), any());
    verify(k8sTaskHelper, times(1)).renderTemplate(any(), any(), any(), any(), any(), any(), any(), any());
    verify(k8sTaskHelper, times(1)).setNamespaceToKubernetesResourcesIfRequired(any(), any());
    verify(k8sTaskHelper, times(1)).deleteSkippedManifestFiles(any(), any());
    verify(kubernetesContainerService, times(1)).fetchReleaseHistory(any(), any(), any());
    verify(containerDeploymentDelegateHelper, times(1)).getKubernetesConfig(any(K8sClusterConfig.class));
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testMissingLabelInService() {
    K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().build();

    List<KubernetesResource> kubernetesResources = new ArrayList<>();
    kubernetesResources.addAll(ManifestHelper.processYaml(DEPLOYMENT_YAML));
    kubernetesResources.addAll(ManifestHelper.processYaml(SERVICE_YAML));

    Service service = new ServiceBuilder()
                          .withApiVersion("v1")
                          .withNewMetadata()
                          .withName("servicename")
                          .endMetadata()
                          .withNewSpec()
                          .withType("LoadBalancer")
                          .addNewPort()
                          .withPort(80)
                          .endPort()
                          .withClusterIP("1.2.3.4")
                          .endSpec()
                          .withNewStatus()
                          .endStatus()
                          .build();

    on(k8sBlueGreenDeployTaskHandler).set("resources", kubernetesResources);
    on(k8sBlueGreenDeployTaskHandler).set("releaseHistory", releaseHistory);

    when(kubernetesContainerService.getService(null, Collections.emptyList(), "servicename")).thenReturn(service);

    boolean result = k8sBlueGreenDeployTaskHandler.prepareForBlueGreen(
        K8sBlueGreenDeployTaskParameters.builder().build(), delegateTaskParams, executionLogCallback);
    assertThat(result).isFalse();

    verify(kubernetesContainerService, times(2)).getService(any(), any(), any());
    verify(releaseHistory, times(0)).createNewRelease(any());
    verify(executionLogCallback, times(1))
        .saveExecutionLog(
            "Found conflicting service [servicename] in the cluster. For blue/green deployment, the label [harness.io/color] is required in service selector. Delete this existing service to proceed",
            LogLevel.ERROR, CommandExecutionStatus.FAILURE);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testCleanupForBlueGreenForNPE() throws Exception {
    K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().build();

    on(k8sBlueGreenDeployTaskHandler).set("client", client);
    on(k8sBlueGreenDeployTaskHandler).set("primaryColor", "blue");
    on(k8sBlueGreenDeployTaskHandler).set("stageColor", "green");
    on(k8sBlueGreenDeployTaskHandler).set("currentRelease", Release.builder().number(1).build());

    KubernetesResource kubernetesResource = ManifestHelper.processYaml(DEPLOYMENT_YAML).get(0);
    Release release = Release.builder()
                          .resources(asList(kubernetesResource.getResourceId()))
                          .number(2)
                          .managedWorkloads(asList(KubernetesResourceIdRevision.builder()
                                                       .workload(kubernetesResource.getResourceId())
                                                       .revision("2")
                                                       .build()))
                          .build();
    ReleaseHistory releaseHistory = ReleaseHistory.createNew();
    releaseHistory.setReleases(new ArrayList<>(asList(release)));

    k8sBlueGreenDeployTaskHandler.cleanupForBlueGreen(delegateTaskParams, releaseHistory, executionLogCallback);

    kubernetesResource.getResourceId().setName("deployment-green");
    kubernetesResource.getResourceId().setVersioned(true);
    release.setManagedWorkload(kubernetesResource.getResourceId());
    release.setManagedWorkloadRevision("2");
    release.setManagedWorkloads(null);
    k8sBlueGreenDeployTaskHandler.cleanupForBlueGreen(delegateTaskParams, releaseHistory, executionLogCallback);
    verify(k8sTaskHelper, times(1))
        .delete(client, delegateTaskParams, asList(kubernetesResource.getResourceId()), executionLogCallback);
  }
}
