package software.wings.delegatetasks.k8s.taskhandler;

import static io.harness.k8s.model.Release.Status.Failed;
import static io.harness.k8s.model.Release.Status.Succeeded;
import static io.harness.logging.LogLevel.INFO;
import static io.harness.rule.OwnerRule.ACASIAN;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.BOJANA;
import static io.harness.rule.OwnerRule.YOGESH;

import static software.wings.delegatetasks.k8s.K8sTestHelper.buildProcessResult;
import static software.wings.delegatetasks.k8s.K8sTestHelper.buildRelease;
import static software.wings.delegatetasks.k8s.K8sTestHelper.buildReleaseMultipleManagedWorkloads;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.delegate.task.k8s.K8sTaskHelperBase;
import io.harness.exception.InvalidArgumentsException;
import io.harness.k8s.KubernetesContainerService;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.kubectl.RolloutUndoCommand;
import io.harness.k8s.kubectl.Utils;
import io.harness.k8s.manifest.ManifestHelper;
import io.harness.k8s.model.K8sDelegateTaskParams;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.k8s.model.Release;
import io.harness.k8s.model.ReleaseHistory;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.k8s.K8sTaskHelper;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;
import software.wings.helpers.ext.k8s.request.K8sRollingDeployRollbackTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.zeroturnaround.exec.ProcessOutput;
import org.zeroturnaround.exec.ProcessResult;

@RunWith(PowerMockRunner.class)
@PrepareForTest({K8sTaskHelper.class, Utils.class})
@PowerMockIgnore({"javax.security.*", "javax.net.*"})
@TargetModule(Module._930_DELEGATE_TASKS)
public class K8sRollingDeployRollbackTaskHandlerTest extends WingsBaseTest {
  @Mock private ReleaseHistory releaseHistory;
  @Mock private K8sTaskHelper taskHelper;
  @Mock private K8sTaskHelperBase taskHelperBase;
  @Mock private ExecutionLogCallback logCallback;
  @Mock private KubernetesContainerService kubernetesContainerService;
  @Mock private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;

  @Before
  public void setUp() throws Exception {
    doReturn(logCallback).when(taskHelper).getExecutionLogCallback(any(), any());
  }

  @InjectMocks private K8sRollingDeployRollbackTaskHandler k8sRollingDeployRollbackTaskHandler;

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testRollbackForDC() throws Exception {
    rollbackUtil("/k8s/deployment-config.yaml",
        "oc --kubeconfig=config-path rollout undo DeploymentConfig/test-dc --namespace=default --to-revision=1");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testFirstDeploymentFailsRollBack() throws Exception {
    on(k8sRollingDeployRollbackTaskHandler).set("releaseHistory", null);
    doReturn("").when(taskHelperBase).getReleaseHistoryData(any(KubernetesConfig.class), anyString());
    k8sRollingDeployRollbackTaskHandler.executeTaskInternal(
        K8sRollingDeployRollbackTaskParameters.builder().build(), K8sDelegateTaskParams.builder().build());

    assertThat((String) on(k8sRollingDeployRollbackTaskHandler).get("release")).isNull();
    assertThat((String) on(k8sRollingDeployRollbackTaskHandler).get("releaseHistory")).isNull();
    verify(taskHelperBase, never()).doStatusCheck(any(), any(), any(), any());
    verify(taskHelperBase, never()).saveReleaseHistory(any(), any(), any(), eq(false));
    final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(logCallback, times(1)).saveExecutionLog(captor.capture(), eq(INFO), eq(CommandExecutionStatus.SUCCESS));
    assertThat(captor.getValue())
        .isEqualTo("Skipping Status Check since there is no previous eligible Managed Workload.");
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testRollbackForDeployment() throws Exception {
    rollbackUtil("/k8s/deployment.yaml",
        "kubectl --kubeconfig=config-path rollout undo Deployment/nginx-deployment --to-revision=1");
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testRollbackCustomWorkloadAndDoStatusCheck() throws Exception {
    K8sRollingDeployRollbackTaskParameters k8sRollingDeployRollbackTaskParameters =
        K8sRollingDeployRollbackTaskParameters.builder().build();
    K8sDelegateTaskParams k8sDelegateTaskParams =
        K8sDelegateTaskParams.builder().kubectlPath("kubectl").ocPath("oc").kubeconfigPath("config-path").build();
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback();

    KubernetesResource previousCustomResource = parseKubernetesResourceFromFile("/k8s/crd-old.yaml");
    KubernetesResource currentCustomResource = parseKubernetesResourceFromFile("/k8s/crd-new.yaml");

    Release release = Release.builder()
                          .resources(asList(currentCustomResource.getResourceId()))
                          .number(2)
                          .customWorkloads(asList(currentCustomResource))
                          .build();
    Release previousEligibleRelease = Release.builder()
                                          .resources(asList(previousCustomResource.getResourceId()))
                                          .number(1)
                                          .customWorkloads(asList(previousCustomResource))
                                          .build();

    when(releaseHistory.getPreviousRollbackEligibleRelease(anyInt())).thenReturn(previousEligibleRelease);
    when(releaseHistory.getLatestRelease()).thenReturn(release);
    when(containerDeploymentDelegateHelper.getKubernetesConfig(any(K8sClusterConfig.class), anyBoolean()))
        .thenReturn(KubernetesConfig.builder().build());
    on(k8sRollingDeployRollbackTaskHandler).set("releaseHistory", releaseHistory);
    on(k8sRollingDeployRollbackTaskHandler).set("release", release);
    on(k8sRollingDeployRollbackTaskHandler).set("client", Kubectl.client("kubectl", "config-path"));
    when(taskHelperBase.applyManifests(any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class),
             any(ExecutionLogCallback.class), anyBoolean()))
        .thenReturn(true);
    when(taskHelperBase.doStatusCheckForAllCustomResources(any(Kubectl.class), anyList(),
             any(K8sDelegateTaskParams.class), any(ExecutionLogCallback.class), anyBoolean(), anyLong()))
        .thenReturn(true);

    K8sTaskExecutionResponse response = k8sRollingDeployRollbackTaskHandler.executeTaskInternal(
        K8sRollingDeployRollbackTaskParameters.builder().build(), K8sDelegateTaskParams.builder().build());
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);

    ArgumentCaptor<List> statusCheckCustomWorkloadsCaptor = ArgumentCaptor.forClass(List.class);
    verify(taskHelperBase, times(1))
        .doStatusCheckForAllCustomResources(any(Kubectl.class), statusCheckCustomWorkloadsCaptor.capture(),
            any(K8sDelegateTaskParams.class), any(ExecutionLogCallback.class), anyBoolean(), anyLong());
    List<KubernetesResource> customWorkloadsUnderCheck = statusCheckCustomWorkloadsCaptor.getValue();
    assertThat(customWorkloadsUnderCheck).isNotEmpty();
    assertThat(customWorkloadsUnderCheck.get(0)).isEqualTo(previousCustomResource);

    Release releaseToSave = on(k8sRollingDeployRollbackTaskHandler).get("release");
    assertThat(releaseToSave).isNotNull();
    assertThat(releaseToSave.getStatus()).isEqualTo(Failed);
    verify(taskHelperBase, never()).saveReleaseHistory(any(), any(), any(), eq(false));
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testRollbackCustomWorkload() throws Exception {
    K8sRollingDeployRollbackTaskParameters k8sRollingDeployRollbackTaskParameters =
        K8sRollingDeployRollbackTaskParameters.builder().build();
    K8sDelegateTaskParams k8sDelegateTaskParams =
        K8sDelegateTaskParams.builder().kubectlPath("kubectl").ocPath("oc").kubeconfigPath("config-path").build();
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback();

    KubernetesResource previousCustomResource = parseKubernetesResourceFromFile("/k8s/crd-old.yaml");
    KubernetesResource currentCustomResource = parseKubernetesResourceFromFile("/k8s/crd-new.yaml");

    Release release = Release.builder()
                          .resources(asList(currentCustomResource.getResourceId()))
                          .number(2)
                          .customWorkloads(asList(currentCustomResource))
                          .build();
    Release previousEligibleRelease = Release.builder()
                                          .resources(asList(previousCustomResource.getResourceId()))
                                          .number(1)
                                          .customWorkloads(asList(previousCustomResource))
                                          .build();

    when(releaseHistory.getPreviousRollbackEligibleRelease(anyInt())).thenReturn(previousEligibleRelease);
    on(k8sRollingDeployRollbackTaskHandler).set("releaseHistory", releaseHistory);
    on(k8sRollingDeployRollbackTaskHandler).set("release", release);
    on(k8sRollingDeployRollbackTaskHandler).set("client", Kubectl.client("kubectl", "config-path"));
    when(taskHelperBase.applyManifests(any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class),
             any(ExecutionLogCallback.class), anyBoolean()))
        .thenReturn(true);

    boolean rollback = k8sRollingDeployRollbackTaskHandler.rollback(
        k8sRollingDeployRollbackTaskParameters, k8sDelegateTaskParams, executionLogCallback);
    assertThat(rollback).isTrue();

    ArgumentCaptor<List> previousCustomWorkloadsCaptor = ArgumentCaptor.forClass(List.class);
    ArgumentCaptor<List> currentCustomWorkloadsCaptor = ArgumentCaptor.forClass(List.class);

    verify(taskHelperBase, times(1))
        .delete(any(Kubectl.class), any(K8sDelegateTaskParams.class), currentCustomWorkloadsCaptor.capture(),
            any(ExecutionLogCallback.class), anyBoolean());

    verify(taskHelperBase, times(1))
        .applyManifests(any(Kubectl.class), previousCustomWorkloadsCaptor.capture(), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class), anyBoolean());

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
    K8sRollingDeployRollbackTaskParameters k8sRollingDeployRollbackTaskParameters =
        K8sRollingDeployRollbackTaskParameters.builder().build();
    K8sDelegateTaskParams k8sDelegateTaskParams =
        K8sDelegateTaskParams.builder().kubectlPath("kubectl").ocPath("oc").kubeconfigPath("config-path").build();
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback();

    KubernetesResource previousCustomResource = parseKubernetesResourceFromFile("/k8s/crd-old.yaml");
    KubernetesResource deployment = parseKubernetesResourceFromFile("/k8s/deployment.yaml");

    Release release = Release.builder()
                          .resources(asList(deployment.getResourceId()))
                          .number(2)
                          .managedWorkloads(asList(Release.KubernetesResourceIdRevision.builder()
                                                       .workload(deployment.getResourceId())
                                                       .revision("2")
                                                       .build()))
                          .build();
    Release previousEligibleRelease = Release.builder()
                                          .resources(asList(previousCustomResource.getResourceId()))
                                          .number(1)
                                          .customWorkloads(asList(previousCustomResource))
                                          .build();

    when(releaseHistory.getPreviousRollbackEligibleRelease(anyInt())).thenReturn(previousEligibleRelease);
    on(k8sRollingDeployRollbackTaskHandler).set("releaseHistory", releaseHistory);
    on(k8sRollingDeployRollbackTaskHandler).set("release", release);
    on(k8sRollingDeployRollbackTaskHandler).set("client", Kubectl.client("kubectl", "config-path"));
    when(taskHelperBase.applyManifests(any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class),
             any(ExecutionLogCallback.class), anyBoolean()))
        .thenReturn(true);

    boolean rollback = k8sRollingDeployRollbackTaskHandler.rollback(
        k8sRollingDeployRollbackTaskParameters, k8sDelegateTaskParams, executionLogCallback);
    assertThat(rollback).isTrue();

    ArgumentCaptor<List> previousCustomWorkloadsCaptor = ArgumentCaptor.forClass(List.class);

    verify(taskHelperBase, times(0))
        .delete(any(Kubectl.class), any(K8sDelegateTaskParams.class), anyList(), any(ExecutionLogCallback.class),
            anyBoolean());

    verify(taskHelperBase, times(1))
        .applyManifests(any(Kubectl.class), previousCustomWorkloadsCaptor.capture(), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class), anyBoolean());

    List<KubernetesResource> previousCustomWorkloads = previousCustomWorkloadsCaptor.getValue();
    assertThat(previousCustomWorkloads).isNotEmpty();
    assertThat(previousCustomWorkloads.get(0)).isEqualTo(previousCustomResource);
  }

  private KubernetesResource parseKubernetesResourceFromFile(String path) throws IOException {
    URL url = this.getClass().getResource(path);
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    return ManifestHelper.processYaml(fileContents).get(0);
  }

  private void rollbackUtil(String manifestFilePath, String expectedOutput) throws Exception {
    K8sRollingDeployRollbackTaskParameters k8sRollingDeployRollbackTaskParameters =
        K8sRollingDeployRollbackTaskParameters.builder().build();
    K8sDelegateTaskParams k8sDelegateTaskParams =
        K8sDelegateTaskParams.builder().kubectlPath("kubectl").ocPath("oc").kubeconfigPath("config-path").build();
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback();

    URL url = this.getClass().getResource(manifestFilePath);
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    KubernetesResource kubernetesResource = ManifestHelper.processYaml(fileContents).get(0);

    Release release = Release.builder()
                          .resources(asList(kubernetesResource.getResourceId()))
                          .number(2)
                          .managedWorkloads(asList(Release.KubernetesResourceIdRevision.builder()
                                                       .workload(kubernetesResource.getResourceId())
                                                       .revision("2")
                                                       .build()))
                          .build();
    Release previousEligibleRelease = Release.builder()
                                          .resources(asList(kubernetesResource.getResourceId()))
                                          .number(1)
                                          .managedWorkloads(asList(Release.KubernetesResourceIdRevision.builder()
                                                                       .workload(kubernetesResource.getResourceId())
                                                                       .revision("1")
                                                                       .build()))
                                          .build();

    when(releaseHistory.getPreviousRollbackEligibleRelease(anyInt())).thenReturn(previousEligibleRelease);
    on(k8sRollingDeployRollbackTaskHandler).set("releaseHistory", releaseHistory);
    on(k8sRollingDeployRollbackTaskHandler).set("release", release);
    on(k8sRollingDeployRollbackTaskHandler).set("client", Kubectl.client("kubectl", "config-path"));

    PowerMockito.mockStatic(Utils.class);
    PowerMockito.when(Utils.encloseWithQuotesIfNeeded("kubectl")).thenReturn("kubectl");
    PowerMockito.when(Utils.encloseWithQuotesIfNeeded("oc")).thenReturn("oc");
    PowerMockito.when(Utils.encloseWithQuotesIfNeeded("config-path")).thenReturn("config-path");

    ProcessResult processResult = new ProcessResult(0, new ProcessOutput("".getBytes()));
    PowerMockito.when(Utils.executeScript(anyString(), anyString(), any(), any())).thenReturn(processResult);

    boolean rollback = k8sRollingDeployRollbackTaskHandler.rollback(
        k8sRollingDeployRollbackTaskParameters, k8sDelegateTaskParams, executionLogCallback);
    assertThat(rollback).isTrue();

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    PowerMockito.verifyStatic(Utils.class);
    Utils.executeScript(any(), captor.capture(), any(), any());
    assertThat(captor.getValue()).isEqualTo(expectedOutput);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void rollback() throws Exception {
    testRollBackReleaseIsNull();
    testRollBackIfNoManagedWorkload();
    testRollBackToSpecificRelease();
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void invalidTypeOfTaskParams() {
    assertThatExceptionOfType(InvalidArgumentsException.class)
        .isThrownBy(() -> k8sRollingDeployRollbackTaskHandler.executeTaskInternal(null, null))
        .withMessageContaining("INVALID_ARGUMENT");
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void executeTaskInternalNoReleaseHistory() throws Exception {
    doReturn(logCallback).when(taskHelper).getExecutionLogCallback(any(K8sTaskParameters.class), anyString());
    k8sRollingDeployRollbackTaskHandler.executeTaskInternal(
        K8sRollingDeployRollbackTaskParameters.builder().build(), K8sDelegateTaskParams.builder().build());
    verify(containerDeploymentDelegateHelper, times(1)).getKubernetesConfig(any(K8sClusterConfig.class), anyBoolean());
    verify(taskHelperBase, times(1)).getReleaseHistoryData(any(KubernetesConfig.class), anyString());
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testSkipRollback() throws Exception {
    K8sDelegateTaskParams k8sDelegateTaskParams =
        K8sDelegateTaskParams.builder().kubectlPath("kubectl").ocPath("oc").kubeconfigPath("config-path").build();
    setRelease();

    boolean rollback = k8sRollingDeployRollbackTaskHandler.rollback(
        K8sRollingDeployRollbackTaskParameters.builder().build(), k8sDelegateTaskParams, logCallback);
    assertThat(rollback).isTrue();
    ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
    verify(logCallback, times(1)).saveExecutionLog(logCaptor.capture());
    assertThat(logCaptor.getValue()).isEqualTo("No failed release found. Skipping rollback.");
  }

  private void setRelease() throws IOException {
    on(k8sRollingDeployRollbackTaskHandler).set("releaseHistory", releaseHistory);
    KubernetesResource currentCustomResource = parseKubernetesResourceFromFile("/k8s/crd-new.yaml");
    Release release = Release.builder()
                          .resources(asList(currentCustomResource.getResourceId()))
                          .customWorkloads(asList(currentCustomResource))
                          .status(Succeeded)
                          .build();
    on(k8sRollingDeployRollbackTaskHandler).set("release", release);
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testSkipRollbackNoPreviousEligibleRelease() throws Exception {
    K8sDelegateTaskParams k8sDelegateTaskParams =
        K8sDelegateTaskParams.builder().kubectlPath("kubectl").ocPath("oc").kubeconfigPath("config-path").build();
    setRelease();
    when(releaseHistory.getPreviousRollbackEligibleRelease(anyInt())).thenReturn(null);

    boolean rollback = k8sRollingDeployRollbackTaskHandler.rollback(
        K8sRollingDeployRollbackTaskParameters.builder().releaseNumber(1).build(), k8sDelegateTaskParams, logCallback);
    assertThat(rollback).isTrue();
    ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
    verify(logCallback).saveExecutionLog(logCaptor.capture());
    assertThat(logCaptor.getValue()).isEqualTo("No previous eligible release found. Can't rollback.");
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testRollbackNoManagedWorkloadInPreviousEligibleRelease() throws Exception {
    K8sRollingDeployRollbackTaskParameters k8sRollingDeployRollbackTaskParameters =
        K8sRollingDeployRollbackTaskParameters.builder().releaseNumber(1).build();
    K8sDelegateTaskParams k8sDelegateTaskParams =
        K8sDelegateTaskParams.builder().kubectlPath("kubectl").ocPath("oc").kubeconfigPath("config-path").build();
    setRelease();
    when(releaseHistory.getPreviousRollbackEligibleRelease(anyInt())).thenReturn(null);

    KubernetesResource previousCustomResource = parseKubernetesResourceFromFile("/k8s/crd-old.yaml");
    Release previousEligibleRelease =
        Release.builder().resources(asList(previousCustomResource.getResourceId())).number(1).status(Succeeded).build();
    when(releaseHistory.getPreviousRollbackEligibleRelease(anyInt())).thenReturn(previousEligibleRelease);

    boolean rollback = k8sRollingDeployRollbackTaskHandler.rollback(
        k8sRollingDeployRollbackTaskParameters, k8sDelegateTaskParams, logCallback);
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
    K8sRollingDeployRollbackTaskHandler spyHandler = spy(K8sRollingDeployRollbackTaskHandler.class);
    on(spyHandler).set("client", Kubectl.client("kubectl", "config-path"));
    doReturn(buildProcessResult(1)).when(spyHandler).runK8sExecutable(any(), any(), any());

    ReleaseHistory releaseHistory = ReleaseHistory.createNew();
    releaseHistory.getReleases().add(buildRelease(Failed, 2));
    releaseHistory.getReleases().add(buildRelease(Succeeded, 1));
    releaseHistory.getReleases().add(buildRelease(Succeeded, 0));
    on(spyHandler).set("release", releaseHistory.getLatestRelease());
    on(spyHandler).set("releaseHistory", releaseHistory);

    final boolean success =
        spyHandler.rollback(K8sRollingDeployRollbackTaskParameters.builder().releaseNumber(2).build(),
            K8sDelegateTaskParams.builder().build(), logCallback);
    assertThat(success).isFalse();

    ArgumentCaptor<RolloutUndoCommand> captor = ArgumentCaptor.forClass(RolloutUndoCommand.class);
    verify(spyHandler, times(1)).runK8sExecutable(any(), any(), captor.capture());
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

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testPrintManagedWorkloads() throws Exception {
    on(k8sRollingDeployRollbackTaskHandler).set("releaseHistory", null);
    doReturn("").when(taskHelperBase).getReleaseHistoryData(any(KubernetesConfig.class), anyString());
    ReleaseHistory releaseHistory = ReleaseHistory.createNew();
    releaseHistory.getReleases().add(buildReleaseMultipleManagedWorkloads(Succeeded));
    String releaseHistoryData = releaseHistory.getAsYaml();

    doReturn(releaseHistoryData).when(taskHelperBase).getReleaseHistoryData(any(), any());
    K8sRollingDeployRollbackTaskParameters k8sRollingDeployRollbackTaskParameters =
        K8sRollingDeployRollbackTaskParameters.builder().build();
    K8sDelegateTaskParams k8sDelegateTaskParams = K8sDelegateTaskParams.builder().build();
    k8sRollingDeployRollbackTaskHandler.executeTaskInternal(
        k8sRollingDeployRollbackTaskParameters, k8sDelegateTaskParams);

    assertThat((Release) on(k8sRollingDeployRollbackTaskHandler).get("release")).isNotNull();
    verify(taskHelperBase, never()).doStatusCheck(any(), any(), any(), any());
    verify(taskHelperBase, never()).saveReleaseHistory(any(), any(), any(), eq(true));
    final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(logCallback, times(1)).saveExecutionLog(captor.capture(), eq(INFO), eq(CommandExecutionStatus.SUCCESS));
    assertThat(captor.getValue())
        .contains("Skipping Status Check since there is no previous eligible Managed Workload.");
  }

  private void testRollBackToSpecificRelease() throws Exception {
    rollback1ManagedWorkload();
    rollbackMultipleWorkloads();
  }

  private void rollbackMultipleWorkloads() throws Exception {
    K8sRollingDeployRollbackTaskHandler spyHandler = spy(K8sRollingDeployRollbackTaskHandler.class);
    on(spyHandler).set("client", Kubectl.client("kubectl", "config-path"));
    doReturn(buildProcessResult(0)).when(spyHandler).runK8sExecutable(any(), any(), any());
    doReturn(buildProcessResult(0)).when(spyHandler).executeScript(any(), anyString(), any(), any());

    ReleaseHistory releaseHistory = ReleaseHistory.createNew();
    releaseHistory.getReleases().add(buildReleaseMultipleManagedWorkloads(Failed));
    releaseHistory.getReleases().add(buildReleaseMultipleManagedWorkloads(Succeeded));
    releaseHistory.getReleases().add(buildReleaseMultipleManagedWorkloads(Succeeded));
    on(spyHandler).set("release", releaseHistory.getLatestRelease());
    on(spyHandler).set("releaseHistory", releaseHistory);

    final boolean success =
        spyHandler.rollback(K8sRollingDeployRollbackTaskParameters.builder().releaseNumber(2).build(),
            K8sDelegateTaskParams.builder().kubeconfigPath("kubeconfig").build(), logCallback);

    ArgumentCaptor<RolloutUndoCommand> captor = ArgumentCaptor.forClass(RolloutUndoCommand.class);
    ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(spyHandler, times(1)).runK8sExecutable(any(), any(), captor.capture());
    verify(spyHandler, times(1)).executeScript(any(), stringArgumentCaptor.capture(), any(), any());

    RolloutUndoCommand rolloutUndoCommand = captor.getValue();
    assertThat(rolloutUndoCommand.command())
        .isEqualTo("kubectl --kubeconfig=config-path rollout undo Deployment/nginx-deployment --to-revision=2");
    assertThat(success).isTrue();

    String command = stringArgumentCaptor.getValue();
    assertThat(command).isEqualTo(
        "oc --kubeconfig=kubeconfig rollout undo DeploymentConfig/test-dc --namespace=default --to-revision=2");
  }

  private void rollback1ManagedWorkload() throws Exception {
    K8sRollingDeployRollbackTaskHandler spyHandler = spy(K8sRollingDeployRollbackTaskHandler.class);
    on(spyHandler).set("client", Kubectl.client("kubectl", "config-path"));
    doReturn(buildProcessResult(0)).when(spyHandler).runK8sExecutable(any(), any(), any());

    ReleaseHistory releaseHistory = ReleaseHistory.createNew();
    releaseHistory.getReleases().add(buildRelease(Failed, 2));
    releaseHistory.getReleases().add(buildRelease(Succeeded, 1));
    releaseHistory.getReleases().add(buildRelease(Succeeded, 0));
    on(spyHandler).set("release", releaseHistory.getLatestRelease());
    on(spyHandler).set("releaseHistory", releaseHistory);

    final boolean success =
        spyHandler.rollback(K8sRollingDeployRollbackTaskParameters.builder().releaseNumber(2).build(),
            K8sDelegateTaskParams.builder().build(), logCallback);

    ArgumentCaptor<RolloutUndoCommand> captor = ArgumentCaptor.forClass(RolloutUndoCommand.class);
    verify(spyHandler, times(1)).runK8sExecutable(any(), any(), captor.capture());

    RolloutUndoCommand rolloutUndoCommand = captor.getValue();
    assertThat(rolloutUndoCommand.command())
        .isEqualTo("kubectl --kubeconfig=config-path rollout undo Deployment/nginx-deployment");
    assertThat(success).isTrue();
  }

  private void testRollBackIfNoManagedWorkload() throws Exception {
    on(k8sRollingDeployRollbackTaskHandler).set("release", new Release());
    final boolean success = k8sRollingDeployRollbackTaskHandler.rollback(
        K8sRollingDeployRollbackTaskParameters.builder().build(), K8sDelegateTaskParams.builder().build(), logCallback);
    assertThat(success).isTrue();
  }

  private void testRollBackReleaseIsNull() throws Exception {
    final boolean success = k8sRollingDeployRollbackTaskHandler.rollback(
        K8sRollingDeployRollbackTaskParameters.builder().build(), K8sDelegateTaskParams.builder().build(), logCallback);

    assertThat(success).isTrue();
  }
}
