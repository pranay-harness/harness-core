package io.harness.delegate.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.delegate.k8s.K8sTestHelper.buildProcessResult;
import static io.harness.delegate.k8s.K8sTestHelper.buildRelease;
import static io.harness.delegate.k8s.K8sTestHelper.buildReleaseMultipleManagedWorkloads;
import static io.harness.delegate.k8s.K8sTestHelper.crdNew;
import static io.harness.delegate.k8s.K8sTestHelper.crdOld;
import static io.harness.delegate.k8s.K8sTestHelper.deployment;
import static io.harness.delegate.k8s.K8sTestHelper.deploymentConfig;
import static io.harness.k8s.model.Release.Status.Failed;
import static io.harness.k8s.model.Release.Status.Succeeded;
import static io.harness.logging.LogLevel.INFO;
import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.ACASIAN;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.BOJANA;
import static io.harness.rule.OwnerRule.YOGESH;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.k8s.beans.K8sRollingRollbackHandlerConfig;
import io.harness.delegate.task.k8s.K8sTaskHelperBase;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.kubectl.RolloutUndoCommand;
import io.harness.k8s.model.K8sDelegateTaskParams;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.k8s.model.Release;
import io.harness.k8s.model.Release.KubernetesResourceIdRevision;
import io.harness.k8s.model.ReleaseHistory;
import io.harness.logging.LogCallback;
import io.harness.rule.Owner;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.zeroturnaround.exec.ProcessOutput;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.LogOutputStream;

@OwnedBy(CDP)
public class K8sRollingRollbackBaseHandlerTest extends CategoryTest {
  @Mock private LogCallback logCallback;
  @Mock private ReleaseHistory releaseHistory;
  @Mock private K8sTaskHelperBase k8sTaskHelperBase;

  @InjectMocks @Spy private K8sRollingRollbackBaseHandler k8sRollingRollbackBaseHandler;

  private KubernetesConfig kubernetesConfig = KubernetesConfig.builder().build();
  private K8sDelegateTaskParams k8sDelegateTaskParams =
      K8sDelegateTaskParams.builder().kubectlPath("kubectl").ocPath("oc").kubeconfigPath("config-path").build();

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testRollbackForDC() throws Exception {
    rollbackUtil(deploymentConfig(),
        "oc --kubeconfig=config-path rollout undo DeploymentConfig/test-dc --namespace=default --to-revision=1");
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testRollbackForDeployment() throws Exception {
    rollbackUtil(
        deployment(), "kubectl --kubeconfig=config-path rollout undo Deployment/nginx-deployment --to-revision=1");
  }

  private void rollbackUtil(KubernetesResource kubernetesResource, String expectedOutput) throws Exception {
    int releaseNumber = 2;
    K8sDelegateTaskParams k8sDelegateTaskParams =
        K8sDelegateTaskParams.builder().kubectlPath("kubectl").ocPath("oc").kubeconfigPath("config-path").build();
    K8sRollingRollbackHandlerConfig rollbackHandlerConfig = new K8sRollingRollbackHandlerConfig();
    Release release = Release.builder()
                          .resources(asList(kubernetesResource.getResourceId()))
                          .number(2)
                          .managedWorkloads(asList(KubernetesResourceIdRevision.builder()
                                                       .workload(kubernetesResource.getResourceId())
                                                       .revision("2")
                                                       .build()))
                          .build();
    Release previousEligibleRelease = Release.builder()
                                          .resources(asList(kubernetesResource.getResourceId()))
                                          .number(1)
                                          .managedWorkloads(asList(KubernetesResourceIdRevision.builder()
                                                                       .workload(kubernetesResource.getResourceId())
                                                                       .revision("1")
                                                                       .build()))
                                          .build();
    rollbackHandlerConfig.setReleaseHistory(releaseHistory);
    rollbackHandlerConfig.setRelease(release);
    rollbackHandlerConfig.setClient(Kubectl.client("kubectl", "config-path"));
    ProcessResult processResult = new ProcessResult(0, new ProcessOutput("".getBytes()));

    doReturn(processResult)
        .when(k8sRollingRollbackBaseHandler)
        .executeScript(
            eq(k8sDelegateTaskParams), eq(expectedOutput), any(LogOutputStream.class), any(LogOutputStream.class));
    doReturn(processResult)
        .when(k8sRollingRollbackBaseHandler)
        .runK8sExecutable(eq(k8sDelegateTaskParams), eq(logCallback), any(RolloutUndoCommand.class));
    doReturn(previousEligibleRelease).when(releaseHistory).getPreviousRollbackEligibleRelease(releaseNumber);

    boolean rollback = k8sRollingRollbackBaseHandler.rollback(
        rollbackHandlerConfig, k8sDelegateTaskParams, releaseNumber, logCallback);
    assertThat(rollback).isTrue();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testFirstDeploymentFailsRollBack() throws Exception {
    String releaseName = "releaseName";
    K8sDelegateTaskParams k8sDelegateTaskParams = K8sDelegateTaskParams.builder().build();
    K8sRollingRollbackHandlerConfig rollbackHandlerConfig = new K8sRollingRollbackHandlerConfig();
    rollbackHandlerConfig.setReleaseHistory(null);
    rollbackHandlerConfig.setKubernetesConfig(kubernetesConfig);
    doReturn("").when(k8sTaskHelperBase).getReleaseHistoryData(kubernetesConfig, releaseName);

    boolean result = k8sRollingRollbackBaseHandler.init(rollbackHandlerConfig, releaseName, logCallback);
    assertThat(rollbackHandlerConfig.isNoopRollBack()).isTrue();
    assertThat(result).isTrue();

    result = k8sRollingRollbackBaseHandler.rollback(rollbackHandlerConfig, k8sDelegateTaskParams, null, logCallback);
    assertThat(result).isTrue();
    verify(logCallback).saveExecutionLog("No previous release found. Skipping rollback.");

    k8sRollingRollbackBaseHandler.steadyStateCheck(rollbackHandlerConfig, k8sDelegateTaskParams, null, logCallback);
    verify(logCallback)
        .saveExecutionLog("Skipping Status Check since there is no previous eligible Managed Workload.", INFO);
    verify(k8sTaskHelperBase, never()).doStatusCheck(any(), any(), any(), any());

    k8sRollingRollbackBaseHandler.postProcess(rollbackHandlerConfig, releaseName);
    verify(k8sTaskHelperBase, never()).saveReleaseHistory(any(), any(), any(), eq(false));
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testRollbackCustomWorkloadAndDoStatusCheck() throws Exception {
    KubernetesResource previousCustomResource = crdOld();
    KubernetesResource currentCustomResource = crdNew();
    K8sRollingRollbackHandlerConfig rollbackHandlerConfig =
        prepareRollbackCustomWorkloads(previousCustomResource, currentCustomResource);

    when(releaseHistory.getLatestRelease()).thenReturn(rollbackHandlerConfig.getRelease());
    when(k8sTaskHelperBase.doStatusCheckForAllCustomResources(any(Kubectl.class), anyList(),
             any(K8sDelegateTaskParams.class), any(LogCallback.class), anyBoolean(), anyLong()))
        .thenReturn(true);

    boolean result = k8sRollingRollbackBaseHandler.init(rollbackHandlerConfig, "releaseName", logCallback);
    assertThat(result).isTrue();

    result = k8sRollingRollbackBaseHandler.rollback(rollbackHandlerConfig, k8sDelegateTaskParams, 2, logCallback);
    assertThat(result).isTrue();

    k8sRollingRollbackBaseHandler.steadyStateCheck(rollbackHandlerConfig, k8sDelegateTaskParams, 10, logCallback);
    k8sRollingRollbackBaseHandler.postProcess(rollbackHandlerConfig, "releaseName");

    ArgumentCaptor<List> statusCheckCustomWorkloadsCaptor = ArgumentCaptor.forClass(List.class);
    verify(k8sTaskHelperBase, times(1))
        .doStatusCheckForAllCustomResources(any(Kubectl.class), statusCheckCustomWorkloadsCaptor.capture(),
            any(K8sDelegateTaskParams.class), any(LogCallback.class), anyBoolean(), anyLong());
    List<KubernetesResource> customWorkloadsUnderCheck = statusCheckCustomWorkloadsCaptor.getValue();
    assertThat(customWorkloadsUnderCheck).isNotEmpty();
    assertThat(customWorkloadsUnderCheck.get(0)).isEqualTo(previousCustomResource);

    Release releaseToSave = rollbackHandlerConfig.getRelease();
    assertThat(releaseToSave).isNotNull();
    assertThat(releaseToSave.getStatus()).isEqualTo(Failed);
    verify(k8sTaskHelperBase, never()).saveReleaseHistory(any(), any(), any(), eq(false));
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testRollbackCustomWorkload() throws Exception {
    KubernetesResource previousCustomResource = crdOld();
    KubernetesResource currentCustomResource = crdNew();
    K8sRollingRollbackHandlerConfig rollbackHandlerConfig =
        prepareRollbackCustomWorkloads(previousCustomResource, currentCustomResource);

    boolean rollback =
        k8sRollingRollbackBaseHandler.rollback(rollbackHandlerConfig, k8sDelegateTaskParams, 2, logCallback);
    assertThat(rollback).isTrue();

    ArgumentCaptor<List> previousCustomWorkloadsCaptor = ArgumentCaptor.forClass(List.class);
    ArgumentCaptor<List> currentCustomWorkloadsCaptor = ArgumentCaptor.forClass(List.class);

    verify(k8sTaskHelperBase, times(1))
        .delete(any(Kubectl.class), any(K8sDelegateTaskParams.class), currentCustomWorkloadsCaptor.capture(),
            any(LogCallback.class), anyBoolean());

    verify(k8sTaskHelperBase, times(1))
        .applyManifests(any(Kubectl.class), previousCustomWorkloadsCaptor.capture(), any(K8sDelegateTaskParams.class),
            any(LogCallback.class), anyBoolean());

    List<KubernetesResource> previousCustomWorkloads = previousCustomWorkloadsCaptor.getValue();
    assertThat(previousCustomWorkloads).isNotEmpty();
    assertThat(previousCustomWorkloads.get(0)).isEqualTo(previousCustomResource);

    List<KubernetesResourceId> currentCustomWorkloads = currentCustomWorkloadsCaptor.getValue();
    assertThat(currentCustomWorkloads).isNotEmpty();
    assertThat(currentCustomWorkloads.get(0)).isEqualTo(currentCustomResource.getResourceId());
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testRollbackCustomWorkloadWithoutDeletingCurrent() throws Exception {
    KubernetesResource previousCustomResource = crdOld();
    KubernetesResource currentCustomResource = deployment();
    K8sRollingRollbackHandlerConfig rollbackHandlerConfig =
        prepareRollbackCustomWorkloads(previousCustomResource, currentCustomResource);

    boolean rollback =
        k8sRollingRollbackBaseHandler.rollback(rollbackHandlerConfig, k8sDelegateTaskParams, 2, logCallback);
    assertThat(rollback).isTrue();

    ArgumentCaptor<List> previousCustomWorkloadsCaptor = ArgumentCaptor.forClass(List.class);

    verify(k8sTaskHelperBase, times(0))
        .delete(any(Kubectl.class), any(K8sDelegateTaskParams.class), anyList(), any(LogCallback.class), anyBoolean());

    verify(k8sTaskHelperBase, times(1))
        .applyManifests(any(Kubectl.class), previousCustomWorkloadsCaptor.capture(), any(K8sDelegateTaskParams.class),
            any(LogCallback.class), anyBoolean());

    List<KubernetesResource> previousCustomWorkloads = previousCustomWorkloadsCaptor.getValue();
    assertThat(previousCustomWorkloads).isNotEmpty();
    assertThat(previousCustomWorkloads.get(0)).isEqualTo(previousCustomResource);
  }

  private K8sRollingRollbackHandlerConfig prepareRollbackCustomWorkloads(
      KubernetesResource previousCustomResource, KubernetesResource currentCustomResource) throws Exception {
    K8sRollingRollbackHandlerConfig rollbackHandlerConfig = new K8sRollingRollbackHandlerConfig();
    rollbackHandlerConfig.setKubernetesConfig(kubernetesConfig);
    Release release = Release.builder()
                          .resources(asList(currentCustomResource.getResourceId()))
                          .number(2)
                          .managedWorkloads(getManagedWorkloads(currentCustomResource))
                          .customWorkloads(getCustomWorkloads(currentCustomResource))
                          .build();
    Release previousEligibleRelease = Release.builder()
                                          .resources(asList(previousCustomResource.getResourceId()))
                                          .number(1)
                                          .customWorkloads(asList(previousCustomResource))
                                          .build();

    when(releaseHistory.getPreviousRollbackEligibleRelease(anyInt())).thenReturn(previousEligibleRelease);
    rollbackHandlerConfig.setReleaseHistory(releaseHistory);
    rollbackHandlerConfig.setRelease(release);
    rollbackHandlerConfig.setClient(Kubectl.client("kubectl", "config-path"));
    when(k8sTaskHelperBase.applyManifests(
             any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class), any(LogCallback.class), anyBoolean()))
        .thenReturn(true);

    return rollbackHandlerConfig;
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void rollback() throws Exception {
    testRollBackReleaseIsNull();
    testRollBackIfNoManagedWorkload();
    testRollBackToSpecificRelease();
  }

  private void testRollBackReleaseIsNull() throws Exception {
    final boolean success = k8sRollingRollbackBaseHandler.rollback(
        new K8sRollingRollbackHandlerConfig(), K8sDelegateTaskParams.builder().build(), null, logCallback);

    assertThat(success).isTrue();
  }

  private void testRollBackIfNoManagedWorkload() throws Exception {
    K8sRollingRollbackHandlerConfig rollbackHandlerConfig = new K8sRollingRollbackHandlerConfig();
    rollbackHandlerConfig.setRelease(new Release());
    final boolean success = k8sRollingRollbackBaseHandler.rollback(
        rollbackHandlerConfig, K8sDelegateTaskParams.builder().build(), 2, logCallback);
    assertThat(success).isTrue();
  }

  private void testRollBackToSpecificRelease() throws Exception {
    rollback1ManagedWorkload();
    rollbackMultipleWorkloads();
  }

  private void rollback1ManagedWorkload() throws Exception {
    reset(k8sRollingRollbackBaseHandler);
    K8sRollingRollbackHandlerConfig rollbackHandlerConfig = new K8sRollingRollbackHandlerConfig();
    rollbackHandlerConfig.setClient(Kubectl.client("kubectl", "config-path"));
    doReturn(buildProcessResult(0)).when(k8sRollingRollbackBaseHandler).runK8sExecutable(any(), any(), any());

    ReleaseHistory releaseHistory = ReleaseHistory.createNew();
    releaseHistory.getReleases().add(buildRelease(Failed, 2));
    releaseHistory.getReleases().add(buildRelease(Succeeded, 1));
    releaseHistory.getReleases().add(buildRelease(Succeeded, 0));
    rollbackHandlerConfig.setRelease(releaseHistory.getLatestRelease());
    rollbackHandlerConfig.setReleaseHistory(releaseHistory);

    final boolean success = k8sRollingRollbackBaseHandler.rollback(
        rollbackHandlerConfig, K8sDelegateTaskParams.builder().build(), 2, logCallback);

    ArgumentCaptor<RolloutUndoCommand> captor = ArgumentCaptor.forClass(RolloutUndoCommand.class);
    verify(k8sRollingRollbackBaseHandler, times(1)).runK8sExecutable(any(), any(), captor.capture());

    RolloutUndoCommand rolloutUndoCommand = captor.getValue();
    assertThat(rolloutUndoCommand.command())
        .isEqualTo("kubectl --kubeconfig=config-path rollout undo Deployment/nginx-deployment");
    assertThat(success).isTrue();
  }

  private void rollbackMultipleWorkloads() throws Exception {
    reset(k8sRollingRollbackBaseHandler);
    K8sRollingRollbackHandlerConfig rollbackHandlerConfig = new K8sRollingRollbackHandlerConfig();
    rollbackHandlerConfig.setClient(Kubectl.client("kubectl", "config-path"));
    doReturn(buildProcessResult(0)).when(k8sRollingRollbackBaseHandler).runK8sExecutable(any(), any(), any());
    doReturn(buildProcessResult(0)).when(k8sRollingRollbackBaseHandler).executeScript(any(), anyString(), any(), any());

    ReleaseHistory releaseHistory = ReleaseHistory.createNew();
    releaseHistory.getReleases().add(buildReleaseMultipleManagedWorkloads(Failed));
    releaseHistory.getReleases().add(buildReleaseMultipleManagedWorkloads(Succeeded));
    releaseHistory.getReleases().add(buildReleaseMultipleManagedWorkloads(Succeeded));
    rollbackHandlerConfig.setRelease(releaseHistory.getLatestRelease());
    rollbackHandlerConfig.setReleaseHistory(releaseHistory);

    final boolean success = k8sRollingRollbackBaseHandler.rollback(
        rollbackHandlerConfig, K8sDelegateTaskParams.builder().kubeconfigPath("kubeconfig").build(), 2, logCallback);

    ArgumentCaptor<RolloutUndoCommand> captor = ArgumentCaptor.forClass(RolloutUndoCommand.class);
    ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(k8sRollingRollbackBaseHandler, times(1)).runK8sExecutable(any(), any(), captor.capture());
    verify(k8sRollingRollbackBaseHandler, times(1)).executeScript(any(), stringArgumentCaptor.capture(), any(), any());

    RolloutUndoCommand rolloutUndoCommand = captor.getValue();
    assertThat(rolloutUndoCommand.command())
        .isEqualTo("kubectl --kubeconfig=config-path rollout undo Deployment/nginx-deployment --to-revision=2");
    assertThat(success).isTrue();

    String command = stringArgumentCaptor.getValue();
    assertThat(command).isEqualTo(
        "oc --kubeconfig=kubeconfig rollout undo DeploymentConfig/test-dc --namespace=default --to-revision=2");
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testSkipRollback() throws Exception {
    K8sDelegateTaskParams k8sDelegateTaskParams = K8sDelegateTaskParams.builder().build();
    K8sRollingRollbackHandlerConfig rollbackHandlerConfig = new K8sRollingRollbackHandlerConfig();
    rollbackHandlerConfig.setReleaseHistory(releaseHistory);
    KubernetesResource currentCustomResource = crdNew();
    Release release = Release.builder()
                          .resources(asList(currentCustomResource.getResourceId()))
                          .customWorkloads(asList(currentCustomResource))
                          .status(Succeeded)
                          .build();
    rollbackHandlerConfig.setRelease(release);

    boolean rollback =
        k8sRollingRollbackBaseHandler.rollback(rollbackHandlerConfig, k8sDelegateTaskParams, 0, logCallback);
    assertThat(rollback).isTrue();
    ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
    verify(logCallback, times(1)).saveExecutionLog(logCaptor.capture());
    assertThat(logCaptor.getValue()).isEqualTo("No failed release found. Skipping rollback.");
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testSkipRollbackNoPreviousEligibleRelease() throws Exception {
    K8sDelegateTaskParams k8sDelegateTaskParams = K8sDelegateTaskParams.builder().build();
    K8sRollingRollbackHandlerConfig rollbackHandlerConfig = new K8sRollingRollbackHandlerConfig();
    KubernetesResource currentCustomResource = crdNew();
    Release release = Release.builder()
                          .resources(asList(currentCustomResource.getResourceId()))
                          .customWorkloads(asList(currentCustomResource))
                          .status(Succeeded)
                          .build();
    rollbackHandlerConfig.setRelease(release);
    rollbackHandlerConfig.setReleaseHistory(releaseHistory);
    when(releaseHistory.getPreviousRollbackEligibleRelease(anyInt())).thenReturn(null);

    boolean rollback =
        k8sRollingRollbackBaseHandler.rollback(rollbackHandlerConfig, k8sDelegateTaskParams, 1, logCallback);
    assertThat(rollback).isTrue();
    ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
    verify(logCallback).saveExecutionLog(logCaptor.capture());
    assertThat(logCaptor.getValue()).isEqualTo("No previous eligible release found. Can't rollback.");
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testRollbackNoManagedWorkloadInPreviousEligibleRelease() throws Exception {
    K8sDelegateTaskParams k8sDelegateTaskParams = K8sDelegateTaskParams.builder().build();
    K8sRollingRollbackHandlerConfig rollbackHandlerConfig = new K8sRollingRollbackHandlerConfig();
    KubernetesResource currentCustomResource = crdNew();
    Release release = Release.builder()
                          .resources(asList(currentCustomResource.getResourceId()))
                          .customWorkloads(asList(currentCustomResource))
                          .status(Succeeded)
                          .build();
    rollbackHandlerConfig.setRelease(release);
    rollbackHandlerConfig.setReleaseHistory(releaseHistory);
    when(releaseHistory.getPreviousRollbackEligibleRelease(anyInt())).thenReturn(null);

    KubernetesResource previousCustomResource = crdOld();
    Release previousEligibleRelease =
        Release.builder().resources(asList(previousCustomResource.getResourceId())).number(1).status(Succeeded).build();
    when(releaseHistory.getPreviousRollbackEligibleRelease(anyInt())).thenReturn(previousEligibleRelease);

    boolean rollback =
        k8sRollingRollbackBaseHandler.rollback(rollbackHandlerConfig, k8sDelegateTaskParams, 1, logCallback);
    assertThat(rollback).isTrue();
    ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
    verify(logCallback, times(2)).saveExecutionLog(logCaptor.capture());
    List<String> allLogs = logCaptor.getAllValues();
    assertThat(allLogs).containsExactly("Previous eligible Release is 1 with status Succeeded",
        "No Managed Workload found in previous eligible release. Skipping rollback.");
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testRollbackFailed() throws Exception {
    K8sRollingRollbackHandlerConfig rollbackHandlerConfig = new K8sRollingRollbackHandlerConfig();
    rollbackHandlerConfig.setClient(Kubectl.client("kubectl", "config-path"));
    doReturn(buildProcessResult(1)).when(k8sRollingRollbackBaseHandler).runK8sExecutable(any(), any(), any());

    ReleaseHistory releaseHistory = ReleaseHistory.createNew();
    releaseHistory.getReleases().add(buildRelease(Failed, 2));
    releaseHistory.getReleases().add(buildRelease(Succeeded, 1));
    releaseHistory.getReleases().add(buildRelease(Succeeded, 0));
    rollbackHandlerConfig.setRelease(releaseHistory.getLatestRelease());
    rollbackHandlerConfig.setReleaseHistory(releaseHistory);

    final boolean success = k8sRollingRollbackBaseHandler.rollback(
        rollbackHandlerConfig, K8sDelegateTaskParams.builder().build(), 2, logCallback);
    assertThat(success).isFalse();

    ArgumentCaptor<RolloutUndoCommand> captor = ArgumentCaptor.forClass(RolloutUndoCommand.class);
    verify(k8sRollingRollbackBaseHandler, times(1)).runK8sExecutable(any(), any(), captor.capture());
    RolloutUndoCommand rolloutUndoCommand = captor.getValue();
    assertThat(rolloutUndoCommand.command())
        .isEqualTo("kubectl --kubeconfig=config-path rollout undo Deployment/nginx-deployment");

    ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
    verify(logCallback, times(4)).saveExecutionLog(logCaptor.capture());
    List<String> logValues = logCaptor.getAllValues();
    assertThat(logValues).contains("Previous eligible Release is 1 with status Succeeded",
        "\nRolling back to release 1",
        "\nRolling back resource Deployment/nginx-deployment in namespace null to revision null");
  }

  private List<KubernetesResourceIdRevision> getManagedWorkloads(KubernetesResource kubernetesResource) {
    if (kubernetesResource.getResourceId().getKind().equals("Deployment")) {
      return asList(Release.KubernetesResourceIdRevision.builder()
                        .workload(kubernetesResource.getResourceId())
                        .revision("2")
                        .build());
    }

    return emptyList();
  }

  private List<KubernetesResource> getCustomWorkloads(KubernetesResource kubernetesResource) {
    if (kubernetesResource.isManagedWorkload() && !kubernetesResource.getResourceId().getKind().equals("Deployment")) {
      return asList(kubernetesResource);
    }

    return emptyList();
  }
}