package software.wings.delegatetasks.k8s.taskhandler;

import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.BOJANA;
import static io.harness.rule.OwnerRule.YOGESH;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.delegatetasks.k8s.K8sTask.MANIFEST_FILES_DIR;
import static software.wings.delegatetasks.k8s.K8sTestConstants.DEPLOYMENT_YAML;

import io.harness.category.element.UnitTests;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.KubernetesYamlException;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.manifest.ManifestHelper;
import io.harness.k8s.model.K8sPod;
import io.harness.k8s.model.Kind;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.k8s.model.Release;
import io.harness.k8s.model.ReleaseHistory;
import io.harness.rule.Owner;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.delegatetasks.k8s.K8sDelegateTaskParams;
import software.wings.delegatetasks.k8s.K8sTaskHelper;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.helm.request.HelmChartConfigParams;
import software.wings.helpers.ext.helm.response.HelmChartInfo;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig;
import software.wings.helpers.ext.k8s.request.K8sRollingDeployTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sRollingDeployResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class K8sRollingDeployTaskHandlerTest extends WingsBaseTest {
  @Mock private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Mock private KubernetesContainerService kubernetesContainerService;
  @Mock private K8sTaskHelper k8sTaskHelper;
  @InjectMocks private K8sRollingDeployTaskHandler k8sRollingDeployTaskHandler;

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testDryRunIsSkipped() throws Exception {
    K8sRollingDeployTaskParameters rollingDeployTaskParams =
        K8sRollingDeployTaskParameters.builder().skipDryRun(true).build();
    K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().build();
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback();

    when(containerDeploymentDelegateHelper.getKubernetesConfig(any(K8sClusterConfig.class)))
        .thenReturn(KubernetesConfig.builder().build());
    when(kubernetesContainerService.fetchReleaseHistory(any(), any(), any())).thenReturn(null);
    doNothing().when(k8sTaskHelper).deleteSkippedManifestFiles(any(), any());
    when(k8sTaskHelper.renderTemplate(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(emptyList());
    doNothing().when(k8sTaskHelper).setNamespaceToKubernetesResourcesIfRequired(any(), any());
    when(k8sTaskHelper.readManifestAndOverrideLocalSecrets(any(), any(), anyBoolean())).thenReturn(emptyList());

    k8sRollingDeployTaskHandler.init(rollingDeployTaskParams, delegateTaskParams, executionLogCallback);
    verify(k8sTaskHelper, times(0)).dryRunManifests(any(), any(), any(), any());
    verify(k8sTaskHelper, times(0)).updateVirtualServiceManifestFilesWithRoutesForCanary(any(), any(), any());
    verify(k8sTaskHelper, times(0)).updateDestinationRuleManifestFilesWithSubsets(any(), any(), any(), any());
    verify(k8sTaskHelper, times(1)).readManifestAndOverrideLocalSecrets(any(), any(), anyBoolean());
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
    K8sRollingDeployTaskParameters rollingDeployTaskParams =
        K8sRollingDeployTaskParameters.builder().skipDryRun(false).build();
    K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().build();
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback();

    when(containerDeploymentDelegateHelper.getKubernetesConfig(any(K8sClusterConfig.class)))
        .thenReturn(KubernetesConfig.builder().build());
    when(kubernetesContainerService.fetchReleaseHistory(any(), any(), any())).thenReturn(null);
    doNothing().when(k8sTaskHelper).setNamespaceToKubernetesResourcesIfRequired(any(), any());
    doNothing().when(k8sTaskHelper).deleteSkippedManifestFiles(any(), any());
    when(k8sTaskHelper.renderTemplate(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(emptyList());
    when(k8sTaskHelper.readManifestAndOverrideLocalSecrets(any(), any(), anyBoolean())).thenReturn(emptyList());

    k8sRollingDeployTaskHandler.init(rollingDeployTaskParams, delegateTaskParams, executionLogCallback);
    verify(k8sTaskHelper, times(1)).dryRunManifests(any(), any(), any(), any());
    verify(k8sTaskHelper, times(0)).updateVirtualServiceManifestFilesWithRoutesForCanary(any(), any(), any());
    verify(k8sTaskHelper, times(0)).updateDestinationRuleManifestFilesWithSubsets(any(), any(), any(), any());
    verify(k8sTaskHelper, times(1)).setNamespaceToKubernetesResourcesIfRequired(any(), any());
    verify(k8sTaskHelper, times(1)).readManifestAndOverrideLocalSecrets(any(), any(), anyBoolean());
    verify(k8sTaskHelper, times(1)).renderTemplate(any(), any(), any(), any(), any(), any(), any(), any());
    verify(k8sTaskHelper, times(1)).deleteSkippedManifestFiles(any(), any());
    verify(kubernetesContainerService, times(1)).fetchReleaseHistory(any(), any(), any());
    verify(containerDeploymentDelegateHelper, times(1)).getKubernetesConfig(any(K8sClusterConfig.class));
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testUpdateDeploymentConfigRevision() throws Exception {
    K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().build();

    Release.KubernetesResourceIdRevision resourceIdMock = Mockito.mock(Release.KubernetesResourceIdRevision.class);
    Release release = Release.builder().managedWorkloads(asList(resourceIdMock)).build();

    on(k8sRollingDeployTaskHandler).set("release", release);
    when(k8sTaskHelper.getLatestRevision(any(), any(), any())).thenReturn("2");
    when(resourceIdMock.getWorkload())
        .thenReturn(KubernetesResourceId.builder().kind(Kind.DeploymentConfig.name()).build());

    k8sRollingDeployTaskHandler.updateDeploymentConfigRevision(delegateTaskParams);
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(resourceIdMock).setRevision(captor.capture());
    assertThat(captor.getValue()).isEqualTo("2");

    when(resourceIdMock.getWorkload()).thenReturn(KubernetesResourceId.builder().kind(Kind.Deployment.name()).build());
    verify(resourceIdMock, times(1)).setRevision(anyString());
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testTagNewPods() {
    assertThat(k8sRollingDeployTaskHandler.tagNewPods(emptyList(), emptyList())).isEmpty();

    List<K8sPod> pods = k8sRollingDeployTaskHandler.tagNewPods(
        asList(podWithName("pod-1"), podWithName("pod-2")), asList(podWithName("old-pod-1"), podWithName("old-pod-2")));
    assertThat(pods).hasSize(2);
    assertThat(pods.stream().filter(K8sPod::isNewPod).count()).isEqualTo(2);
    assertThat(pods.stream().map(K8sPod::getName).collect(Collectors.toList()))
        .containsExactlyInAnyOrder("pod-1", "pod-2");

    pods = k8sRollingDeployTaskHandler.tagNewPods(
        asList(podWithName("pod-1"), podWithName("pod-2")), asList(podWithName("pod-1")));
    assertThat(pods).hasSize(2);
    assertThat(pods.stream().filter(K8sPod::isNewPod).count()).isEqualTo(1);
    assertThat(pods.stream().map(K8sPod::getName).collect(Collectors.toList()))
        .containsExactlyInAnyOrder("pod-1", "pod-2");

    pods = k8sRollingDeployTaskHandler.tagNewPods(asList(podWithName("pod-1"), podWithName("pod-2")), emptyList());
    assertThat(pods).hasSize(2);
    assertThat(pods.stream().filter(K8sPod::isNewPod).count()).isEqualTo(2);
    assertThat(pods.stream().map(K8sPod::getName).collect(Collectors.toList()))
        .containsExactlyInAnyOrder("pod-1", "pod-2");
  }

  private K8sPod podWithName(String name) {
    return K8sPod.builder().name(name).build();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testAssignHelmChartInfo() throws Exception {
    K8sRollingDeployTaskHandler handler = spy(k8sRollingDeployTaskHandler);
    K8sDelegateManifestConfig manifestConfig = K8sDelegateManifestConfig.builder()
                                                   .manifestStoreTypes(StoreType.HelmChartRepo)
                                                   .helmChartConfigParams(HelmChartConfigParams.builder().build())
                                                   .build();
    K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().workingDirectory(".").build();
    K8sRollingDeployTaskParameters rollingDeployTaskParams =
        K8sRollingDeployTaskParameters.builder().k8sDelegateManifestConfig(manifestConfig).skipDryRun(true).build();
    HelmChartInfo helmChartInfo = HelmChartInfo.builder().name("chart").version("1.0.0").build();

    on(handler).set("resources", Lists.emptyList());
    ReleaseHistory releaseHist = ReleaseHistory.createNew();
    releaseHist.setReleases(new ArrayList<>());
    on(handler).set("releaseHistory", releaseHist);
    doReturn(true).when(handler).init(
        any(K8sRollingDeployTaskParameters.class), any(K8sDelegateTaskParams.class), any(ExecutionLogCallback.class));
    doReturn(mock(ExecutionLogCallback.class))
        .when(k8sTaskHelper)
        .getExecutionLogCallback(any(K8sTaskParameters.class), anyString());
    doReturn(true)
        .when(k8sTaskHelper)
        .fetchManifestFilesAndWriteToDirectory(
            any(K8sDelegateManifestConfig.class), anyString(), any(ExecutionLogCallback.class));
    doReturn(true)
        .when(k8sTaskHelper)
        .applyManifests(any(Kubectl.class), anyListOf(KubernetesResource.class), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class));
    doReturn(helmChartInfo)
        .when(k8sTaskHelper)
        .getHelmChartDetails(manifestConfig, Paths.get(".", MANIFEST_FILES_DIR).toString());

    K8sTaskExecutionResponse response = handler.executeTask(rollingDeployTaskParams, delegateTaskParams);
    K8sRollingDeployResponse rollingDeployResponse = (K8sRollingDeployResponse) response.getK8sTaskResponse();

    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    assertThat(rollingDeployResponse.getHelmChartInfo()).isEqualTo(helmChartInfo);
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void invalidTypeOfTaskParams() {
    assertThatExceptionOfType(InvalidArgumentsException.class)
        .isThrownBy(() -> k8sRollingDeployTaskHandler.executeTaskInternal(null, null))
        .withMessageContaining("INVALID_ARGUMENT");
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void getFailureResponse() throws Exception {
    k8sRollingDeployTaskHandler.executeTaskInternal(
        K8sRollingDeployTaskParameters.builder().build(), K8sDelegateTaskParams.builder().build());
    verify(k8sTaskHelper, times(1))
        .fetchManifestFilesAndWriteToDirectory(
            any(K8sDelegateManifestConfig.class), anyString(), any(ExecutionLogCallback.class));
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void prepareForRollingNotCanary() throws Exception {
    List<KubernetesResource> kubernetesResources = getResources();
    K8sRollingDeployTaskHandler handler = spy(k8sRollingDeployTaskHandler);
    doReturn(kubernetesResources)
        .when(k8sTaskHelper)
        .readManifestAndOverrideLocalSecrets(anyList(), any(ExecutionLogCallback.class), anyBoolean());
    doReturn(true)
        .when(k8sTaskHelper)
        .fetchManifestFilesAndWriteToDirectory(
            any(K8sDelegateManifestConfig.class), anyString(), any(ExecutionLogCallback.class));
    doReturn(mock(ExecutionLogCallback.class))
        .when(k8sTaskHelper)
        .getExecutionLogCallback(any(K8sTaskParameters.class), anyString());
    doReturn(KubernetesConfig.builder().build())
        .when(containerDeploymentDelegateHelper)
        .getKubernetesConfig(any(K8sClusterConfig.class));
    doReturn(true)
        .when(k8sTaskHelper)
        .dryRunManifests(
            any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class), any(ExecutionLogCallback.class));
    doReturn(true)
        .when(k8sTaskHelper)
        .applyManifests(
            any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class), any(ExecutionLogCallback.class));
    handler.executeTaskInternal(
        K8sRollingDeployTaskParameters.builder().releaseName("releaseName").isInCanaryWorkflow(false).build(),
        K8sDelegateTaskParams.builder().build());

    verify(k8sTaskHelper, times(1))
        .fetchManifestFilesAndWriteToDirectory(
            any(K8sDelegateManifestConfig.class), anyString(), any(ExecutionLogCallback.class));
    verify(kubernetesContainerService, times(1))
        .fetchReleaseHistory(any(KubernetesConfig.class), anyList(), anyString());
    verify(containerDeploymentDelegateHelper, times(1)).getKubernetesConfig(any(K8sClusterConfig.class));
    verify(k8sTaskHelper, times(1))
        .dryRunManifests(
            any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class), any(ExecutionLogCallback.class));
    verify(k8sTaskHelper, times(1))
        .applyManifests(
            any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class), any(ExecutionLogCallback.class));
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void prepareForRollingIsCanary() throws Exception {
    String releaseHistory = "---\n"
        + "version: v1\n"
        + "releases:\n"
        + "- status: Succeeded\n"
        + "  managedWorkloads: []\n";
    doReturn(releaseHistory)
        .when(kubernetesContainerService)
        .fetchReleaseHistory(any(KubernetesConfig.class), anyList(), anyString());
    doReturn(true)
        .when(k8sTaskHelper)
        .fetchManifestFilesAndWriteToDirectory(
            any(K8sDelegateManifestConfig.class), anyString(), any(ExecutionLogCallback.class));
    List<KubernetesResource> kubernetesResources = getResources();
    doReturn(kubernetesResources)
        .when(k8sTaskHelper)
        .readManifestAndOverrideLocalSecrets(anyList(), any(ExecutionLogCallback.class), anyBoolean());
    K8sRollingDeployTaskHandler handler = spy(k8sRollingDeployTaskHandler);
    doReturn(mock(ExecutionLogCallback.class))
        .when(k8sTaskHelper)
        .getExecutionLogCallback(any(K8sTaskParameters.class), anyString());
    doReturn(KubernetesConfig.builder().build())
        .when(containerDeploymentDelegateHelper)
        .getKubernetesConfig(any(K8sClusterConfig.class));
    doReturn(true)
        .when(k8sTaskHelper)
        .dryRunManifests(
            any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class), any(ExecutionLogCallback.class));
    doReturn(true)
        .when(k8sTaskHelper)
        .applyManifests(
            any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class), any(ExecutionLogCallback.class));
    handler.executeTaskInternal(
        K8sRollingDeployTaskParameters.builder().releaseName("releaseName").isInCanaryWorkflow(true).build(),
        K8sDelegateTaskParams.builder().build());

    verify(k8sTaskHelper, times(1))
        .fetchManifestFilesAndWriteToDirectory(
            any(K8sDelegateManifestConfig.class), anyString(), any(ExecutionLogCallback.class));
    verify(kubernetesContainerService, times(1))
        .fetchReleaseHistory(any(KubernetesConfig.class), anyList(), anyString());
    verify(containerDeploymentDelegateHelper, times(1)).getKubernetesConfig(any(K8sClusterConfig.class));
    verify(k8sTaskHelper, times(1))
        .dryRunManifests(
            any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class), any(ExecutionLogCallback.class));
    verify(k8sTaskHelper, times(1))
        .applyManifests(
            any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class), any(ExecutionLogCallback.class));
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testInitException() throws Exception {
    doThrow(new KubernetesYamlException("reason"))
        .when(k8sTaskHelper)
        .deleteSkippedManifestFiles(anyString(), any(ExecutionLogCallback.class));
    final boolean success = k8sRollingDeployTaskHandler.init(K8sRollingDeployTaskParameters.builder().build(),
        K8sDelegateTaskParams.builder().build(), Mockito.mock(ExecutionLogCallback.class));
    assertThat(success).isFalse();
  }

  private List<KubernetesResource> getResources() {
    List<KubernetesResource> kubernetesResources = new ArrayList<>();
    kubernetesResources.addAll(ManifestHelper.processYaml(DEPLOYMENT_YAML));
    return kubernetesResources;
  }
}
