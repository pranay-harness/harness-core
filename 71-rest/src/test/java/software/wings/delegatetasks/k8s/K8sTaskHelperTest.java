package software.wings.delegatetasks.k8s;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.k8s.KubernetesConvention.ReleaseHistoryKeyName;
import static io.harness.k8s.manifest.ManifestHelper.processYaml;
import static io.harness.k8s.model.Kind.ConfigMap;
import static io.harness.k8s.model.Kind.Deployment;
import static io.harness.k8s.model.Kind.DeploymentConfig;
import static io.harness.k8s.model.Kind.Namespace;
import static io.harness.k8s.model.Kind.ReplicaSet;
import static io.harness.k8s.model.Kind.Secret;
import static io.harness.k8s.model.Kind.Service;
import static io.harness.logging.LogLevel.INFO;
import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.SAHIL;
import static io.harness.rule.OwnerRule.SATYAM;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static io.harness.rule.OwnerRule.YOGESH;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;
import static software.wings.beans.appmanifest.StoreType.HelmChartRepo;
import static software.wings.beans.appmanifest.StoreType.HelmSourceRepo;
import static software.wings.beans.appmanifest.StoreType.KustomizeSourceRepo;
import static software.wings.beans.appmanifest.StoreType.Local;
import static software.wings.beans.appmanifest.StoreType.OC_TEMPLATES;
import static software.wings.beans.appmanifest.StoreType.Remote;
import static software.wings.delegatetasks.k8s.K8sTestConstants.DAEMON_SET_YAML;
import static software.wings.delegatetasks.k8s.K8sTestConstants.DEPLOYMENT_YAML;
import static software.wings.delegatetasks.k8s.K8sTestConstants.STATEFUL_SET_YAML;
import static software.wings.delegatetasks.k8s.K8sTestHelper.configMap;
import static software.wings.utils.WingsTestConstants.LONG_TIMEOUT_INTERVAL;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.harness.category.element.UnitTests;
import io.harness.delegate.task.k8s.K8sTaskHelperBase;
import io.harness.exception.KubernetesYamlException;
import io.harness.filesystem.FileIo;
import io.harness.k8s.KubernetesContainerService;
import io.harness.k8s.KubernetesHelperService;
import io.harness.k8s.kubectl.AbstractExecutable;
import io.harness.k8s.kubectl.ApplyCommand;
import io.harness.k8s.kubectl.DeleteCommand;
import io.harness.k8s.kubectl.DescribeCommand;
import io.harness.k8s.kubectl.GetJobCommand;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.kubectl.RolloutHistoryCommand;
import io.harness.k8s.kubectl.ScaleCommand;
import io.harness.k8s.kubectl.Utils;
import io.harness.k8s.model.HelmVersion;
import io.harness.k8s.model.K8sDelegateTaskParams;
import io.harness.k8s.model.Kind;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.k8s.model.Release;
import io.harness.k8s.model.Release.Status;
import io.harness.k8s.model.ReleaseHistory;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.zeroturnaround.exec.ProcessOutput;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.StartedProcess;
import org.zeroturnaround.exec.stream.LogOutputStream;
import software.wings.WingsBaseTest;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFileConfig;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.yaml.GitFetchFilesResult;
import software.wings.beans.yaml.GitFile;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.delegatetasks.helm.HelmTaskHelper;
import software.wings.helpers.ext.helm.request.HelmChartConfigParams;
import software.wings.helpers.ext.helm.response.HelmChartInfo;
import software.wings.helpers.ext.k8s.request.K8sApplyTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig;
import software.wings.helpers.ext.k8s.request.K8sDeleteTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sApplyResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskResponse;
import software.wings.helpers.ext.kustomize.KustomizeTaskHelper;
import software.wings.helpers.ext.openshift.OpenShiftDelegateService;
import software.wings.service.intfc.GitService;
import software.wings.service.intfc.security.EncryptionService;
import wiremock.com.google.common.collect.Lists;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/*
 * Do not use powermock because jacoco does not support coverage using it. If you are sure that jacoco supports it
 now,
 * only then use it. Meanwhile, move powermock based tests to K8sTaskHelperSecondaryTest
 */
public class K8sTaskHelperTest extends WingsBaseTest {
  @Mock private ExecutionLogCallback logCallback;
  @Mock private DelegateLogService mockDelegateLogService;
  @Mock private KubernetesContainerService mockKubernetesContainerService;
  @Mock private TimeLimiter mockTimeLimiter;
  @Mock private GitService mockGitService;
  @Mock private EncryptionService mockEncryptionService;
  @Mock private HelmTaskHelper mockHelmTaskHelper;
  @Mock private KubernetesHelperService mockKubernetesHelperService;
  @Mock private ExecutionLogCallback executionLogCallback;
  @Mock private StartedProcess startedProcess;
  @Mock private Process process;
  @Mock private KustomizeTaskHelper kustomizeTaskHelper;
  @Mock private OpenShiftDelegateService openShiftDelegateService;
  @Mock private K8sTaskHelperBase mockK8sTaskHelperBase;

  private String resourcePath = "./k8s";
  private String deploymentYaml = "deployment.yaml";
  private String deploymentConfigYaml = "deployment-config.yaml";
  private String configMapYaml = "configMap.yaml";

  @Inject @InjectMocks private K8sTaskHelperBase k8sTaskHelperBase;
  @Inject @InjectMocks private K8sTaskHelper helper;
  @Inject private K8sTaskHelper spyHelper;
  @Inject private K8sTaskHelperBase spyHelperBase;

  @Before
  public void setUp() throws Exception {
    spyHelper = Mockito.spy(helper);
    spyHelperBase = Mockito.spy(k8sTaskHelperBase);
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testGetTargetInstancesForCanary() {
    ExecutionLogCallback mockLogCallback = logCallback;
    doNothing().when(mockLogCallback).saveExecutionLog(anyString());
    assertThat(k8sTaskHelperBase.getTargetInstancesForCanary(50, 4, mockLogCallback)).isEqualTo(2);
    assertThat(k8sTaskHelperBase.getTargetInstancesForCanary(5, 2, mockLogCallback)).isEqualTo(1);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetResourcesInTableFormat() {
    String expectedResourcesInTableFormat = "\n"
        + "\u001B[1;97m\u001B[40mKind                Name                                    Versioned #==#\n"
        + "\u001B[0;37m\u001B[40mDeployment          deployment                              false     #==#\n"
        + "\u001B[0;37m\u001B[40mStatefulSet         statefulSet                             false     #==#\n"
        + "\u001B[0;37m\u001B[40mDaemonSet           daemonSet                               false     #==#\n";
    List<KubernetesResource> kubernetesResources = new ArrayList<>();
    kubernetesResources.addAll(processYaml(DEPLOYMENT_YAML));
    kubernetesResources.addAll(processYaml(STATEFUL_SET_YAML));
    kubernetesResources.addAll(processYaml(DAEMON_SET_YAML));

    String resourcesInTableFormat = k8sTaskHelperBase.getResourcesInTableFormat(kubernetesResources);

    assertThat(resourcesInTableFormat).isEqualTo(expectedResourcesInTableFormat);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testFetchAllResourcesForRelease() throws Exception {
    K8sDeleteTaskParameters k8sDeleteTaskParameters =
        K8sDeleteTaskParameters.builder().releaseName("releaseName").build();

    ExecutionLogCallback executionLogCallback = logCallback;
    doNothing().when(executionLogCallback).saveExecutionLog(anyString());

    ConfigMap configMap = new ConfigMap();
    configMap.setKind(ConfigMap.name());

    Map<String, String> data = new HashMap<>();
    configMap.setData(data);
    doReturn(configMap).when(mockKubernetesContainerService).getConfigMap(any(), anyString());

    // Empty release history
    List<KubernetesResourceId> kubernetesResourceIds = helper.fetchAllResourcesForRelease(
        k8sDeleteTaskParameters, KubernetesConfig.builder().build(), executionLogCallback);
    assertThat(kubernetesResourceIds).isEmpty();

    data.put(ReleaseHistoryKeyName, null);
    kubernetesResourceIds = helper.fetchAllResourcesForRelease(
        k8sDeleteTaskParameters, KubernetesConfig.builder().build(), executionLogCallback);
    assertThat(kubernetesResourceIds).isEmpty();

    data.put(ReleaseHistoryKeyName, "");
    kubernetesResourceIds = helper.fetchAllResourcesForRelease(
        k8sDeleteTaskParameters, KubernetesConfig.builder().build(), executionLogCallback);
    assertThat(kubernetesResourceIds).isEmpty();

    List<KubernetesResourceId> kubernetesResourceIdList = getKubernetesResourceIdList();
    ReleaseHistory releaseHistory = ReleaseHistory.createNew();
    releaseHistory.setReleases(
        Arrays.asList(Release.builder().status(Status.Succeeded).resources(kubernetesResourceIdList).build()));

    String releaseHistoryString = releaseHistory.getAsYaml();
    data.put(ReleaseHistoryKeyName, releaseHistoryString);
    kubernetesResourceIds = helper.fetchAllResourcesForRelease(
        k8sDeleteTaskParameters, KubernetesConfig.builder().namespace("default").build(), executionLogCallback);

    assertThat(kubernetesResourceIds.size()).isEqualTo(5);
    Set<String> resourceIdentifiers = kubernetesResourceIds.stream()
                                          .map(resourceId
                                              -> new StringBuilder(resourceId.getNamespace())
                                                     .append('/')
                                                     .append(resourceId.getKind())
                                                     .append('/')
                                                     .append(resourceId.getName())
                                                     .toString())
                                          .collect(Collectors.toSet());

    assertThat(resourceIdentifiers.containsAll(Arrays.asList("default/Namespace/n1", "default/Deployment/d1",
                   "default/ConfigMap/c1", "default/ConfigMap/releaseName", "default/Service/s1")))
        .isTrue();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testFetchAllResourcesForReleaseWhenMissingConfigMap() throws Exception {
    K8sDeleteTaskParameters k8sDeleteTaskParameters =
        K8sDeleteTaskParameters.builder().releaseName("releaseName").build();
    KubernetesConfig config = KubernetesConfig.builder().build();

    doReturn(null).when(mockKubernetesContainerService).getConfigMap(config, "releaseName");
    List<KubernetesResourceId> kubernetesResourceIds =
        helper.fetchAllResourcesForRelease(k8sDeleteTaskParameters, config, logCallback);
    assertThat(kubernetesResourceIds).isEmpty();
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGetResourceIdsForDeletion() throws Exception {
    K8sDeleteTaskParameters k8sDeleteTaskParameters =
        K8sDeleteTaskParameters.builder().releaseName("releaseName").build();

    ExecutionLogCallback executionLogCallback = logCallback;
    doNothing().when(executionLogCallback).saveExecutionLog(anyString());

    ConfigMap configMap = new ConfigMap();
    configMap.setKind(ConfigMap.name());
    doReturn(configMap).when(mockKubernetesContainerService).getConfigMap(any(), anyString());
    List<KubernetesResourceId> kubernetesResourceIdList = getKubernetesResourceIdList();
    ReleaseHistory releaseHistory = ReleaseHistory.createNew();
    releaseHistory.setReleases(
        Arrays.asList(Release.builder().status(Status.Succeeded).resources(kubernetesResourceIdList).build()));

    String releaseHistoryString = releaseHistory.getAsYaml();
    Map<String, String> data = new HashMap<>();
    data.put(ReleaseHistoryKeyName, releaseHistoryString);
    configMap.setData(data);

    helper.getResourceIdsForDeletion(
        k8sDeleteTaskParameters, KubernetesConfig.builder().namespace("default").build(), executionLogCallback);

    verify(mockK8sTaskHelperBase, times(1)).arrangeResourceIdsInDeletionOrder(any());
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testArrangeResourceIdsInDeletionOrder() throws Exception {
    List<KubernetesResourceId> kubernetesResourceIdList = getKubernetesResourceIdList();
    kubernetesResourceIdList.add(
        KubernetesResourceId.builder().kind(Secret.name()).name("sc1").namespace("default").build());
    kubernetesResourceIdList.add(
        KubernetesResourceId.builder().kind(ReplicaSet.name()).name("rs1").namespace("default").build());

    kubernetesResourceIdList = k8sTaskHelperBase.arrangeResourceIdsInDeletionOrder(kubernetesResourceIdList);
    assertThat(kubernetesResourceIdList.get(0).getKind()).isEqualTo(Deployment.name());
    assertThat(kubernetesResourceIdList.get(1).getKind()).isEqualTo(ReplicaSet.name());
    assertThat(kubernetesResourceIdList.get(2).getKind()).isEqualTo(Service.name());
    assertThat(kubernetesResourceIdList.get(3).getKind()).isEqualTo(ConfigMap.name());
    assertThat(kubernetesResourceIdList.get(4).getKind()).isEqualTo(Secret.name());
    assertThat(kubernetesResourceIdList.get(5).getKind()).isEqualTo(Namespace.name());
  }

  private List<KubernetesResourceId> getKubernetesResourceIdList() {
    List<KubernetesResourceId> kubernetesResourceIds = new ArrayList<>();
    kubernetesResourceIds.add(
        KubernetesResourceId.builder().kind(Namespace.name()).name("n1").namespace("default").build());
    kubernetesResourceIds.add(
        KubernetesResourceId.builder().kind(Deployment.name()).name("d1").namespace("default").build());
    kubernetesResourceIds.add(
        KubernetesResourceId.builder().kind(ConfigMap.name()).name("c1").namespace("default").build());
    kubernetesResourceIds.add(
        KubernetesResourceId.builder().kind(Service.name()).name("s1").namespace("default").build());
    return kubernetesResourceIds;
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGetHelmV2CommandForRender() {
    String command = helper.getHelmCommandForRender(
        "helm", "chart_location", "test-release", "default", " -f values-0.yaml", HelmVersion.V2);
    assertThat(command).doesNotContain("$").doesNotContain("{").doesNotContain("}");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGetHelmV2CommandForRenderOneChartFile() {
    String command = helper.getHelmCommandForRender("helm", "chart_location", "test-release", "default",
        " -f values-0.yaml", "template/service.yaml", HelmVersion.V2);
    assertThat(command).doesNotContain("$").doesNotContain("{").doesNotContain("}");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGetHelmV3CommandForRender() {
    String command = helper.getHelmCommandForRender(
        "helm", "chart_location", "test-release", "default", " -f values-0.yaml", HelmVersion.V3);
    assertThat(command).doesNotContain("$").doesNotContain("{").doesNotContain("}");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGetHelmV3CommandForRenderOneChartFile() {
    String command = helper.getHelmCommandForRender("helm", "chart_location", "test-release", "default",
        " -f values-0.yaml", "template/service.yaml", HelmVersion.V3);
    assertThat(command).doesNotContain("$").doesNotContain("{").doesNotContain("}");
  }

  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetLatestRevision() throws Exception {
    URL url = this.getClass().getResource("/k8s/deployment-config.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    KubernetesResource resource = processYaml(fileContents).get(0);

    K8sDelegateTaskParams k8sDelegateTaskParams = K8sDelegateTaskParams.builder().build();
    Kubectl client = Kubectl.client("kubectl", "config-path");

    String output = "deploymentconfigs \"test-dc\"\n"
        + "REVISION\tSTATUS\t\tCAUSE\n"
        + "35\t\tComplete\tconfig change\n"
        + "36\t\tComplete\tconfig change";

    PowerMockito.mockStatic(Utils.class);
    ProcessResult processResult = new ProcessResult(0, new ProcessOutput(output.getBytes()));
    when(Utils.executeScript(anyString(), anyString(), any(), any())).thenReturn(processResult);

    String latestRevision =
        k8sTaskHelperBase.getLatestRevision(client, resource.getResourceId(), k8sDelegateTaskParams);
    assertThat(latestRevision).isEqualTo("36");

    PowerMockito.mockStatic(Utils.class);
    processResult = new ProcessResult(1, new ProcessOutput("".getBytes()));
    when(Utils.executeScript(anyString(), anyString(), any(), any())).thenReturn(processResult);

    latestRevision = k8sTaskHelperBase.getLatestRevision(client, resource.getResourceId(), k8sDelegateTaskParams);
    assertThat(latestRevision).isEqualTo("");
  }

  private void setupForDoStatusCheckForAllResources() throws Exception {
    ProcessResult processResult = new ProcessResult(0, new ProcessOutput("".getBytes()));
    doReturn(processResult)
        .when(spyHelperBase)
        .executeCommandUsingUtils(any(K8sDelegateTaskParams.class), any(), any(), any());
    StartedProcess startedProcess = mock(StartedProcess.class);
    Process process = mock(Process.class);
    doReturn(startedProcess).when(spyHelperBase).getEventWatchProcess(any(), any(), any(), any());
    doReturn(process).when(startedProcess).getProcess();
    doReturn(process).when(process).destroyForcibly();
    doReturn(0).when(process).waitFor();
  }

  private void doStatusCheck(String manifestFilePath, String expectedOutput, boolean allResources) throws Exception {
    URL url = this.getClass().getResource(manifestFilePath);
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    KubernetesResource resource = processYaml(fileContents).get(0);

    K8sDelegateTaskParams k8sDelegateTaskParams =
        K8sDelegateTaskParams.builder().kubectlPath("kubectl").ocPath("oc").kubeconfigPath("config-path").build();
    Kubectl client = Kubectl.client("kubectl", "config-path");

    if (allResources) {
      spyHelperBase.doStatusCheckForAllResources(
          client, asList(resource.getResourceId()), k8sDelegateTaskParams, "default", executionLogCallback, true);
    } else {
      spyHelperBase.doStatusCheck(client, resource.getResourceId(), k8sDelegateTaskParams, executionLogCallback);
    }

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(spyHelperBase, times(1))
        .executeCommandUsingUtils(any(K8sDelegateTaskParams.class), any(), any(), eq(expectedOutput));
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testDryRunForOpenshiftResources() throws Exception {
    ProcessResult processResult = new ProcessResult(0, new ProcessOutput("abc".getBytes()));
    doReturn(processResult).when(spyHelperBase).runK8sExecutable(any(), any(), any());

    final String workingDirectory = ".";
    K8sDelegateTaskParams k8sDelegateTaskParams = K8sDelegateTaskParams.builder()
                                                      .workingDirectory(workingDirectory)
                                                      .ocPath("oc")
                                                      .kubectlPath("kubectl")
                                                      .kubeconfigPath("config-path")
                                                      .build();
    Kubectl client = Kubectl.client("kubectl", "config-path");

    spyHelperBase.dryRunManifests(client, emptyList(), k8sDelegateTaskParams, executionLogCallback);

    ArgumentCaptor<ApplyCommand> captor = ArgumentCaptor.forClass(ApplyCommand.class);
    verify(spyHelperBase, times(1)).runK8sExecutable(any(), any(), captor.capture());
    assertThat(captor.getValue().command())
        .isEqualTo("kubectl --kubeconfig=config-path apply --filename=manifests-dry-run.yaml --dry-run");
    reset(spyHelperBase);

    doReturn(processResult).when(spyHelperBase).runK8sExecutable(any(), any(), any());
    spyHelperBase.dryRunManifests(client,
        asList(KubernetesResource.builder()
                   .spec("")
                   .resourceId(KubernetesResourceId.builder().kind("Route").build())
                   .build()),
        k8sDelegateTaskParams, executionLogCallback);
    verify(spyHelperBase, times(1)).runK8sExecutable(any(), any(), captor.capture());
    assertThat(captor.getValue().command())
        .isEqualTo("oc --kubeconfig=config-path apply --filename=manifests-dry-run.yaml --dry-run");
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testApplyForOpenshiftResources() throws Exception {
    ProcessResult processResult = new ProcessResult(0, new ProcessOutput("abc".getBytes()));
    doReturn(processResult).when(spyHelperBase).runK8sExecutable(any(), any(), any(AbstractExecutable.class));

    final String workingDirectory = ".";
    K8sDelegateTaskParams k8sDelegateTaskParams = K8sDelegateTaskParams.builder()
                                                      .workingDirectory(workingDirectory)
                                                      .kubectlPath("kubectl")
                                                      .ocPath("oc")
                                                      .kubeconfigPath("config-path")
                                                      .build();
    //    createDirectoryIfDoesNotExist(Paths.get("/tmp/test").toString());
    Kubectl client = Kubectl.client("kubectl", "config-path");

    spyHelperBase.applyManifests(client, emptyList(), k8sDelegateTaskParams, executionLogCallback);

    ArgumentCaptor<ApplyCommand> captor = ArgumentCaptor.forClass(ApplyCommand.class);
    verify(spyHelperBase, times(1)).runK8sExecutable(any(), any(), captor.capture());
    assertThat(captor.getValue().command())
        .isEqualTo("kubectl --kubeconfig=config-path apply --filename=manifests.yaml --record");
    reset(spyHelperBase);

    doReturn(processResult).when(spyHelperBase).runK8sExecutable(any(), any(), any(AbstractExecutable.class));
    spyHelperBase.applyManifests(client,
        asList(KubernetesResource.builder()
                   .spec("")
                   .resourceId(KubernetesResourceId.builder().kind("Route").build())
                   .build()),
        k8sDelegateTaskParams, executionLogCallback);
    verify(spyHelperBase, times(1)).runK8sExecutable(any(), any(), captor.capture());
    assertThat(captor.getValue().command())
        .isEqualTo("oc --kubeconfig=config-path apply --filename=manifests.yaml --record");
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testDeleteForOpenshiftResources() throws Exception {
    ProcessResult processResult = new ProcessResult(0, new ProcessOutput("abc".getBytes()));
    doReturn(processResult).when(spyHelperBase).runK8sExecutable(any(), any(), any(AbstractExecutable.class));

    final String workingDirectory = ".";
    K8sDelegateTaskParams k8sDelegateTaskParams = K8sDelegateTaskParams.builder()
                                                      .workingDirectory(workingDirectory)
                                                      .kubectlPath("kubectl")
                                                      .ocPath("oc")
                                                      .kubeconfigPath("config-path")
                                                      .build();
    Kubectl client = Kubectl.client("kubectl", "config-path");

    spyHelperBase.deleteManifests(client, emptyList(), k8sDelegateTaskParams, executionLogCallback);

    ArgumentCaptor<DeleteCommand> captor = ArgumentCaptor.forClass(DeleteCommand.class);
    verify(spyHelperBase, times(1)).runK8sExecutable(any(), any(), captor.capture());
    assertThat(captor.getValue().command())
        .isEqualTo("kubectl --kubeconfig=config-path delete --filename=manifests.yaml");
    reset(spyHelperBase);

    doReturn(processResult).when(spyHelperBase).runK8sExecutable(any(), any(), any(AbstractExecutable.class));
    spyHelperBase.deleteManifests(client,
        asList(KubernetesResource.builder()
                   .spec("")
                   .resourceId(KubernetesResourceId.builder().kind("Route").build())
                   .build()),
        k8sDelegateTaskParams, executionLogCallback);
    verify(spyHelperBase, times(1)).runK8sExecutable(any(), any(), captor.capture());
    assertThat(captor.getValue().command()).isEqualTo("oc --kubeconfig=config-path delete --filename=manifests.yaml");
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void doStatusCheckForJob() throws Exception {
    String RANDOM = "RANDOM";
    K8sDelegateTaskParams k8sDelegateTaskParams = K8sDelegateTaskParams.builder().workingDirectory(RANDOM).build();
    GetJobCommand jobStatusCommand = spy(new GetJobCommand(null, null, null));
    doReturn(null).when(jobStatusCommand).execute(RANDOM, null, null, false);

    shouldReturnFalseWhenCompletedJobCommandFailed(RANDOM, k8sDelegateTaskParams, jobStatusCommand);
    shouldReturnFalseWhenCompletedTimeCommandFailed(RANDOM, k8sDelegateTaskParams, jobStatusCommand);
    shouldReturnTrueWhenCompletedTimeReached(RANDOM, k8sDelegateTaskParams, jobStatusCommand);
    shouldReturnFalseWhenFailedJobCommandFailed(RANDOM, k8sDelegateTaskParams, jobStatusCommand);
    shouldReturnFalseWhenJobStatusIsFailed(RANDOM, k8sDelegateTaskParams, jobStatusCommand);
  }

  private void shouldReturnFalseWhenFailedJobCommandFailed(
      String RANDOM, K8sDelegateTaskParams k8sDelegateTaskParams, GetJobCommand jobStatusCommand) throws Exception {
    GetJobCommand jobCompletionStatus = spy(new GetJobCommand(null, null, null));
    ProcessResult jobStatusResult = new ProcessResult(0, new ProcessOutput("".getBytes()));
    GetJobCommand jobFailedCommand = spy(new GetJobCommand(null, null, null));
    ProcessResult jobFailedResult = new ProcessResult(1, new ProcessOutput("True".getBytes()));

    doReturn(jobStatusResult).when(jobCompletionStatus).execute(RANDOM, null, null, false);
    doReturn(jobFailedResult).when(jobFailedCommand).execute(RANDOM, null, null, false);

    assertThat(k8sTaskHelperBase.getJobStatus(
                   k8sDelegateTaskParams, null, null, jobCompletionStatus, jobFailedCommand, jobStatusCommand, null))
        .isFalse();
  }

  private void shouldReturnFalseWhenCompletedTimeCommandFailed(
      String RANDOM, K8sDelegateTaskParams k8sDelegateTaskParams, GetJobCommand jobStatusCommand) throws Exception {
    GetJobCommand jobCompletionStatus = spy(new GetJobCommand(null, null, null));
    ProcessResult jobStatusResult = new ProcessResult(0, new ProcessOutput("True".getBytes()));
    GetJobCommand jobCompletionCommand = spy(new GetJobCommand(null, null, null));
    ProcessResult jobCompletionTimeResult = new ProcessResult(1, new ProcessOutput("time".getBytes()));

    doReturn(jobStatusResult).when(jobCompletionStatus).execute(RANDOM, null, null, false);
    doReturn(jobCompletionTimeResult).when(jobCompletionCommand).execute(RANDOM, null, null, false);

    assertThat(k8sTaskHelperBase.getJobStatus(k8sDelegateTaskParams, null, null, jobCompletionStatus, null,
                   jobStatusCommand, jobCompletionCommand))
        .isFalse();
  }

  private void shouldReturnFalseWhenJobStatusIsFailed(
      String RANDOM, K8sDelegateTaskParams k8sDelegateTaskParams, GetJobCommand jobStatusCommand) throws Exception {
    GetJobCommand jobCompletionStatus = spy(new GetJobCommand(null, null, null));
    ProcessResult jobStatusResult = new ProcessResult(0, new ProcessOutput("".getBytes()));
    GetJobCommand jobFailedCommand = spy(new GetJobCommand(null, null, null));
    ProcessResult jobFailedResult = new ProcessResult(0, new ProcessOutput("True".getBytes()));

    doReturn(jobStatusResult).when(jobCompletionStatus).execute(RANDOM, null, null, false);
    doReturn(jobFailedResult).when(jobFailedCommand).execute(RANDOM, null, null, false);

    assertThat(k8sTaskHelperBase.getJobStatus(
                   k8sDelegateTaskParams, null, null, jobCompletionStatus, jobFailedCommand, jobStatusCommand, null))
        .isFalse();
  }

  private void shouldReturnTrueWhenCompletedTimeReached(
      String RANDOM, K8sDelegateTaskParams k8sDelegateTaskParams, GetJobCommand jobStatusCommand) throws Exception {
    GetJobCommand jobCompletionStatus = spy(new GetJobCommand(null, null, null));
    ProcessResult jobStatusResult = new ProcessResult(0, new ProcessOutput("True".getBytes()));
    GetJobCommand jobCompletionCommand = spy(new GetJobCommand(null, null, null));
    ProcessResult jobCompletionTimeResult = new ProcessResult(0, new ProcessOutput("time".getBytes()));

    doReturn(jobStatusResult).when(jobCompletionStatus).execute(RANDOM, null, null, false);
    doReturn(jobCompletionTimeResult).when(jobCompletionCommand).execute(RANDOM, null, null, false);

    assertThat(k8sTaskHelperBase.getJobStatus(k8sDelegateTaskParams, null, null, jobCompletionStatus, null,
                   jobStatusCommand, jobCompletionCommand))
        .isTrue();
  }

  private void shouldReturnFalseWhenCompletedJobCommandFailed(
      String RANDOM, K8sDelegateTaskParams k8sDelegateTaskParams, GetJobCommand jobStatusCommand) throws Exception {
    GetJobCommand jobCompletionStatus = spy(new GetJobCommand(null, null, null));
    ProcessResult jobStatusResult = new ProcessResult(1, new ProcessOutput("FAILURE".getBytes()));

    doReturn(jobStatusResult).when(jobCompletionStatus).execute(RANDOM, null, null, false);

    assertThat(k8sTaskHelperBase.getJobStatus(
                   k8sDelegateTaskParams, null, null, jobCompletionStatus, null, jobStatusCommand, null))
        .isFalse();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void scaleFailure() throws Exception {
    Kubectl kubectl = Kubectl.client("kubectl", "config-path");
    doReturn(new ProcessResult(1, new ProcessOutput("failure".getBytes())))
        .when(spyHelperBase)
        .runK8sExecutable(any(), any(), any());
    final boolean success = spyHelperBase.scale(kubectl, K8sDelegateTaskParams.builder().build(),
        KubernetesResourceId.builder().name("nginx").kind("Deployment").namespace("default").build(), 5, logCallback);
    assertThat(success).isFalse();
    ArgumentCaptor<ScaleCommand> captor = ArgumentCaptor.forClass(ScaleCommand.class);
    verify(spyHelperBase, times(1)).runK8sExecutable(any(), any(), captor.capture());
    assertThat(captor.getValue().command())
        .isEqualTo("kubectl --kubeconfig=config-path scale Deployment/nginx --namespace=default --replicas=5");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void scaleSuccess() throws Exception {
    Kubectl kubectl = Kubectl.client("kubectl", "config-path");
    doReturn(new ProcessResult(0, null)).when(spyHelperBase).runK8sExecutable(any(), any(), any());
    final boolean success = spyHelperBase.scale(kubectl, K8sDelegateTaskParams.builder().workingDirectory(".").build(),
        KubernetesResourceId.builder().name("nginx").kind("Deployment").namespace("default").build(), 5, logCallback);

    assertThat(success).isTrue();
    ArgumentCaptor<ScaleCommand> captor = ArgumentCaptor.forClass(ScaleCommand.class);
    verify(spyHelperBase, times(1)).runK8sExecutable(any(), any(), captor.capture());
    assertThat(captor.getValue().command())
        .isEqualTo("kubectl --kubeconfig=config-path scale Deployment/nginx --namespace=default --replicas=5");
  }

  //  @Test
  //  @Owner(developers = YOGESH)
  //  @Category(UnitTests.class)
  //  public void delete() throws Exception {
  //    final ReleaseHistory releaseHistory = ReleaseHistory.createNew();
  //    releaseHistory.getReleases().add(K8sTestHelper.buildRelease(Status.Succeeded, 0));
  //    doReturn(K8sTestHelper.buildProcessResult(0)).when(spyHelper).k8sTaskHelperBase.runK8sExecutable(any(), any(),
  //    any()); spyHelper.delete(Kubectl.client("kubectl", "kubeconfig"), K8sDelegateTaskParams.builder().build(),
  //        asList(configMap().getResourceId()), logCallback);
  //    ArgumentCaptor<DeleteCommand> captor = ArgumentCaptor.forClass(DeleteCommand.class);
  //    verify(spyHelper, times(1)).k8sTaskHelperBase.runK8sExecutable(any(), any(), captor.capture());
  //    final List<DeleteCommand> deleteCommands = captor.getAllValues();
  //    assertThat(deleteCommands).hasSize(1);
  //    assertThat(deleteCommands.get(0).command()).isEqualTo("kubectl --kubeconfig=kubeconfig delete
  //    ConfigMap/configMap");
  //  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void cleanUp() throws Exception {
    cleanUpIfOnly1FailedRelease();
    cleanUpIfMultipleFailedReleases();
    cleanUpAllOlderReleases();
  }

  private void cleanUpAllOlderReleases() throws Exception {
    final ReleaseHistory releaseHistory = ReleaseHistory.createNew();
    releaseHistory.getReleases().add(K8sTestHelper.buildRelease(Status.Succeeded, 3));
    releaseHistory.getReleases().add(K8sTestHelper.buildRelease(Status.Succeeded, 2));
    releaseHistory.getReleases().add(K8sTestHelper.buildRelease(Status.Succeeded, 1));
    releaseHistory.getReleases().add(K8sTestHelper.buildRelease(Status.Succeeded, 0));
    doReturn(K8sTestHelper.buildProcessResult(0)).when(spyHelperBase).runK8sExecutable(any(), any(), any());
    spyHelperBase.cleanup(
        Kubectl.client("kubectl", "kubeconfig"), K8sDelegateTaskParams.builder().build(), releaseHistory, logCallback);
    ArgumentCaptor<DeleteCommand> captor = ArgumentCaptor.forClass(DeleteCommand.class);
    verify(spyHelperBase, times(3)).runK8sExecutable(any(), any(), captor.capture());
    final List<DeleteCommand> deleteCommands = captor.getAllValues();
    assertThat(releaseHistory.getReleases()).hasSize(1);
    assertThat(deleteCommands.get(0).command()).isEqualTo("kubectl --kubeconfig=kubeconfig delete ConfigMap/configMap");
    reset(spyHelperBase);
  }

  private void cleanUpIfMultipleFailedReleases() throws Exception {
    final ReleaseHistory releaseHistory = ReleaseHistory.createNew();
    releaseHistory.getReleases().add(K8sTestHelper.buildRelease(Status.Failed, 3));
    releaseHistory.getReleases().add(K8sTestHelper.buildRelease(Status.Failed, 2));
    releaseHistory.getReleases().add(K8sTestHelper.buildRelease(Status.Succeeded, 1));
    releaseHistory.getReleases().add(K8sTestHelper.buildRelease(Status.Failed, 0));
    doReturn(K8sTestHelper.buildProcessResult(0)).when(spyHelperBase).runK8sExecutable(any(), any(), any());
    spyHelperBase.cleanup(
        Kubectl.client("kubectl", "kubeconfig"), K8sDelegateTaskParams.builder().build(), releaseHistory, logCallback);
    ArgumentCaptor<DeleteCommand> captor = ArgumentCaptor.forClass(DeleteCommand.class);
    verify(spyHelperBase, times(3)).runK8sExecutable(any(), any(), captor.capture());
    final List<DeleteCommand> deleteCommands = captor.getAllValues();
    assertThat(releaseHistory.getReleases()).hasSize(1);
    assertThat(deleteCommands.get(0).command()).isEqualTo("kubectl --kubeconfig=kubeconfig delete ConfigMap/configMap");
    reset(spyHelperBase);
  }

  private void cleanUpIfOnly1FailedRelease() throws Exception {
    final ReleaseHistory releaseHistory = ReleaseHistory.createNew();
    releaseHistory.getReleases().add(Release.builder()
                                         .number(0)
                                         .resources(asList(K8sTestHelper.deployment().getResourceId()))
                                         .status(Status.Failed)
                                         .build());
    k8sTaskHelperBase.cleanup(
        mock(Kubectl.class), K8sDelegateTaskParams.builder().build(), releaseHistory, logCallback);
    assertThat(releaseHistory.getReleases()).isEmpty();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void getCurrentReplicas() throws Exception {
    doReturn(K8sTestHelper.buildProcessResult(0, "3"))
        .doReturn(K8sTestHelper.buildProcessResult(1))
        .when(spyHelperBase)
        .runK8sExecutableSilent(any(), any());
    assertThat(spyHelperBase.getCurrentReplicas(Kubectl.client("kubectl", "kubeconfig"),
                   K8sTestHelper.deployment().getResourceId(), K8sDelegateTaskParams.builder().build()))
        .isEqualTo(3);

    assertThat(spyHelperBase.getCurrentReplicas(Kubectl.client("kubectl", "kubeconfig"),
                   K8sTestHelper.deployment().getResourceId(), K8sDelegateTaskParams.builder().build()))
        .isNull();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void getLatestRevisionForDeploymentConfig() throws Exception {
    doReturn(K8sTestHelper.buildProcessResult(0,
                 "deploymentconfig.apps.openshift.io/anshul-dc\n"
                     + "REVISION\tSTATUS\t\tCAUSE\n"
                     + "137\t\tComplete\tconfig change\n"
                     + "138\t\tComplete\tconfig change\n"
                     + "139\t\tComplete\tconfig change\n"
                     + "140\t\tComplete\tconfig change\n"))
        .when(spyHelperBase)
        .executeCommandUsingUtils(any(K8sDelegateTaskParams.class), any(), any(), any());
    String latestRevision;
    latestRevision = spyHelperBase.getLatestRevision(Kubectl.client("kubectl", "kubeconfig"),
        K8sTestHelper.deploymentConfig().getResourceId(),
        K8sDelegateTaskParams.builder()
            .ocPath("oc")
            .kubeconfigPath("kubeconfig")
            .workingDirectory("./working-dir")
            .build());

    verify(spyHelperBase, times(1))
        .executeCommandUsingUtils(eq(K8sDelegateTaskParams.builder()
                                          .ocPath("oc")
                                          .kubeconfigPath("kubeconfig")
                                          .workingDirectory("./working-dir")
                                          .build()),
            any(), any(),
            eq("oc --kubeconfig=kubeconfig rollout history DeploymentConfig/test-dc --namespace=default"));
    assertThat(latestRevision).isEqualTo("140");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void getLatestRevisionForDeployment() throws Exception {
    doReturn(
        K8sTestHelper.buildProcessResult(0,
            "deployments \"nginx-deployment\"\n"
                + "REVISION    CHANGE-CAUSE\n"
                + "1           kubectl apply --filename=https://k8s.io/examples/controllers/nginx-deployment.yaml --record=true\n"
                + "2           kubectl set image deployment.v1.apps/nginx-deployment nginx=nginx:1.16.1 --record=true\n"
                + "3           kubectl set image deployment.v1.apps/nginx-deployment nginx=nginx:1.161 --record=true"))
        .when(spyHelperBase)
        .runK8sExecutableSilent(any(), any());
    String latestRevision;
    latestRevision = spyHelperBase.getLatestRevision(Kubectl.client("kubectl", "kubeconfig"),
        K8sTestHelper.deployment().getResourceId(),
        K8sDelegateTaskParams.builder()
            .kubectlPath("kubectl")
            .kubeconfigPath("kubeconfig")
            .workingDirectory("./working-dir")
            .build());

    ArgumentCaptor<RolloutHistoryCommand> captor = ArgumentCaptor.forClass(RolloutHistoryCommand.class);
    verify(spyHelperBase, times(1))
        .runK8sExecutableSilent(eq(K8sDelegateTaskParams.builder()
                                        .kubectlPath("kubectl")
                                        .kubeconfigPath("kubeconfig")
                                        .workingDirectory("./working-dir")
                                        .build()),
            captor.capture());
    RolloutHistoryCommand rolloutHistoryCommand = captor.getValue();
    assertThat(rolloutHistoryCommand.command())
        .isEqualTo("kubectl --kubeconfig=kubeconfig rollout history Deployment/nginx-deployment");
    assertThat(latestRevision).isEqualTo("3");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void readManifests() throws IOException {
    final List<KubernetesResource> resources = helper.readManifests(prepareSomeCorrectManifestFiles(), logCallback);
    assertThat(resources).hasSize(3);
    assertThat(resources.stream()
                   .map(KubernetesResource::getResourceId)
                   .map(KubernetesResourceId::getKind)
                   .collect(Collectors.toList()))
        .containsExactly("ConfigMap", "Deployment", "DeploymentConfig");
    assertThatExceptionOfType(KubernetesYamlException.class)
        .isThrownBy(() -> helper.readManifests(prepareSomeInCorrectManifestFiles(), logCallback));
  }

  private List<ManifestFile> prepareSomeCorrectManifestFiles() throws IOException {
    return Arrays.asList(ManifestFile.builder()
                             .fileContent(K8sTestHelper.readFileContent(deploymentYaml, resourcePath))
                             .fileName(deploymentYaml)
                             .build(),
        ManifestFile.builder()
            .fileName(deploymentConfigYaml)
            .fileContent(K8sTestHelper.readFileContent(deploymentConfigYaml, resourcePath))
            .build(),
        ManifestFile.builder()
            .fileName(configMapYaml)
            .fileContent(K8sTestHelper.readFileContent(configMapYaml, resourcePath))
            .build());
  }

  private ManifestFile prepareValuesYamlFile() {
    return ManifestFile.builder().fileName("values.yaml").fileContent("key:value").build();
  }

  private List<ManifestFile> prepareSomeInCorrectManifestFiles() throws IOException {
    return Arrays.asList(ManifestFile.builder().fileContent("some-random-content").fileName("manifest.yaml").build(),
        ManifestFile.builder().fileContent("not-a-manifest-file").fileName("a.txt").build());
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void setNameSpaceToKubernetesResources() throws IOException {
    k8sTaskHelperBase.setNamespaceToKubernetesResourcesIfRequired(null, "default");
    k8sTaskHelperBase.setNamespaceToKubernetesResourcesIfRequired(emptyList(), "default");
    KubernetesResource deployment = K8sTestHelper.deployment();
    deployment.getResourceId().setNamespace(null);
    KubernetesResource configMap = configMap();
    configMap.getResourceId().setNamespace("default");
    k8sTaskHelperBase.setNamespaceToKubernetesResourcesIfRequired(asList(deployment, configMap), "harness");
    assertThat(deployment.getResourceId().getNamespace()).isEqualTo("harness");
    assertThat(configMap.getResourceId().getNamespace()).isEqualTo("default");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void getResourcesInStringFormat() throws IOException {
    final String resourcesInStringFormat = K8sTaskHelperBase.getResourcesInStringFormat(
        asList(K8sTestHelper.deployment().getResourceId(), configMap().getResourceId()));
    assertThat(resourcesInStringFormat)
        .isEqualTo("\n"
            + "- Deployment/nginx-deployment\n"
            + "- ConfigMap/configMap");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void describe() throws Exception {
    doReturn(K8sTestHelper.buildProcessResult(0)).when(spyHelperBase).runK8sExecutable(any(), any(), any());
    spyHelperBase.describe(Kubectl.client("kubectl", "kubeconfig"),
        K8sDelegateTaskParams.builder().workingDirectory("./working-dir").build(), logCallback);
    ArgumentCaptor<DescribeCommand> captor = ArgumentCaptor.forClass(DescribeCommand.class);
    verify(spyHelperBase, times(1))
        .runK8sExecutable(
            eq(K8sDelegateTaskParams.builder().workingDirectory("./working-dir").build()), any(), captor.capture());
    assertThat(captor.getValue().command())
        .isEqualTo("kubectl --kubeconfig=kubeconfig describe --filename=manifests.yaml");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void fetchManifestFilesAndWriteToDirectory() throws Exception {
    fetchManifestFilesAndWriteToDirectory_local();
    fetchManifestFilesAndWriteToDirectory_helmChartRepo();
    fetchManifestFilesAndWriteToDirectory_gitRepo(HelmSourceRepo);
    fetchManifestFilesAndWriteToDirectory_gitRepo(Remote);
    fetchManifestFilesAndWriteToDirectory_gitRepo(OC_TEMPLATES);
    fetchManifestFilesAndWriteToDirectory_gitRepo(KustomizeSourceRepo);
  }

  private void fetchManifestFilesAndWriteToDirectory_gitRepo(StoreType storeType) throws IOException {
    K8sTaskHelper spyHelper = spy(helper);
    doReturn("").when(spyHelperBase).getManifestFileNamesInLogFormat(anyString());
    assertThat(
        spyHelper.fetchManifestFilesAndWriteToDirectory(
            K8sDelegateManifestConfig.builder()
                .manifestStoreTypes(storeType)
                .gitConfig(GitConfig.builder().repoUrl("helm-url").build())
                .gitFileConfig(
                    GitFileConfig.builder().filePath("dir/file").branch("master").connectorId("git-connector").build())
                .build(),
            "./dir", logCallback, LONG_TIMEOUT_INTERVAL))
        .isTrue();

    verify(mockGitService, times(1))
        .downloadFiles(eq(GitConfig.builder().repoUrl("helm-url").build()), any(GitFileConfig.class), eq("./dir"));
    verify(mockEncryptionService, times(1)).decrypt(any(), anyList());

    // handle exception
    doThrow(new RuntimeException())
        .when(mockGitService)
        .downloadFiles(any(GitConfig.class), any(GitFileConfig.class), anyString());
    assertThat(
        spyHelper.fetchManifestFilesAndWriteToDirectory(
            K8sDelegateManifestConfig.builder()
                .manifestStoreTypes(storeType)
                .gitConfig(GitConfig.builder().repoUrl("helm-url").build())
                .gitFileConfig(
                    GitFileConfig.builder().filePath("dir/file").branch("master").connectorId("git-connector").build())
                .build(),
            "./dir", logCallback, LONG_TIMEOUT_INTERVAL))
        .isFalse();
    reset(mockGitService);
    reset(mockEncryptionService);
  }

  private void fetchManifestFilesAndWriteToDirectory_helmChartRepo() throws Exception {
    doReturn("").when(mockK8sTaskHelperBase).getManifestFileNamesInLogFormat(anyString());
    final HelmChartConfigParams helmChartConfigParams = HelmChartConfigParams.builder().chartVersion("1.0").build();
    assertThat(helper.fetchManifestFilesAndWriteToDirectory(K8sDelegateManifestConfig.builder()
                                                                .manifestStoreTypes(StoreType.HelmChartRepo)
                                                                .helmChartConfigParams(helmChartConfigParams)
                                                                .build(),
                   "dir", logCallback, LONG_TIMEOUT_INTERVAL))
        .isTrue();

    verify(mockHelmTaskHelper, times(1)).printHelmChartInfoInExecutionLogs(helmChartConfigParams, logCallback);
    verify(mockHelmTaskHelper, times(1)).downloadChartFiles(eq(helmChartConfigParams), eq("dir"), anyLong());

    doThrow(new RuntimeException())
        .when(mockHelmTaskHelper)
        .downloadChartFiles(any(HelmChartConfigParams.class), anyString(), anyLong());
    assertThat(helper.fetchManifestFilesAndWriteToDirectory(K8sDelegateManifestConfig.builder()
                                                                .manifestStoreTypes(StoreType.HelmChartRepo)
                                                                .helmChartConfigParams(helmChartConfigParams)
                                                                .build(),
                   "dir", logCallback, LONG_TIMEOUT_INTERVAL))
        .isFalse();
  }

  private void fetchManifestFilesAndWriteToDirectory_local() throws IOException {
    String manifestFileDirectory = Files.createTempDirectory(generateUuid()).toString();
    List<ManifestFile> manifestFiles = new ArrayList<>(prepareSomeCorrectManifestFiles());
    manifestFiles.add(prepareValuesYamlFile());
    boolean success = helper.fetchManifestFilesAndWriteToDirectory(
        K8sDelegateManifestConfig.builder().manifestStoreTypes(Local).manifestFiles(manifestFiles).build(),
        manifestFileDirectory, logCallback, LONG_TIMEOUT_INTERVAL);
    assertThat(success).isTrue();
    assertThat(Arrays.stream(new File(manifestFileDirectory).listFiles())
                   .filter(file -> file.length() > 0)
                   .map(File::getName)
                   .collect(Collectors.toList()))
        .containsExactlyInAnyOrder("deployment.yaml", "deployment-config.yaml", "configMap.yaml");
    FileIo.deleteDirectoryAndItsContentIfExists(manifestFileDirectory);

    // only values.yaml
    assertThat(helper.fetchManifestFilesAndWriteToDirectory(K8sDelegateManifestConfig.builder()
                                                                .manifestFiles(asList(prepareValuesYamlFile()))
                                                                .manifestStoreTypes(Local)
                                                                .build(),
                   manifestFileDirectory, logCallback, LONG_TIMEOUT_INTERVAL))
        .isTrue();

    // invalid manifest files directory
    assertThat(helper.fetchManifestFilesAndWriteToDirectory(K8sDelegateManifestConfig.builder()
                                                                .manifestStoreTypes(Local)
                                                                .manifestFiles(prepareSomeCorrectManifestFiles())
                                                                .build(),
                   "", logCallback, LONG_TIMEOUT_INTERVAL))
        .isFalse();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void manifestFilesFromGitFetchFilesResult() {
    List<ManifestFile> manifestFiles = K8sTaskHelper.manifestFilesFromGitFetchFilesResult(
        GitFetchFilesResult.builder()
            .files(asList(GitFile.builder().fileContent("abc").filePath("file-1").build()))
            .build(),
        "");
    assertThat(manifestFiles).hasSize(1);
    assertThat(manifestFiles.get(0).getFileContent()).isEqualTo("abc");
    assertThat(manifestFiles.get(0).getFileName()).isEqualTo("file-1");

    assertThat(K8sTaskHelper.manifestFilesFromGitFetchFilesResult(GitFetchFilesResult.builder().build(), "")).isEmpty();
    assertThat(K8sTaskHelper.manifestFilesFromGitFetchFilesResult(
                   GitFetchFilesResult.builder().files(emptyList()).build(), ""))
        .isEmpty();
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testDoStatusCheck() throws Exception {
    KubernetesResourceId resourceId = KubernetesResourceId.builder().namespace("namespace").name("resource").build();
    Kubectl client = Kubectl.client("kubectl", "config-path");
    K8sDelegateTaskParams k8sDelegateTaskParams =
        K8sDelegateTaskParams.builder().ocPath(".").workingDirectory(".").build();

    final boolean result = spyHelperBase.doStatusCheck(client, resourceId, k8sDelegateTaskParams, executionLogCallback);

    assertThat(result).isEqualTo(false);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testDoStatusCheckKindDeployment() throws Exception {
    KubernetesResourceId resourceId =
        KubernetesResourceId.builder().namespace("namespace").kind(DeploymentConfig.name()).name("name").build();
    Kubectl client = Kubectl.client("kubectl", "config-path");
    final String workingDirectory = ".";
    K8sDelegateTaskParams k8sDelegateTaskParams = K8sDelegateTaskParams.builder()
                                                      .workingDirectory(workingDirectory)
                                                      .ocPath("oc")
                                                      .kubectlPath("kubectl")
                                                      .kubeconfigPath("config-path")
                                                      .build();

    ProcessResult processResult = new ProcessResult(0, new ProcessOutput("".getBytes()));
    doReturn(processResult).when(spyHelperBase).executeCommandUsingUtils(any(String.class), any(), any(), any());

    final String expectedCommand =
        "oc --kubeconfig=config-path rollout status DeploymentConfig/name --namespace=namespace --watch=true";
    final boolean result = spyHelperBase.doStatusCheck(client, resourceId, k8sDelegateTaskParams, executionLogCallback);

    verify(spyHelperBase).executeCommandUsingUtils(eq("."), any(), any(), eq(expectedCommand));

    assertThat(result).isEqualTo(true);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testDoStatusCheckForAllResourcesNonJobResource() throws Exception {
    KubernetesResourceId resourceId =
        KubernetesResourceId.builder().namespace("namespace").kind(DeploymentConfig.name()).name("name").build();
    KubernetesResourceId resourceId2 =
        KubernetesResourceId.builder().kind(ConfigMap.name()).name("resource").namespace("namespace").build();

    Kubectl client = Kubectl.client("kubectl", "config-path");
    final String workingDirectory = ".";
    K8sDelegateTaskParams k8sDelegateTaskParams = K8sDelegateTaskParams.builder()
                                                      .workingDirectory(workingDirectory)
                                                      .ocPath("oc")
                                                      .kubectlPath("kubectl")
                                                      .kubeconfigPath("config-path")
                                                      .build();

    ProcessResult processResult = new ProcessResult(0, new ProcessOutput("".getBytes()));
    doReturn(processResult)
        .when(spyHelperBase)
        .executeCommandUsingUtils(any(K8sDelegateTaskParams.class), any(), any(), any());

    List<KubernetesResourceId> resourceIds = new ArrayList<>();
    resourceIds.add(resourceId);
    resourceIds.add(resourceId2);
    final boolean result = spyHelperBase.doStatusCheckForAllResources(
        client, resourceIds, k8sDelegateTaskParams, "name", executionLogCallback, false);
    verify(spyHelperBase)
        .executeCommandUsingUtils(eq(k8sDelegateTaskParams), any(), any(),
            eq("oc --kubeconfig=config-path rollout status DeploymentConfig/name --namespace=namespace --watch=true"));

    assertThat(result).isEqualTo(false);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testDoStatusCheckForAllResourcesMultipleResources() throws Exception {
    KubernetesResourceId resourceId =
        KubernetesResourceId.builder().namespace("namespace").kind(DeploymentConfig.name()).name("name").build();
    KubernetesResourceId resourceId1 =
        KubernetesResourceId.builder().kind(Kind.Job.name()).name("resource").namespace("namespace").build();
    Kubectl client = Kubectl.client("kubectl", "config-path");
    final String workingDirectory = ".";
    K8sDelegateTaskParams k8sDelegateTaskParams = K8sDelegateTaskParams.builder()
                                                      .workingDirectory(workingDirectory)
                                                      .ocPath("oc")
                                                      .kubectlPath("kubectl")
                                                      .kubeconfigPath("config-path")
                                                      .build();

    ProcessResult processResult = new ProcessResult(0, new ProcessOutput("".getBytes()));
    doReturn(processResult)
        .when(spyHelperBase)
        .executeCommandUsingUtils(any(K8sDelegateTaskParams.class), any(), any(), any());

    List<KubernetesResourceId> resourceIds = new ArrayList<>();
    resourceIds.add(resourceId);
    resourceIds.add(resourceId1);
    final boolean result = spyHelperBase.doStatusCheckForAllResources(
        client, resourceIds, k8sDelegateTaskParams, "name", executionLogCallback, false);

    verify(spyHelperBase)
        .executeCommandUsingUtils(eq(k8sDelegateTaskParams), any(), any(),
            eq("oc --kubeconfig=config-path rollout status DeploymentConfig/name --namespace=namespace --watch=true"));

    assertThat(result).isEqualTo(false);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testRenderTemplateForGivenFilesLocal() throws Exception {
    fetchManifestFilesAndWriteToDirectory();
    final String workingDirectory = ".";
    K8sDelegateTaskParams k8sDelegateTaskParams = K8sDelegateTaskParams.builder()
                                                      .workingDirectory(workingDirectory)
                                                      .ocPath("oc")
                                                      .kubectlPath("kubectl")
                                                      .kubeconfigPath("config-path")
                                                      .build();

    final List<ManifestFile> manifestFiles = spyHelper.renderTemplateForGivenFiles(k8sDelegateTaskParams,
        K8sDelegateManifestConfig.builder().manifestStoreTypes(Local).build(), ".", new ArrayList<>(),
        new ArrayList<>(), "release", "namespace", executionLogCallback, K8sApplyTaskParameters.builder().build());

    assertThat(manifestFiles.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testRenderTemplateForGivenFilesGit() throws Exception {
    fetchManifestFilesAndWriteToDirectory();
    final String workingDirectory = ".";
    K8sDelegateTaskParams k8sDelegateTaskParams = K8sDelegateTaskParams.builder()
                                                      .workingDirectory(workingDirectory)
                                                      .ocPath("oc")
                                                      .kubectlPath("kubectl")
                                                      .kubeconfigPath("config-path")
                                                      .build();

    ProcessResult processResult = new ProcessResult(0, new ProcessOutput("".getBytes()));
    doReturn(processResult)
        .when(spyHelperBase)
        .executeCommandUsingUtils(any(K8sDelegateTaskParams.class), any(), any(), any());

    final List<ManifestFile> manifestFiles = spyHelper.renderTemplateForGivenFiles(k8sDelegateTaskParams,
        K8sDelegateManifestConfig.builder().manifestStoreTypes(Remote).build(), ".", new ArrayList<>(),
        new ArrayList<>(), "release", "namespace", executionLogCallback, K8sApplyTaskParameters.builder().build());

    assertThat(manifestFiles.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testRenderTemplateForGiveFilesHelmSourceRepo() throws Exception {
    final String workingDirectory = ".";
    K8sDelegateTaskParams k8sDelegateTaskParams =
        K8sDelegateTaskParams.builder().workingDirectory(workingDirectory).helmPath("helm").build();

    ProcessResult processResult = new ProcessResult(0, new ProcessOutput("".getBytes()));
    doReturn(processResult).when(spyHelperBase).executeShellCommand(any(), any(), any(), anyLong());

    final List<ManifestFile> manifestFiles = spyHelper.renderTemplateForGivenFiles(k8sDelegateTaskParams,
        K8sDelegateManifestConfig.builder().manifestStoreTypes(HelmSourceRepo).build(), ".", new ArrayList<>(),
        new ArrayList<>(), "release", "namespace", executionLogCallback, K8sApplyTaskParameters.builder().build());

    assertThat(manifestFiles.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testRenderTemplateForGivenFilesKustomizeSourceRepo() throws Exception {
    final String workingDirectory = ".";
    K8sDelegateTaskParams k8sDelegateTaskParams =
        K8sDelegateTaskParams.builder().workingDirectory(workingDirectory).helmPath("helm").build();

    when(kustomizeTaskHelper.buildForApply(any(), any(), any(), any(), any())).thenReturn(new ArrayList<>());
    final List<ManifestFile> manifestFiles = spyHelper.renderTemplateForGivenFiles(k8sDelegateTaskParams,
        K8sDelegateManifestConfig.builder().manifestStoreTypes(KustomizeSourceRepo).build(), ".", new ArrayList<>(),
        new ArrayList<>(), "release", "namespace", executionLogCallback, K8sApplyTaskParameters.builder().build());
    verify(kustomizeTaskHelper).buildForApply(any(), any(), any(), any(), any());
    assertThat(manifestFiles.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testRenderTemplateForGivenFilesOCTemplates() throws Exception {
    final String workingDirectory = ".";
    K8sDelegateTaskParams k8sDelegateTaskParams =
        K8sDelegateTaskParams.builder().workingDirectory(workingDirectory).helmPath("helm").build();

    when(openShiftDelegateService.processTemplatization(any(), any(), any(), any(), any()))
        .thenReturn(new ArrayList<>());
    final List<ManifestFile> manifestFiles = spyHelper.renderTemplate(k8sDelegateTaskParams,
        K8sDelegateManifestConfig.builder()
            .manifestStoreTypes(OC_TEMPLATES)
            .gitFileConfig(GitFileConfig.builder().build())
            .build(),
        ".", new ArrayList<>(), "release", "namespace", executionLogCallback, K8sApplyTaskParameters.builder().build());

    verify(openShiftDelegateService).processTemplatization(any(), any(), any(), any(), any());
    assertThat(manifestFiles.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testRenderTemplateForGivenFilesHelmChartRepo() throws Exception {
    final String workingDirectory = ".";
    K8sDelegateTaskParams k8sDelegateTaskParams =
        K8sDelegateTaskParams.builder().workingDirectory(workingDirectory).helmPath("helm").build();

    ProcessResult processResult = new ProcessResult(0, new ProcessOutput("".getBytes()));
    doReturn(processResult).when(spyHelperBase).executeShellCommand(any(), any(), any(), anyLong());

    final List<ManifestFile> manifestFiles = spyHelper.renderTemplateForGivenFiles(k8sDelegateTaskParams,
        K8sDelegateManifestConfig.builder()
            .helmChartConfigParams(HelmChartConfigParams.builder().chartName("chart").build())
            .manifestStoreTypes(HelmChartRepo)
            .build(),
        ".", new ArrayList<>(), new ArrayList<>(), "release", "namespace", executionLogCallback,
        K8sApplyTaskParameters.builder().build());

    assertThat(manifestFiles.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testReadManifestAndOverrideLocalSecrets() throws Exception {
    fetchManifestFilesAndWriteToDirectory();
    final String workingDirectory = ".";
    K8sDelegateTaskParams k8sDelegateTaskParams = K8sDelegateTaskParams.builder()
                                                      .workingDirectory(workingDirectory)
                                                      .ocPath("oc")
                                                      .kubectlPath("kubectl")
                                                      .kubeconfigPath("config-path")
                                                      .build();

    ProcessResult processResult = new ProcessResult(0, new ProcessOutput("".getBytes()));
    doReturn(processResult)
        .when(spyHelperBase)
        .executeCommandUsingUtils(any(K8sDelegateTaskParams.class), any(), any(), any());

    final List<ManifestFile> manifestFiles = prepareSomeCorrectManifestFiles();

    final List<KubernetesResource> resources =
        spyHelper.readManifestAndOverrideLocalSecrets(manifestFiles, executionLogCallback, true);

    assertThat(resources.stream()
                   .map(KubernetesResource::getResourceId)
                   .map(KubernetesResourceId::getKind)
                   .collect(Collectors.toList()))
        .isEqualTo(Lists.newArrayList(ConfigMap.name(), Deployment.name(), DeploymentConfig.name()));
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testReadManifestAndOverrideLocalSecretsOverrideLocalSecrets() throws Exception {
    fetchManifestFilesAndWriteToDirectory();

    ProcessResult processResult = new ProcessResult(0, new ProcessOutput("".getBytes()));
    doReturn(processResult)
        .when(spyHelperBase)
        .executeCommandUsingUtils(any(K8sDelegateTaskParams.class), any(), any(), any());

    final List<ManifestFile> manifestFiles = prepareSomeCorrectManifestFiles();
    final List<KubernetesResource> resources =
        spyHelper.readManifestAndOverrideLocalSecrets(manifestFiles, executionLogCallback, false);

    assertThat(resources.size()).isEqualTo(3);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testK8sTaskExecutionResponse() throws Exception {
    final K8sTaskResponse k8sTaskResponse = K8sApplyResponse.builder().build();
    final K8sTaskExecutionResponse executionResponse =
        spyHelper.getK8sTaskExecutionResponse(k8sTaskResponse, CommandExecutionStatus.FAILURE);

    assertThat(executionResponse.getK8sTaskResponse()).isEqualTo(k8sTaskResponse);
    assertThat(executionResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetManifestFileNamesInLogFormat() throws Exception {
    final String result = spyHelperBase.getManifestFileNamesInLogFormat(".");

    assertThat(result).isNotBlank();
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testRenderTemplateHelmSourceRepo() throws Exception {
    final String workingDirectory = ".";
    K8sDelegateTaskParams k8sDelegateTaskParams =
        K8sDelegateTaskParams.builder().workingDirectory(workingDirectory).helmPath("helm").build();

    ProcessResult processResult = new ProcessResult(0, new ProcessOutput("".getBytes()));
    doReturn(processResult).when(mockK8sTaskHelperBase).executeShellCommand(any(), any(), any(), anyLong());

    final List<ManifestFile> manifestFiles = helper.renderTemplate(k8sDelegateTaskParams,
        K8sDelegateManifestConfig.builder().manifestStoreTypes(HelmSourceRepo).build(), ".", new ArrayList<>(),
        "release", "namespace", executionLogCallback, K8sApplyTaskParameters.builder().build());

    assertThat(manifestFiles.size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testRenderTemplateKustomizeSourceRepo() throws Exception {
    final String workingDirectory = ".";
    K8sDelegateTaskParams k8sDelegateTaskParams =
        K8sDelegateTaskParams.builder().workingDirectory(workingDirectory).helmPath("helm").build();

    when(kustomizeTaskHelper.build(any(), any(), any(), any())).thenReturn(new ArrayList<>());
    final List<ManifestFile> manifestFiles = spyHelper.renderTemplate(k8sDelegateTaskParams,
        K8sDelegateManifestConfig.builder().manifestStoreTypes(KustomizeSourceRepo).build(), ".", new ArrayList<>(),
        "release", "namespace", executionLogCallback, K8sApplyTaskParameters.builder().build());
    verify(kustomizeTaskHelper).build(any(), any(), any(), any());
    assertThat(manifestFiles.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testRenderTemplateOCTemplates() throws Exception {
    final String workingDirectory = ".";
    K8sDelegateTaskParams k8sDelegateTaskParams =
        K8sDelegateTaskParams.builder().workingDirectory(workingDirectory).helmPath("helm").build();

    when(openShiftDelegateService.processTemplatization(any(), any(), any(), any(), any()))
        .thenReturn(new ArrayList<>());
    final List<ManifestFile> manifestFiles = spyHelper.renderTemplate(k8sDelegateTaskParams,
        K8sDelegateManifestConfig.builder()
            .manifestStoreTypes(OC_TEMPLATES)
            .gitFileConfig(GitFileConfig.builder().build())
            .build(),
        ".", new ArrayList<>(), "release", "namespace", executionLogCallback, K8sApplyTaskParameters.builder().build());

    verify(openShiftDelegateService).processTemplatization(any(), any(), any(), any(), any());
    assertThat(manifestFiles.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testRenderTemplateHelmChartRepo() throws Exception {
    final String workingDirectory = ".";
    K8sDelegateTaskParams k8sDelegateTaskParams =
        K8sDelegateTaskParams.builder().workingDirectory(workingDirectory).helmPath("helm").build();

    ProcessResult processResult = new ProcessResult(0, new ProcessOutput("".getBytes()));
    doReturn(processResult).when(mockK8sTaskHelperBase).executeShellCommand(any(), any(), any(), anyLong());

    final List<ManifestFile> manifestFiles = helper.renderTemplate(k8sDelegateTaskParams,
        K8sDelegateManifestConfig.builder()
            .helmChartConfigParams(HelmChartConfigParams.builder().chartName("chart").build())
            .manifestStoreTypes(HelmChartRepo)
            .build(),
        ".", new ArrayList<>(), "release", "namespace", executionLogCallback, K8sApplyTaskParameters.builder().build());

    assertThat(manifestFiles.size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetExecutionLogOutputStream() throws Exception {
    LogOutputStream logOutputStream = K8sTaskHelperBase.getExecutionLogOutputStream(executionLogCallback, INFO);

    assertThat(logOutputStream).isInstanceOf(LogOutputStream.class);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testRunK8sExecutable() throws Exception {
    KubernetesResourceId resourceId =
        KubernetesResourceId.builder().namespace("namespace").kind(DeploymentConfig.name()).name("name").build();
    KubernetesResourceId resourceId1 =
        KubernetesResourceId.builder().kind(Kind.Job.name()).name("resource").namespace("namespace").build();
    Kubectl client = Kubectl.client("kubectl", "config-path");
    final String workingDirectory = ".";
    K8sDelegateTaskParams k8sDelegateTaskParams = K8sDelegateTaskParams.builder()
                                                      .workingDirectory(workingDirectory)
                                                      .ocPath("oc")
                                                      .kubectlPath("kubectl")
                                                      .kubeconfigPath("config-path")
                                                      .build();

    List<KubernetesResourceId> resourceIds = new ArrayList<>();
    resourceIds.add(resourceId);
    resourceIds.add(resourceId1);
    ProcessResult result =
        spyHelperBase.runK8sExecutable(k8sDelegateTaskParams, executionLogCallback, new ApplyCommand(client));

    assertThat(result.getExitValue()).isEqualTo(1);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testRunK8sExecutableSilent() throws Exception {
    Kubectl client = Kubectl.client("kubectl", "config-path");
    final String workingDirectory = ".";
    K8sDelegateTaskParams k8sDelegateTaskParams = K8sDelegateTaskParams.builder()
                                                      .workingDirectory(workingDirectory)
                                                      .ocPath("oc")
                                                      .kubectlPath("kubectl")
                                                      .kubeconfigPath("config-path")
                                                      .build();

    ProcessResult result = spyHelperBase.runK8sExecutableSilent(k8sDelegateTaskParams, new ApplyCommand(client));
    assertThat(result.getExitValue()).isEqualTo(1);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testDoStatusCheckForAllResourcesEmptyResourceIds() throws Exception {
    Kubectl client = Kubectl.client("kubectl", "config-path");
    final String workingDirectory = ".";
    K8sDelegateTaskParams k8sDelegateTaskParams = K8sDelegateTaskParams.builder()
                                                      .workingDirectory(workingDirectory)
                                                      .ocPath("oc")
                                                      .kubectlPath("kubectl")
                                                      .kubeconfigPath("config-path")
                                                      .build();

    List<KubernetesResourceId> resourceIds = new ArrayList<>();
    final boolean result = spyHelperBase.doStatusCheckForAllResources(
        client, resourceIds, k8sDelegateTaskParams, "name", executionLogCallback, false);

    assertThat(result).isEqualTo(true);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetOcCommandPrefix() {
    K8sDelegateTaskParams k8sDelegateTaskParams = K8sDelegateTaskParams.builder()
                                                      .workingDirectory(".")
                                                      .kubectlPath("kubectl")
                                                      .kubeconfigPath("config-path")
                                                      .build();

    final String result = spyHelperBase.getOcCommandPrefix(k8sDelegateTaskParams);

    assertThat(result).isEqualTo("oc --kubeconfig=config-path");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void helmChartInfoFromDelegateManifestConfig() throws Exception {
    String workingDirectory = "working/directory";
    ArgumentCaptor<String> checkHelmChartDirCaptor = ArgumentCaptor.forClass(String.class);
    HelmChartInfo existingHelmChartInfo = HelmChartInfo.builder().name("chart").version("1.0.0").build();

    // When Store Type is HelmSourceRepo
    doReturn(existingHelmChartInfo).when(mockHelmTaskHelper).getHelmChartInfoFromChartDirectory(anyString());
    K8sDelegateManifestConfig delegateManifestConfig = K8sDelegateManifestConfig.builder()
                                                           .manifestStoreTypes(HelmSourceRepo)
                                                           .gitConfig(GitConfig.builder().branch("master").build())
                                                           .build();
    HelmChartInfo helmChartInfo = helper.getHelmChartDetails(delegateManifestConfig, workingDirectory);
    verify(mockHelmTaskHelper, times(1)).getHelmChartInfoFromChartDirectory(checkHelmChartDirCaptor.capture());
    assertThat(checkHelmChartDirCaptor.getValue()).isEqualTo(workingDirectory);
    assertHelmChartInfo(helmChartInfo);

    // When Store Type is HelmChartRepo
    reset(mockHelmTaskHelper);
    doReturn(existingHelmChartInfo).when(mockHelmTaskHelper).getHelmChartInfoFromChartDirectory(anyString());
    delegateManifestConfig.setManifestStoreTypes(HelmChartRepo);
    delegateManifestConfig.setHelmChartConfigParams(HelmChartConfigParams.builder().chartName("chart").build());
    delegateManifestConfig.setGitConfig(null);
    helmChartInfo = helper.getHelmChartDetails(delegateManifestConfig, workingDirectory);
    verify(mockHelmTaskHelper, times(1)).getHelmChartInfoFromChartDirectory(checkHelmChartDirCaptor.capture());
    assertThat(checkHelmChartDirCaptor.getValue()).isEqualTo(Paths.get(workingDirectory, "chart").toString());
    assertHelmChartInfo(helmChartInfo);

    // When Store Type is other than Helm type
    reset(mockHelmTaskHelper);
    delegateManifestConfig.setManifestStoreTypes(Remote);
    helmChartInfo = helper.getHelmChartDetails(delegateManifestConfig, workingDirectory);
    verify(mockHelmTaskHelper, never()).getHelmChartInfoFromChartDirectory(anyString());
    assertThat(helmChartInfo).isNull();
  }

  private void assertHelmChartInfo(HelmChartInfo helmChartInfo) {
    assertThat(helmChartInfo.getName()).isEqualTo("chart");
    assertThat(helmChartInfo.getVersion()).isEqualTo("1.0.0");
  }
}