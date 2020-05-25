package software.wings.delegatetasks.k8s;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.filesystem.FileIo.createDirectoryIfDoesNotExist;
import static io.harness.k8s.manifest.ManifestHelper.processYaml;
import static io.harness.k8s.model.Kind.ConfigMap;
import static io.harness.k8s.model.Kind.Deployment;
import static io.harness.k8s.model.Kind.Namespace;
import static io.harness.k8s.model.Kind.ReplicaSet;
import static io.harness.k8s.model.Kind.Secret;
import static io.harness.k8s.model.Kind.Service;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.SATYAM;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static io.harness.rule.OwnerRule.YOGESH;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static software.wings.beans.appmanifest.StoreType.HelmSourceRepo;
import static software.wings.beans.appmanifest.StoreType.KustomizeSourceRepo;
import static software.wings.beans.appmanifest.StoreType.Local;
import static software.wings.beans.appmanifest.StoreType.OC_TEMPLATES;
import static software.wings.beans.appmanifest.StoreType.Remote;
import static software.wings.beans.yaml.YamlConstants.PATH_DELIMITER;
import static software.wings.delegatetasks.k8s.K8sTestConstants.DAEMON_SET_YAML;
import static software.wings.delegatetasks.k8s.K8sTestConstants.DEPLOYMENT_YAML;
import static software.wings.delegatetasks.k8s.K8sTestConstants.STATEFUL_SET_YAML;
import static software.wings.utils.KubernetesConvention.ReleaseHistoryKeyName;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.harness.category.element.UnitTests;
import io.harness.exception.KubernetesYamlException;
import io.harness.filesystem.FileIo;
import io.harness.k8s.kubectl.AbstractExecutable;
import io.harness.k8s.kubectl.ApplyCommand;
import io.harness.k8s.kubectl.DeleteCommand;
import io.harness.k8s.kubectl.DescribeCommand;
import io.harness.k8s.kubectl.GetJobCommand;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.kubectl.RolloutHistoryCommand;
import io.harness.k8s.kubectl.ScaleCommand;
import io.harness.k8s.kubectl.Utils;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.k8s.model.Release;
import io.harness.k8s.model.Release.Status;
import io.harness.k8s.model.ReleaseHistory;
import io.harness.rule.Owner;
import org.apache.commons.io.FileUtils;
import org.apache.sshd.common.file.util.MockPath;
import org.assertj.core.api.Assertions;
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
import org.zeroturnaround.exec.StartedProcess;
import software.wings.WingsBaseTest;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFileConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.yaml.GitFetchFilesResult;
import software.wings.beans.yaml.GitFile;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.delegatetasks.helm.HelmTaskHelper;
import software.wings.helpers.ext.helm.HelmConstants.HelmVersion;
import software.wings.helpers.ext.helm.request.HelmChartConfigParams;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig;
import software.wings.helpers.ext.k8s.request.K8sDeleteTaskParameters;
import software.wings.service.impl.KubernetesHelperService;
import software.wings.service.intfc.GitService;
import software.wings.service.intfc.security.EncryptionService;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Utils.class, K8sTaskHelper.class})
@PowerMockIgnore({"javax.security.*", "javax.net.*"})
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

  private String resourcePath = "./k8s";
  private String deploymentYaml = "deployment.yaml";
  private String deploymentConfigYaml = "deployment-config.yaml";
  private String configMapYaml = "configMap.yaml";

  @Inject @InjectMocks private K8sTaskHelper helper;

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testGetTargetInstancesForCanary() {
    ExecutionLogCallback mockLogCallback = logCallback;
    doNothing().when(mockLogCallback).saveExecutionLog(anyString());
    assertThat(helper.getTargetInstancesForCanary(50, 4, mockLogCallback)).isEqualTo(2);
    assertThat(helper.getTargetInstancesForCanary(5, 2, mockLogCallback)).isEqualTo(1);
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

    String resourcesInTableFormat = helper.getResourcesInTableFormat(kubernetesResources);

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
    doReturn(configMap).when(mockKubernetesContainerService).getConfigMap(any(), anyList(), anyString());

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
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGetResourceIdsForDeletion() throws Exception {
    K8sDeleteTaskParameters k8sDeleteTaskParameters =
        K8sDeleteTaskParameters.builder().releaseName("releaseName").build();

    ExecutionLogCallback executionLogCallback = logCallback;
    doNothing().when(executionLogCallback).saveExecutionLog(anyString());

    ConfigMap configMap = new ConfigMap();
    configMap.setKind(ConfigMap.name());
    doReturn(configMap).when(mockKubernetesContainerService).getConfigMap(any(), anyList(), anyString());
    List<KubernetesResourceId> kubernetesResourceIdList = getKubernetesResourceIdList();
    ReleaseHistory releaseHistory = ReleaseHistory.createNew();
    releaseHistory.setReleases(
        Arrays.asList(Release.builder().status(Status.Succeeded).resources(kubernetesResourceIdList).build()));

    String releaseHistoryString = releaseHistory.getAsYaml();
    Map<String, String> data = new HashMap<>();
    data.put(ReleaseHistoryKeyName, releaseHistoryString);
    configMap.setData(data);
    kubernetesResourceIdList = helper.getResourceIdsForDeletion(
        k8sDeleteTaskParameters, KubernetesConfig.builder().namespace("default").build(), executionLogCallback);

    assertThat(kubernetesResourceIdList.size()).isEqualTo(4);
    assertThat(kubernetesResourceIdList.get(0).getKind()).isEqualTo(Deployment.name());
    assertThat(kubernetesResourceIdList.get(1).getKind()).isEqualTo(Service.name());
    assertThat(kubernetesResourceIdList.get(2).getKind()).isEqualTo(ConfigMap.name());
    assertThat(kubernetesResourceIdList.get(3).getKind()).isEqualTo(ConfigMap.name());

    k8sDeleteTaskParameters.setDeleteNamespacesForRelease(true);
    kubernetesResourceIdList = helper.getResourceIdsForDeletion(
        k8sDeleteTaskParameters, KubernetesConfig.builder().build(), executionLogCallback);
    assertThat(kubernetesResourceIdList.size()).isEqualTo(5);
    assertThat(kubernetesResourceIdList.get(0).getKind()).isEqualTo(Deployment.name());
    assertThat(kubernetesResourceIdList.get(1).getKind()).isEqualTo(Service.name());
    assertThat(kubernetesResourceIdList.get(2).getKind()).isEqualTo(ConfigMap.name());
    assertThat(kubernetesResourceIdList.get(3).getKind()).isEqualTo(ConfigMap.name());
    assertThat(kubernetesResourceIdList.get(4).getKind()).isEqualTo(Namespace.name());
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

    kubernetesResourceIdList = helper.arrangeResourceIdsInDeletionOrder(kubernetesResourceIdList);
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
    Assertions.assertThat(command).doesNotContain("$").doesNotContain("{").doesNotContain("}");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGetHelmV2CommandForRenderOneChartFile() {
    String command = helper.getHelmCommandForRender("helm", "chart_location", "test-release", "default",
        " -f values-0.yaml", "template/service.yaml", HelmVersion.V2);
    Assertions.assertThat(command).doesNotContain("$").doesNotContain("{").doesNotContain("}");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGetHelmV3CommandForRender() {
    String command = helper.getHelmCommandForRender(
        "helm", "chart_location", "test-release", "default", " -f values-0.yaml", HelmVersion.V3);
    Assertions.assertThat(command).doesNotContain("$").doesNotContain("{").doesNotContain("}");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGetHelmV3CommandForRenderOneChartFile() {
    String command = helper.getHelmCommandForRender("helm", "chart_location", "test-release", "default",
        " -f values-0.yaml", "template/service.yaml", HelmVersion.V3);
    Assertions.assertThat(command).doesNotContain("$").doesNotContain("{").doesNotContain("}");
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

    String latestRevision = helper.getLatestRevision(client, resource.getResourceId(), k8sDelegateTaskParams);
    assertThat(latestRevision).isEqualTo("36");

    PowerMockito.mockStatic(Utils.class);
    processResult = new ProcessResult(1, new ProcessOutput("".getBytes()));
    when(Utils.executeScript(anyString(), anyString(), any(), any())).thenReturn(processResult);

    latestRevision = helper.getLatestRevision(client, resource.getResourceId(), k8sDelegateTaskParams);
    assertThat(latestRevision).isEqualTo("");
  }

  private void setupForDoStatusCheckForAllResources() throws Exception {
    PowerMockito.mockStatic(Utils.class);
    ProcessResult processResult = new ProcessResult(0, new ProcessOutput("".getBytes()));
    when(Utils.executeScript(anyString(), anyString(), any(), any())).thenReturn(processResult);

    when(Utils.encloseWithQuotesIfNeeded("kubectl")).thenReturn("kubectl");
    when(Utils.encloseWithQuotesIfNeeded("oc")).thenReturn("oc");
    when(Utils.encloseWithQuotesIfNeeded("config-path")).thenReturn("config-path");

    when(process.destroyForcibly()).thenReturn(process);
    when(Utils.startScript(any(), any(), any(), any())).thenReturn(startedProcess);
    when(startedProcess.getProcess()).thenReturn(process);
  }

  private void doStatusCheck(String manifestFilePath, String expectedOutput, boolean allResources) throws Exception {
    URL url = this.getClass().getResource(manifestFilePath);
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    KubernetesResource resource = processYaml(fileContents).get(0);

    K8sDelegateTaskParams k8sDelegateTaskParams =
        K8sDelegateTaskParams.builder().kubectlPath("kubectl").ocPath("oc").kubeconfigPath("config-path").build();
    Kubectl client = Kubectl.client("kubectl", "config-path");

    if (allResources) {
      helper.doStatusCheckForAllResources(
          client, asList(resource.getResourceId()), k8sDelegateTaskParams, "default", executionLogCallback);
    } else {
      helper.doStatusCheck(client, resource.getResourceId(), k8sDelegateTaskParams, executionLogCallback);
    }

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    PowerMockito.verifyStatic(Utils.class);
    Utils.executeScript(any(), captor.capture(), any(), any());
    assertThat(captor.getValue()).isEqualTo(expectedOutput);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testDoStatusCheckForAllResourcesForDC() throws Exception {
    setupForDoStatusCheckForAllResources();

    doStatusCheck("/k8s/deployment-config.yaml",
        "oc --kubeconfig=config-path rollout status DeploymentConfig/test-dc --namespace=default --watch=true", true);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testDoStatusCheckForAllResourcesForDeployment() throws Exception {
    setupForDoStatusCheckForAllResources();

    doStatusCheck("/k8s/deployment.yaml",
        "kubectl --kubeconfig=config-path rollout status Deployment/nginx-deployment --watch=true", true);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testDoStatusCheckForDC() throws Exception {
    setupForDoStatusCheckForAllResources();

    doStatusCheck("/k8s/deployment-config.yaml",
        "oc --kubeconfig=config-path rollout status DeploymentConfig/test-dc --namespace=default --watch=true", false);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testDoStatusCheckForDeployment() throws Exception {
    setupForDoStatusCheckForAllResources();

    doStatusCheck("/k8s/deployment.yaml",
        "kubectl --kubeconfig=config-path rollout status Deployment/nginx-deployment --watch=true", false);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGenerateTruncatedFileListForLogging() throws Exception {
    mockStatic(Files.class);
    when(Files.isRegularFile(any(Path.class))).thenReturn(true);
    MockPath basePath = new MockPath("foo");
    String loggedFiles;

    loggedFiles = helper.generateTruncatedFileListForLogging(basePath, getNFilePathsWithSuffix(0, "file").stream());
    assertThat(loggedFiles).isEmpty();
    assertThat(loggedFiles).doesNotContain("..more");

    loggedFiles = helper.generateTruncatedFileListForLogging(basePath, getNFilePathsWithSuffix(1, "file").stream());
    assertThat(loggedFiles).isNotEmpty();
    assertThat(loggedFiles).doesNotContain("..more");

    loggedFiles = helper.generateTruncatedFileListForLogging(basePath, getNFilePathsWithSuffix(100, "file").stream());
    assertThat(loggedFiles).isNotEmpty();
    assertThat(loggedFiles).doesNotContain("..more");

    loggedFiles = helper.generateTruncatedFileListForLogging(basePath, getNFilePathsWithSuffix(101, "file").stream());
    assertThat(loggedFiles).isNotEmpty();
    assertThat(loggedFiles).contains("..1 more");

    loggedFiles = helper.generateTruncatedFileListForLogging(basePath, getNFilePathsWithSuffix(199, "file").stream());
    assertThat(loggedFiles).isNotEmpty();
    assertThat(loggedFiles).contains("..99 more");
  }

  private List<Path> getNFilePathsWithSuffix(int n, String suffix) {
    List<Path> paths = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      Path mockPath = mock(Path.class);
      paths.add(mockPath);
      when(mockPath.relativize(any(Path.class))).thenAnswer(invocationOnMock -> new MockPath("foo"));
    }
    return paths;
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testDryRunForOpenshiftResources() throws Exception {
    mockStatic(K8sTaskHelper.class);
    ProcessResult processResult = new ProcessResult(0, new ProcessOutput("abc".getBytes()));
    when(K8sTaskHelper.executeCommand(any(AbstractExecutable.class), anyString(), any(ExecutionLogCallback.class)))
        .thenReturn(processResult);

    K8sDelegateTaskParams k8sDelegateTaskParams = K8sDelegateTaskParams.builder()
                                                      .workingDirectory("/tmp/test")
                                                      .ocPath("oc")
                                                      .kubectlPath("kubectl")
                                                      .kubeconfigPath("config-path")
                                                      .build();
    createDirectoryIfDoesNotExist(Paths.get("/tmp/test").toString());
    Kubectl client = Kubectl.client("kubectl", "config-path");

    helper.dryRunManifests(client, emptyList(), k8sDelegateTaskParams, executionLogCallback);

    ArgumentCaptor<ApplyCommand> captor = ArgumentCaptor.forClass(ApplyCommand.class);
    PowerMockito.verifyStatic(K8sTaskHelper.class);
    K8sTaskHelper.executeCommand(captor.capture(), anyString(), any());
    assertThat(captor.getValue().command())
        .isEqualTo("kubectl --kubeconfig=config-path apply --filename=manifests-dry-run.yaml --dry-run");

    helper.dryRunManifests(client,
        asList(KubernetesResource.builder()
                   .spec("")
                   .resourceId(KubernetesResourceId.builder().kind("Route").build())
                   .build()),
        k8sDelegateTaskParams, executionLogCallback);
    PowerMockito.verifyStatic(K8sTaskHelper.class, times(2));
    K8sTaskHelper.executeCommand(captor.capture(), anyString(), any());
    assertThat(captor.getValue().command())
        .isEqualTo("oc --kubeconfig=config-path apply --filename=manifests-dry-run.yaml --dry-run");
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testApplyForOpenshiftResources() throws Exception {
    mockStatic(K8sTaskHelper.class);
    ProcessResult processResult = new ProcessResult(0, new ProcessOutput("abc".getBytes()));
    when(K8sTaskHelper.executeCommand(any(AbstractExecutable.class), anyString(), any(ExecutionLogCallback.class)))
        .thenReturn(processResult);

    K8sDelegateTaskParams k8sDelegateTaskParams = K8sDelegateTaskParams.builder()
                                                      .workingDirectory("/tmp/test")
                                                      .kubectlPath("kubectl")
                                                      .ocPath("oc")
                                                      .kubeconfigPath("config-path")
                                                      .build();
    createDirectoryIfDoesNotExist(Paths.get("/tmp/test").toString());
    Kubectl client = Kubectl.client("kubectl", "config-path");

    helper.applyManifests(client, emptyList(), k8sDelegateTaskParams, executionLogCallback);

    ArgumentCaptor<ApplyCommand> captor = ArgumentCaptor.forClass(ApplyCommand.class);
    PowerMockito.verifyStatic(K8sTaskHelper.class);
    K8sTaskHelper.executeCommand(captor.capture(), anyString(), any());
    assertThat(captor.getValue().command())
        .isEqualTo("kubectl --kubeconfig=config-path apply --filename=manifests.yaml --record");

    helper.applyManifests(client,
        asList(KubernetesResource.builder()
                   .spec("")
                   .resourceId(KubernetesResourceId.builder().kind("Route").build())
                   .build()),
        k8sDelegateTaskParams, executionLogCallback);
    PowerMockito.verifyStatic(K8sTaskHelper.class, times(2));
    K8sTaskHelper.executeCommand(captor.capture(), anyString(), any());
    assertThat(captor.getValue().command())
        .isEqualTo("oc --kubeconfig=config-path apply --filename=manifests.yaml --record");
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

    assertThat(helper.getJobStatus(
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

    assertThat(helper.getJobStatus(k8sDelegateTaskParams, null, null, jobCompletionStatus, null, jobStatusCommand,
                   jobCompletionCommand))
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

    assertThat(helper.getJobStatus(
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

    assertThat(helper.getJobStatus(k8sDelegateTaskParams, null, null, jobCompletionStatus, null, jobStatusCommand,
                   jobCompletionCommand))
        .isTrue();
  }

  private void shouldReturnFalseWhenCompletedJobCommandFailed(
      String RANDOM, K8sDelegateTaskParams k8sDelegateTaskParams, GetJobCommand jobStatusCommand) throws Exception {
    GetJobCommand jobCompletionStatus = spy(new GetJobCommand(null, null, null));
    ProcessResult jobStatusResult = new ProcessResult(1, new ProcessOutput("FAILURE".getBytes()));

    doReturn(jobStatusResult).when(jobCompletionStatus).execute(RANDOM, null, null, false);

    assertThat(
        helper.getJobStatus(k8sDelegateTaskParams, null, null, jobCompletionStatus, null, jobStatusCommand, null))
        .isFalse();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void scale() throws Exception {
    scaleSuccess();
    scaleFailure();
  }

  private void scaleFailure() throws Exception {
    Kubectl kubectl = Kubectl.client("kubectl", "config-path");
    PowerMockito.mockStatic(K8sTaskHelper.class);
    when(K8sTaskHelper.executeCommand(any(), anyString(), any())).thenReturn(buildProcessResult(1));
    final boolean success = helper.scale(kubectl, K8sDelegateTaskParams.builder().build(),
        KubernetesResourceId.builder().name("nginx").kind("Deployment").namespace("default").build(), 5, logCallback);
    assertThat(success).isFalse();
  }

  private void scaleSuccess() throws Exception {
    Kubectl kubectl = Kubectl.client("kubectl", "config-path");
    PowerMockito.mockStatic(K8sTaskHelper.class);
    when(K8sTaskHelper.executeCommand(any(), anyString(), any())).thenReturn(buildProcessResult(0));
    final boolean success = helper.scale(kubectl, K8sDelegateTaskParams.builder().build(),
        KubernetesResourceId.builder().name("nginx").kind("Deployment").namespace("default").build(), 5, logCallback);

    assertThat(success).isTrue();
    ArgumentCaptor<ScaleCommand> captor = ArgumentCaptor.forClass(ScaleCommand.class);

    verifyStatic(K8sTaskHelper.class);
    K8sTaskHelper.executeCommand(captor.capture(), any(), any());

    assertThat(captor.getValue().command())
        .isEqualTo("kubectl --kubeconfig=config-path scale Deployment/nginx --namespace=default --replicas=5");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void delete() throws Exception {
    final ReleaseHistory releaseHistory = ReleaseHistory.createNew();
    releaseHistory.getReleases().add(buildRelease(Status.Succeeded, 0));
    PowerMockito.mockStatic(K8sTaskHelper.class);
    when(K8sTaskHelper.executeCommand(any(), anyString(), any())).thenReturn(buildProcessResult(0));
    helper.delete(Kubectl.client("kubectl", "kubeconfig"), K8sDelegateTaskParams.builder().build(),
        asList(configMap().getResourceId()), logCallback);
    PowerMockito.verifyStatic(K8sTaskHelper.class, times(1));
    ArgumentCaptor<DeleteCommand> captor = ArgumentCaptor.forClass(DeleteCommand.class);
    K8sTaskHelper.executeCommand(captor.capture(), anyString(), any(ExecutionLogCallback.class));
    final List<DeleteCommand> deleteCommands = captor.getAllValues();
    assertThat(deleteCommands).hasSize(1);
    assertThat(deleteCommands.get(0).command())
        .isEqualTo("kubectl --kubeconfig=kubeconfig delete configMap/config-map --namespace=default");
  }

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
    releaseHistory.getReleases().add(buildRelease(Status.Succeeded, 3));
    releaseHistory.getReleases().add(buildRelease(Status.Succeeded, 2));
    releaseHistory.getReleases().add(buildRelease(Status.Succeeded, 1));
    releaseHistory.getReleases().add(buildRelease(Status.Succeeded, 0));
    PowerMockito.mockStatic(K8sTaskHelper.class);
    when(K8sTaskHelper.executeCommand(any(), anyString(), any())).thenReturn(buildProcessResult(0));
    helper.cleanup(
        Kubectl.client("kubectl", "kubeconfig"), K8sDelegateTaskParams.builder().build(), releaseHistory, logCallback);
    PowerMockito.verifyStatic(K8sTaskHelper.class, times(3));
    ArgumentCaptor<DeleteCommand> captor = ArgumentCaptor.forClass(DeleteCommand.class);
    K8sTaskHelper.executeCommand(captor.capture(), anyString(), any(ExecutionLogCallback.class));
    final List<DeleteCommand> deleteCommands = captor.getAllValues();
    assertThat(releaseHistory.getReleases()).hasSize(1);
    assertThat(deleteCommands.get(0).command())
        .isEqualTo("kubectl --kubeconfig=kubeconfig delete configMap/config-map --namespace=default");
  }

  private void cleanUpIfMultipleFailedReleases() throws Exception {
    final ReleaseHistory releaseHistory = ReleaseHistory.createNew();
    releaseHistory.getReleases().add(buildRelease(Status.Failed, 3));
    releaseHistory.getReleases().add(buildRelease(Status.Failed, 2));
    releaseHistory.getReleases().add(buildRelease(Status.Succeeded, 1));
    releaseHistory.getReleases().add(buildRelease(Status.Failed, 0));
    PowerMockito.mockStatic(K8sTaskHelper.class);
    when(K8sTaskHelper.executeCommand(any(), anyString(), any())).thenReturn(buildProcessResult(0));
    helper.cleanup(
        Kubectl.client("kubectl", "kubeconfig"), K8sDelegateTaskParams.builder().build(), releaseHistory, logCallback);
    PowerMockito.verifyStatic(K8sTaskHelper.class, times(3));
    ArgumentCaptor<DeleteCommand> captor = ArgumentCaptor.forClass(DeleteCommand.class);
    K8sTaskHelper.executeCommand(captor.capture(), anyString(), any(ExecutionLogCallback.class));
    final List<DeleteCommand> deleteCommands = captor.getAllValues();
    assertThat(releaseHistory.getReleases()).hasSize(1);
    assertThat(deleteCommands.get(0).command())
        .isEqualTo("kubectl --kubeconfig=kubeconfig delete configMap/config-map --namespace=default");
  }

  private Release buildRelease(Status status, int number) throws IOException {
    return Release.builder()
        .number(number)
        .resources(asList(deployment().getResourceId(), configMap().getResourceId()))
        .status(status)
        .build();
  }

  private void cleanUpIfOnly1FailedRelease() throws Exception {
    final ReleaseHistory releaseHistory = ReleaseHistory.createNew();
    releaseHistory.getReleases().add(
        Release.builder().number(0).resources(asList(deployment().getResourceId())).status(Status.Failed).build());
    helper.cleanup(mock(Kubectl.class), K8sDelegateTaskParams.builder().build(), releaseHistory, logCallback);
    assertThat(releaseHistory.getReleases()).isEmpty();
  }

  private ProcessResult buildProcessResult(int exitCode) {
    return new ProcessResult(exitCode, new ProcessOutput(null));
  }

  private ProcessResult buildProcessResult(int exitCode, String output) {
    return new ProcessResult(exitCode, new ProcessOutput(output.getBytes()));
  }

  private KubernetesResource deployment() throws IOException {
    String yamlString = readFileContent(deploymentYaml);
    return KubernetesResource.builder()
        .spec(yamlString)
        .resourceId(
            KubernetesResourceId.builder().namespace("default").kind("Deployment").name("nginx-deployment").build())
        .build();
  }

  private KubernetesResource deploymentConfig() throws IOException {
    String yamlString = readFileContent(deploymentConfigYaml);
    return KubernetesResource.builder()
        .spec(yamlString)
        .resourceId(
            KubernetesResourceId.builder().namespace("default").kind("DeploymentConfig").name("test-dc").build())
        .build();
  }

  private KubernetesResource configMap() throws IOException {
    String yamlString = readFileContent(configMapYaml);
    return KubernetesResource.builder()
        .spec(yamlString)
        .resourceId(KubernetesResourceId.builder()
                        .namespace("default")
                        .kind("configMap")
                        .name("config-map")
                        .versioned(true)
                        .build())
        .build();
  }

  private String readFileContent(String filePath) throws IOException {
    File yamlFile = null;
    try {
      yamlFile = new File(getClass().getClassLoader().getResource(resourcePath + PATH_DELIMITER + filePath).toURI());
    } catch (URISyntaxException e) {
      Assertions.fail("Unable to find yaml file " + filePath);
    }
    return FileUtils.readFileToString(yamlFile, "UTF-8");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void getCurrentReplicas() throws Exception {
    PowerMockito.mockStatic(K8sTaskHelper.class);
    when(K8sTaskHelper.executeCommandSilent(any(), anyString()))
        .thenReturn(buildProcessResult(0, "3"))
        .thenReturn(buildProcessResult(1));
    assertThat(helper.getCurrentReplicas(Kubectl.client("kubectl", "kubeconfig"), deployment().getResourceId(),
                   K8sDelegateTaskParams.builder().build()))
        .isEqualTo(3);

    assertThat(helper.getCurrentReplicas(Kubectl.client("kubectl", "kubeconfig"), deployment().getResourceId(),
                   K8sDelegateTaskParams.builder().build()))
        .isNull();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void getLatestRevisionForDeploymentConfig() throws Exception {
    PowerMockito.mockStatic(Utils.class);
    PowerMockito.when(Utils.encloseWithQuotesIfNeeded("oc")).thenReturn("oc");
    PowerMockito.when(Utils.executeScript(anyString(), anyString(), any(), any()))
        .thenReturn(buildProcessResult(0,
            "deploymentconfig.apps.openshift.io/anshul-dc\n"
                + "REVISION\tSTATUS\t\tCAUSE\n"
                + "137\t\tComplete\tconfig change\n"
                + "138\t\tComplete\tconfig change\n"
                + "139\t\tComplete\tconfig change\n"
                + "140\t\tComplete\tconfig change\n"));
    String latestRevision;
    latestRevision =
        helper.getLatestRevision(Kubectl.client("kubectl", "kubeconfig"), deploymentConfig().getResourceId(),
            K8sDelegateTaskParams.builder()
                .ocPath("oc")
                .kubeconfigPath("kubeconfig")
                .workingDirectory("./working-dir")
                .build());

    PowerMockito.verifyStatic(Utils.class);
    Utils.executeScript(eq("./working-dir"),
        eq("oc --kubeconfig=null rollout history DeploymentConfig/test-dc --namespace=default"), any(), any());
    assertThat(latestRevision).isEqualTo("140");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void getLatestRevisionForDeployment() throws Exception {
    PowerMockito.mockStatic(K8sTaskHelper.class);
    PowerMockito.when(K8sTaskHelper.executeCommandSilent(any(), anyString()))
        .thenReturn(buildProcessResult(0,
            "deployments \"nginx-deployment\"\n"
                + "REVISION    CHANGE-CAUSE\n"
                + "1           kubectl apply --filename=https://k8s.io/examples/controllers/nginx-deployment.yaml --record=true\n"
                + "2           kubectl set image deployment.v1.apps/nginx-deployment nginx=nginx:1.16.1 --record=true\n"
                + "3           kubectl set image deployment.v1.apps/nginx-deployment nginx=nginx:1.161 --record=true"));
    String latestRevision;
    latestRevision = helper.getLatestRevision(Kubectl.client("kubectl", "kubeconfig"), deployment().getResourceId(),
        K8sDelegateTaskParams.builder()
            .kubectlPath("kubectl")
            .kubeconfigPath("kubeconfig")
            .workingDirectory("./working-dir")
            .build());

    ArgumentCaptor<RolloutHistoryCommand> captor = ArgumentCaptor.forClass(RolloutHistoryCommand.class);
    PowerMockito.verifyStatic(K8sTaskHelper.class);
    K8sTaskHelper.executeCommandSilent(captor.capture(), eq("./working-dir"));
    RolloutHistoryCommand rolloutHistoryCommand = captor.getValue();
    assertThat(rolloutHistoryCommand.command())
        .isEqualTo("kubectl --kubeconfig=kubeconfig rollout history Deployment/nginx-deployment --namespace=default");
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
    return Arrays.asList(
        ManifestFile.builder().fileContent(readFileContent(deploymentYaml)).fileName(deploymentYaml).build(),
        ManifestFile.builder()
            .fileName(deploymentConfigYaml)
            .fileContent(readFileContent(deploymentConfigYaml))
            .build(),
        ManifestFile.builder().fileName(configMapYaml).fileContent(readFileContent(configMapYaml)).build());
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
    helper.setNamespaceToKubernetesResourcesIfRequired(null, "default");
    helper.setNamespaceToKubernetesResourcesIfRequired(emptyList(), "default");
    KubernetesResource deployment = deployment();
    deployment.getResourceId().setNamespace(null);
    KubernetesResource configMap = configMap();
    configMap.getResourceId().setNamespace("default");
    helper.setNamespaceToKubernetesResourcesIfRequired(asList(deployment, configMap), "harness");
    assertThat(deployment.getResourceId().getNamespace()).isEqualTo("harness");
    assertThat(configMap.getResourceId().getNamespace()).isEqualTo("default");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void getResourcesInStringFormat() throws IOException {
    final String resourcesInStringFormat =
        K8sTaskHelper.getResourcesInStringFormat(asList(deployment().getResourceId(), configMap().getResourceId()));
    assertThat(resourcesInStringFormat)
        .isEqualTo("\n"
            + "- default/Deployment/nginx-deployment\n"
            + "- default/configMap/config-map");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void describe() throws Exception {
    PowerMockito.mockStatic(K8sTaskHelper.class);
    PowerMockito.when(K8sTaskHelper.executeCommand(any(), anyString(), any())).thenReturn(buildProcessResult(0));
    helper.describe(Kubectl.client("kubectl", "kubeconfig"),
        K8sDelegateTaskParams.builder().workingDirectory("./working-dir").build(), logCallback);
    verifyStatic(K8sTaskHelper.class);
    ArgumentCaptor<DescribeCommand> captor = ArgumentCaptor.forClass(DescribeCommand.class);
    K8sTaskHelper.executeCommand(captor.capture(), eq("./working-dir"), any(ExecutionLogCallback.class));
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
    doReturn("").when(spyHelper).getManifestFileNamesInLogFormat(anyString());
    assertThat(
        spyHelper.fetchManifestFilesAndWriteToDirectory(
            K8sDelegateManifestConfig.builder()
                .manifestStoreTypes(storeType)
                .gitConfig(GitConfig.builder().repoUrl("helm-url").build())
                .gitFileConfig(
                    GitFileConfig.builder().filePath("dir/file").branch("master").connectorId("git-connector").build())
                .build(),
            "./dir", logCallback))
        .isTrue();

    verify(mockGitService, times(1))
        .downloadFiles(eq(GitConfig.builder().repoUrl("helm-url").build()), eq("git-connector"), eq(null), eq("master"),
            eq(asList("dir/file")), eq(false), eq("./dir"));
    verify(mockEncryptionService, times(1)).decrypt(any(), anyList());

    // handle exception
    doThrow(new RuntimeException())
        .when(mockGitService)
        .downloadFiles(
            any(GitConfig.class), anyString(), anyString(), anyString(), anyList(), anyBoolean(), anyString());
    assertThat(
        spyHelper.fetchManifestFilesAndWriteToDirectory(
            K8sDelegateManifestConfig.builder()
                .manifestStoreTypes(storeType)
                .gitConfig(GitConfig.builder().repoUrl("helm-url").build())
                .gitFileConfig(
                    GitFileConfig.builder().filePath("dir/file").branch("master").connectorId("git-connector").build())
                .build(),
            "./dir", logCallback))
        .isFalse();
    reset(mockGitService);
    reset(mockEncryptionService);
  }

  private void fetchManifestFilesAndWriteToDirectory_helmChartRepo() throws Exception {
    K8sTaskHelper spyHelper = spy(helper);
    doReturn("").when(spyHelper).getManifestFileNamesInLogFormat(anyString());
    final HelmChartConfigParams helmChartConfigParams = HelmChartConfigParams.builder().chartVersion("1.0").build();
    assertThat(spyHelper.fetchManifestFilesAndWriteToDirectory(K8sDelegateManifestConfig.builder()
                                                                   .manifestStoreTypes(StoreType.HelmChartRepo)
                                                                   .helmChartConfigParams(helmChartConfigParams)
                                                                   .build(),
                   "dir", logCallback))
        .isTrue();

    verify(mockHelmTaskHelper, times(1)).printHelmChartInfoInExecutionLogs(helmChartConfigParams, logCallback);
    verify(mockHelmTaskHelper, times(1)).downloadChartFiles(eq(helmChartConfigParams), eq("dir"));

    doThrow(new RuntimeException())
        .when(mockHelmTaskHelper)
        .downloadChartFiles(any(HelmChartConfigParams.class), anyString());
    assertThat(spyHelper.fetchManifestFilesAndWriteToDirectory(K8sDelegateManifestConfig.builder()
                                                                   .manifestStoreTypes(StoreType.HelmChartRepo)
                                                                   .helmChartConfigParams(helmChartConfigParams)
                                                                   .build(),
                   "dir", logCallback))
        .isFalse();
  }

  private void fetchManifestFilesAndWriteToDirectory_local() throws IOException {
    String manifestFileDirectory = Files.createTempDirectory(generateUuid()).toString();
    List<ManifestFile> manifestFiles = new ArrayList<>(prepareSomeCorrectManifestFiles());
    manifestFiles.add(prepareValuesYamlFile());
    boolean success = helper.fetchManifestFilesAndWriteToDirectory(
        K8sDelegateManifestConfig.builder().manifestStoreTypes(Local).manifestFiles(manifestFiles).build(),
        manifestFileDirectory, logCallback);
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
                   manifestFileDirectory, logCallback))
        .isTrue();

    // invalid manifest files directory
    assertThat(helper.fetchManifestFilesAndWriteToDirectory(K8sDelegateManifestConfig.builder()
                                                                .manifestStoreTypes(Local)
                                                                .manifestFiles(prepareSomeCorrectManifestFiles())
                                                                .build(),
                   "", logCallback))
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
}