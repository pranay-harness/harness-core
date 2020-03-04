package software.wings.delegatetasks.k8s;

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
import static io.harness.rule.OwnerRule.YOGESH;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
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
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.kubectl.Utils;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.k8s.model.Release;
import io.harness.k8s.model.ReleaseHistory;
import io.harness.rule.Owner;
import org.apache.sshd.common.file.util.MockPath;
import org.assertj.core.api.Assertions;
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
import org.zeroturnaround.exec.StartedProcess;
import software.wings.WingsBaseTest;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.delegatetasks.helm.HelmTaskHelper;
import software.wings.helpers.ext.helm.HelmConstants.HelmVersion;
import software.wings.helpers.ext.k8s.request.K8sDeleteTaskParameters;
import software.wings.service.impl.KubernetesHelperService;
import software.wings.service.intfc.GitService;
import software.wings.service.intfc.security.EncryptionService;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Utils.class, K8sTaskHelper.class})
@PowerMockIgnore("javax.net.*")
public class K8sTaskHelperTest extends WingsBaseTest {
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

  @Inject @InjectMocks private K8sTaskHelper helper;

  @Before
  public void setup() {
    mockStatic(Files.class);
    when(Files.isRegularFile(any(Path.class))).thenReturn(true);
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testGetTargetInstancesForCanary() {
    ExecutionLogCallback mockLogCallback = mock(ExecutionLogCallback.class);
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

    ExecutionLogCallback executionLogCallback = mock(ExecutionLogCallback.class);
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
        Arrays.asList(Release.builder().status(Release.Status.Succeeded).resources(kubernetesResourceIdList).build()));

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

    ExecutionLogCallback executionLogCallback = mock(ExecutionLogCallback.class);
    doNothing().when(executionLogCallback).saveExecutionLog(anyString());

    ConfigMap configMap = new ConfigMap();
    configMap.setKind(ConfigMap.name());
    doReturn(configMap).when(mockKubernetesContainerService).getConfigMap(any(), anyList(), anyString());
    List<KubernetesResourceId> kubernetesResourceIdList = getKubernetesResourceIdList();
    ReleaseHistory releaseHistory = ReleaseHistory.createNew();
    releaseHistory.setReleases(
        Arrays.asList(Release.builder().status(Release.Status.Succeeded).resources(kubernetesResourceIdList).build()));

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
    PowerMockito.verifyStatic();
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
        "kubectl --kubeconfig=config-path rollout status Deployment/nginx-deployment --watch=true", true);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGenerateTruncatedFileListForLogging() throws Exception {
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
}