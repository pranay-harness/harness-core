package software.wings.delegatetasks.k8s.taskhandler;

import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.YOGESH;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.InstanceUnitType.COUNT;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.yaml.YamlConstants.PATH_DELIMITER;
import static software.wings.delegatetasks.k8s.K8sTestConstants.DAEMON_SET_YAML;
import static software.wings.delegatetasks.k8s.K8sTestConstants.DEPLOYMENT_DIRECT_APPLY_YAML;
import static software.wings.delegatetasks.k8s.K8sTestConstants.DEPLOYMENT_YAML;
import static software.wings.delegatetasks.k8s.K8sTestConstants.STATEFUL_SET_YAML;

import io.harness.category.element.UnitTests;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.InvalidArgumentsException;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.manifest.ManifestHelper;
import io.harness.k8s.model.K8sPod;
import io.harness.k8s.model.Kind;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.k8s.model.Release;
import io.harness.k8s.model.ReleaseHistory;
import io.harness.rule.Owner;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.delegatetasks.k8s.K8sDelegateTaskParams;
import software.wings.delegatetasks.k8s.K8sTaskHelper;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sCanaryDeployTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sCanaryDeployResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class K8sCanaryDeployTaskHandlerTest extends WingsBaseTest {
  @Mock private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Mock private KubernetesContainerService kubernetesContainerService;
  @Mock private K8sTaskHelper k8sTaskHelper;
  @Mock private ExecutionLogCallback executionLogCallback;
  @InjectMocks private K8sCanaryDeployTaskHandler k8sCanaryDeployTaskHandler;

  private ReleaseHistory releaseHistory;
  private KubernetesResource deployment;
  private KubernetesResource deploymentConfig;

  private String resourcePath = "./k8s";
  private String deploymentYaml = "deployment.yaml";
  @Before
  public void setup() throws Exception {
    releaseHistory = ReleaseHistory.createNew();
    File yamlFile = null;
    try {
      yamlFile =
          new File(getClass().getClassLoader().getResource(resourcePath + PATH_DELIMITER + deploymentYaml).toURI());
    } catch (URISyntaxException e) {
      Assertions.fail("Unable to find yaml file " + deploymentYaml);
    }
    assertThat(yamlFile).isNotNull();
    String yamlString = FileUtils.readFileToString(yamlFile, "UTF-8");
    deployment =
        KubernetesResource.builder()
            .spec(yamlString)
            .resourceId(
                KubernetesResourceId.builder().namespace("default").kind("Deployment").name("nginx-deployment").build())
            .build();
    doReturn(true)
        .when(k8sTaskHelper)
        .fetchManifestFilesAndWriteToDirectory(
            any(K8sDelegateManifestConfig.class), anyString(), any(ExecutionLogCallback.class));
    doReturn(true)
        .when(k8sTaskHelper)
        .applyManifests(
            any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class), any(ExecutionLogCallback.class));
    doReturn(true)
        .when(k8sTaskHelper)
        .doStatusCheck(any(Kubectl.class), any(KubernetesResourceId.class), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class));
    doReturn(Mockito.mock(ExecutionLogCallback.class))
        .when(k8sTaskHelper)
        .getExecutionLogCallback(any(K8sCanaryDeployTaskParameters.class), anyString());
    doReturn(KubernetesConfig.builder().namespace("default").build())
        .when(containerDeploymentDelegateHelper)
        .getKubernetesConfig(any(K8sClusterConfig.class));
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testDryRunIsSkipped() throws Exception {
    K8sCanaryDeployTaskParameters canaryDeployTaskParams =
        K8sCanaryDeployTaskParameters.builder().skipDryRun(true).build();
    K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().build();
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback();

    when(containerDeploymentDelegateHelper.getKubernetesConfig(any(K8sClusterConfig.class)))
        .thenReturn(KubernetesConfig.builder().build());
    when(kubernetesContainerService.fetchReleaseHistory(any(), any(), any())).thenReturn(null);
    doNothing().when(k8sTaskHelper).deleteSkippedManifestFiles(any(), any());
    when(k8sTaskHelper.renderTemplate(any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(Collections.emptyList());
    when(k8sTaskHelper.readManifests(any(), any())).thenReturn(Collections.emptyList());
    when(k8sTaskHelper.updateDestinationRuleManifestFilesWithSubsets(any(), any(), any(), any())).thenReturn(null);
    when(k8sTaskHelper.updateVirtualServiceManifestFilesWithRoutesForCanary(any(), any(), any())).thenReturn(null);

    k8sCanaryDeployTaskHandler.init(canaryDeployTaskParams, delegateTaskParams, executionLogCallback);
    verify(k8sTaskHelper, times(0)).dryRunManifests(any(), any(), any(), any());
    verify(k8sTaskHelper, times(1)).updateVirtualServiceManifestFilesWithRoutesForCanary(any(), any(), any());
    verify(k8sTaskHelper, times(1)).updateDestinationRuleManifestFilesWithSubsets(any(), any(), any(), any());
    verify(k8sTaskHelper, times(1)).readManifests(any(), any());
    verify(k8sTaskHelper, times(1)).renderTemplate(any(), any(), any(), any(), any(), any(), any(), any());
    verify(k8sTaskHelper, times(1)).deleteSkippedManifestFiles(any(), any());
    verify(kubernetesContainerService, times(1)).fetchReleaseHistory(any(), any(), any());
    verify(containerDeploymentDelegateHelper, times(1)).getKubernetesConfig(any(K8sClusterConfig.class));
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testDryRunIsNotSkipped() throws Exception {
    K8sCanaryDeployTaskParameters canaryDeployTaskParams =
        K8sCanaryDeployTaskParameters.builder().skipDryRun(false).build();
    K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().build();
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback();

    when(k8sTaskHelper.renderTemplate(any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(Collections.emptyList());
    when(k8sTaskHelper.readManifests(any(), any())).thenReturn(Collections.emptyList());
    when(containerDeploymentDelegateHelper.getKubernetesConfig(any(K8sClusterConfig.class)))
        .thenReturn(KubernetesConfig.builder().build());
    when(kubernetesContainerService.fetchReleaseHistory(any(), any(), any())).thenReturn(null);
    doNothing().when(k8sTaskHelper).deleteSkippedManifestFiles(any(), any());
    when(k8sTaskHelper.updateDestinationRuleManifestFilesWithSubsets(any(), any(), any(), any())).thenReturn(null);
    when(k8sTaskHelper.updateVirtualServiceManifestFilesWithRoutesForCanary(any(), any(), any())).thenReturn(null);

    k8sCanaryDeployTaskHandler.init(canaryDeployTaskParams, delegateTaskParams, executionLogCallback);
    verify(k8sTaskHelper, times(1)).dryRunManifests(any(), any(), any(), any());
    verify(k8sTaskHelper, times(1)).updateVirtualServiceManifestFilesWithRoutesForCanary(any(), any(), any());
    verify(k8sTaskHelper, times(1)).readManifests(any(), any());
    verify(k8sTaskHelper, times(1)).renderTemplate(any(), any(), any(), any(), any(), any(), any(), any());
    verify(k8sTaskHelper, times(1)).updateDestinationRuleManifestFilesWithSubsets(any(), any(), any(), any());
    verify(k8sTaskHelper, times(1)).deleteSkippedManifestFiles(any(), any());
    verify(kubernetesContainerService, times(1)).fetchReleaseHistory(any(), any(), any());
    verify(containerDeploymentDelegateHelper, times(1)).getKubernetesConfig(any(K8sClusterConfig.class));
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testDeploymentWorkloadsForCanary() throws Exception {
    K8sCanaryDeployTaskParameters canaryDeployTaskParams = K8sCanaryDeployTaskParameters.builder().build();
    K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().build();

    List<KubernetesResource> kubernetesResources = new ArrayList<>();
    kubernetesResources.addAll(ManifestHelper.processYaml(DAEMON_SET_YAML));
    on(k8sCanaryDeployTaskHandler).set("resources", kubernetesResources);

    boolean result =
        k8sCanaryDeployTaskHandler.prepareForCanary(delegateTaskParams, canaryDeployTaskParams, executionLogCallback);

    assertThat(result).isFalse();
    verify(k8sTaskHelper, never()).cleanup(any(), any(), any(), any());
    verify(k8sTaskHelper, times(1)).getResourcesInTableFormat(any());

    ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<LogLevel> logLevelCaptor = ArgumentCaptor.forClass(LogLevel.class);
    ArgumentCaptor<CommandExecutionStatus> commandExecutionStatusCaptor =
        ArgumentCaptor.forClass(CommandExecutionStatus.class);
    verify(executionLogCallback, times(1))
        .saveExecutionLog(msgCaptor.capture(), logLevelCaptor.capture(), commandExecutionStatusCaptor.capture());
    assertThat(logLevelCaptor.getValue()).isEqualTo(ERROR);
    assertThat(commandExecutionStatusCaptor.getValue()).isEqualTo(FAILURE);
    assertThat(msgCaptor.getValue())
        .isEqualTo(
            "\nNo workload found in the Manifests. Can't do Canary Deployment. Only Deployment and DeploymentConfig (OpenShift) workloads are supported in Canary workflow type.");

    kubernetesResources.addAll(ManifestHelper.processYaml(STATEFUL_SET_YAML));
    kubernetesResources.addAll(ManifestHelper.processYaml(DEPLOYMENT_DIRECT_APPLY_YAML));

    on(k8sCanaryDeployTaskHandler).set("resources", kubernetesResources);

    result =
        k8sCanaryDeployTaskHandler.prepareForCanary(delegateTaskParams, canaryDeployTaskParams, executionLogCallback);

    assertThat(result).isFalse();
    verify(k8sTaskHelper, never()).cleanup(any(), any(), any(), any());
    verify(k8sTaskHelper, times(2)).getResourcesInTableFormat(any());

    msgCaptor = ArgumentCaptor.forClass(String.class);
    logLevelCaptor = ArgumentCaptor.forClass(LogLevel.class);
    commandExecutionStatusCaptor = ArgumentCaptor.forClass(CommandExecutionStatus.class);
    verify(executionLogCallback, times(2))
        .saveExecutionLog(msgCaptor.capture(), logLevelCaptor.capture(), commandExecutionStatusCaptor.capture());
    assertThat(logLevelCaptor.getValue()).isEqualTo(ERROR);
    assertThat(commandExecutionStatusCaptor.getValue()).isEqualTo(FAILURE);
    assertThat(msgCaptor.getValue())
        .isEqualTo(
            "\nNo workload found in the Manifests. Can't do Canary Deployment. Only Deployment and DeploymentConfig (OpenShift) workloads are supported in Canary workflow type.");

    kubernetesResources.addAll(ManifestHelper.processYaml(DEPLOYMENT_YAML));
    kubernetesResources.addAll(ManifestHelper.processYaml(DEPLOYMENT_YAML));
    on(k8sCanaryDeployTaskHandler).set("resources", kubernetesResources);

    result =
        k8sCanaryDeployTaskHandler.prepareForCanary(delegateTaskParams, canaryDeployTaskParams, executionLogCallback);

    assertThat(result).isFalse();
    verify(k8sTaskHelper, never()).cleanup(any(), any(), any(), any());
    verify(k8sTaskHelper, times(3)).getResourcesInTableFormat(any());

    msgCaptor = ArgumentCaptor.forClass(String.class);
    logLevelCaptor = ArgumentCaptor.forClass(LogLevel.class);
    commandExecutionStatusCaptor = ArgumentCaptor.forClass(CommandExecutionStatus.class);
    verify(executionLogCallback, times(3))
        .saveExecutionLog(msgCaptor.capture(), logLevelCaptor.capture(), commandExecutionStatusCaptor.capture());
    assertThat(logLevelCaptor.getValue()).isEqualTo(ERROR);
    assertThat(commandExecutionStatusCaptor.getValue()).isEqualTo(FAILURE);
    assertThat(msgCaptor.getValue())
        .isEqualTo(
            "\nMore than one workloads found in the Manifests. Canary deploy supports only one workload. Others should be marked with annotation harness.io/direct-apply: true");

    kubernetesResources = new ArrayList<>();
    kubernetesResources.addAll(ManifestHelper.processYaml(DEPLOYMENT_YAML));
    on(k8sCanaryDeployTaskHandler).set("resources", kubernetesResources);
    on(k8sCanaryDeployTaskHandler).set("releaseHistory", ReleaseHistory.createNew());

    doNothing().when(k8sTaskHelper).cleanup(any(), any(), any(), any());
    result =
        k8sCanaryDeployTaskHandler.prepareForCanary(delegateTaskParams, canaryDeployTaskParams, executionLogCallback);

    assertThat(result).isFalse();
    verify(k8sTaskHelper, times(1)).cleanup(any(), any(), any(), any());
    verify(k8sTaskHelper, times(4)).getResourcesInTableFormat(any());

    ArgumentCaptor<KubernetesResourceId> resourceIdArgumentCaptor = ArgumentCaptor.forClass(KubernetesResourceId.class);
    verify(k8sTaskHelper, times(1)).getCurrentReplicas(any(), resourceIdArgumentCaptor.capture(), any());

    KubernetesResourceId kubernetesResourceId = resourceIdArgumentCaptor.getValue();
    assertThat(kubernetesResourceId.getKind()).isEqualTo(Kind.Deployment.name());
    assertThat(kubernetesResourceId.getName()).isEqualTo("deployment");
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testSupportedWorkloadsInBgWorkflow() {
    K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().build();

    List<KubernetesResource> kubernetesResources = new ArrayList<>();
    kubernetesResources.addAll(ManifestHelper.processYaml(STATEFUL_SET_YAML));

    on(k8sCanaryDeployTaskHandler).set("resources", kubernetesResources);

    boolean result = k8sCanaryDeployTaskHandler.prepareForCanary(
        delegateTaskParams, K8sCanaryDeployTaskParameters.builder().build(), executionLogCallback);
    assertThat(result).isFalse();

    verify(executionLogCallback, times(1))
        .saveExecutionLog(
            "\nNo workload found in the Manifests. Can't do Canary Deployment. Only Deployment and DeploymentConfig (OpenShift) workloads are supported in Canary workflow type.",
            LogLevel.ERROR, CommandExecutionStatus.FAILURE);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGetAllPods() throws Exception {
    KubernetesResource kubernetesResource =
        KubernetesResource.builder().resourceId(KubernetesResourceId.builder().namespace("default").build()).build();
    on(k8sCanaryDeployTaskHandler).set("canaryWorkload", kubernetesResource);
    testGetAllPodsWithNoPreviousPods();
    testGetAllPodsWithPreviousPods();
  }

  private void testGetAllPodsWithNoPreviousPods() throws Exception {
    final K8sPod canaryPod = K8sPod.builder().name("pod-canary").build();
    when(k8sTaskHelper.getPodDetails(any(KubernetesConfig.class), anyString(), anyString()))
        .thenReturn(asList(canaryPod));
    when(k8sTaskHelper.getPodDetailsWithTrack(any(KubernetesConfig.class), anyString(), anyString(), eq("canary")))
        .thenReturn(asList(canaryPod));

    final List<K8sPod> allPods = k8sCanaryDeployTaskHandler.getAllPods();
    assertThat(allPods).hasSize(1);
    assertThat(allPods.get(0).isNewPod()).isTrue();
    assertThat(allPods.get(0).getName()).isEqualTo(canaryPod.getName());
  }

  private void testGetAllPodsWithPreviousPods() throws Exception {
    final K8sPod canaryPod = K8sPod.builder().name("pod-canary").build();
    final List<K8sPod> allPods =
        asList(K8sPod.builder().name("primary-1").build(), K8sPod.builder().name("primary-2").build(), canaryPod);
    when(k8sTaskHelper.getPodDetails(any(KubernetesConfig.class), anyString(), anyString())).thenReturn(allPods);
    when(k8sTaskHelper.getPodDetailsWithTrack(any(KubernetesConfig.class), anyString(), anyString(), eq("canary")))
        .thenReturn(asList(canaryPod));

    final List<K8sPod> pods = k8sCanaryDeployTaskHandler.getAllPods();
    assertThat(pods).hasSize(3);
    assertThat(pods.stream().filter(K8sPod::isNewPod).count()).isEqualTo(1);
    assertThat(pods.stream().map(K8sPod::getName).collect(Collectors.toList()))
        .containsExactlyInAnyOrder("pod-canary", "primary-1", "primary-2");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void invalidTypeOfTaskParams() {
    assertThatExceptionOfType(InvalidArgumentsException.class)
        .isThrownBy(() -> k8sCanaryDeployTaskHandler.executeTaskInternal(null, null))
        .withMessageContaining("INVALID_ARGUMENT");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void failureInFetchingManifestFiles() {
    doReturn(false)
        .when(k8sTaskHelper)
        .fetchManifestFilesAndWriteToDirectory(
            any(K8sDelegateManifestConfig.class), anyString(), any(ExecutionLogCallback.class));

    K8sTaskExecutionResponse response;
    response = k8sCanaryDeployTaskHandler.executeTask(
        K8sCanaryDeployTaskParameters.builder().releaseName("release-name").build(),
        K8sDelegateTaskParams.builder().workingDirectory(".").build());
    assertThat(response.getCommandExecutionStatus()).isEqualTo(FAILURE);
    assertThat(response.getK8sTaskResponse()).isNotNull();

    on(k8sCanaryDeployTaskHandler)
        .set("canaryWorkload",
            KubernetesResource.builder()
                .resourceId(
                    KubernetesResourceId.builder().namespace("default").name("canary").kind("Deployment").build())
                .build());

    response = k8sCanaryDeployTaskHandler.executeTask(
        K8sCanaryDeployTaskParameters.builder().releaseName("release-name").build(),
        K8sDelegateTaskParams.builder().workingDirectory(".").build());

    assertThat(response.getCommandExecutionStatus()).isEqualTo(FAILURE);
    assertThat(((K8sCanaryDeployResponse) response.getK8sTaskResponse()).getCanaryWorkload())
        .isEqualTo("default/Deployment/canary");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testExecute() throws Exception {
    K8sCanaryDeployTaskHandler handler = spy(k8sCanaryDeployTaskHandler);
    doReturn(true).when(handler).init(
        any(K8sCanaryDeployTaskParameters.class), any(K8sDelegateTaskParams.class), any(ExecutionLogCallback.class));
    doReturn(true).when(handler).prepareForCanary(
        any(K8sDelegateTaskParams.class), any(K8sCanaryDeployTaskParameters.class), any(ExecutionLogCallback.class));
    doReturn(Arrays.asList(K8sPod.builder().build())).when(handler).getAllPods();

    on(handler).set("canaryWorkload", deployment);
    ReleaseHistory releaseHist = ReleaseHistory.createNew();
    releaseHist.setReleases(asList(Release.builder().number(2).build()));
    on(handler).set("releaseHistory", releaseHist);
    on(handler).set("currentRelease", releaseHist.getLatestRelease());
    on(handler).set("targetInstances", 3);

    final K8sTaskExecutionResponse response =
        handler.executeTask(K8sCanaryDeployTaskParameters.builder().build(), K8sDelegateTaskParams.builder().build());
    verify(k8sTaskHelper, times(1))
        .describe(any(Kubectl.class), any(K8sDelegateTaskParams.class), any(ExecutionLogCallback.class));
    final K8sCanaryDeployResponse canaryDeployResponse = (K8sCanaryDeployResponse) response.getK8sTaskResponse();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(SUCCESS);
    assertThat(canaryDeployResponse.getCanaryWorkload()).isEqualTo("default/Deployment/nginx-deployment");
    assertThat(canaryDeployResponse.getCurrentInstances()).isEqualTo(3);
    assertThat(canaryDeployResponse.getReleaseNumber()).isEqualTo(2);
    assertThat(canaryDeployResponse.getK8sPodList()).hasSize(1);

    // status check fails
    doReturn(false)
        .when(k8sTaskHelper)
        .doStatusCheck(any(Kubectl.class), any(KubernetesResourceId.class), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class));
    final K8sTaskExecutionResponse failureResponse =
        handler.executeTask(K8sCanaryDeployTaskParameters.builder().build(), K8sDelegateTaskParams.builder().build());
    assertThat(((ReleaseHistory) on(handler).get("releaseHistory")).getLatestRelease().getStatus())
        .isEqualTo(Release.Status.Failed);
    assertThat(failureResponse.getCommandExecutionStatus()).isEqualTo(FAILURE);

    verify(kubernetesContainerService, times(2))
        .saveReleaseHistory(any(KubernetesConfig.class), anyList(), anyString(), anyString());
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testInit() throws Exception {
    doReturn(Arrays.asList(deployment)).when(k8sTaskHelper).readManifests(anyList(), any());
    k8sCanaryDeployTaskHandler.init(K8sCanaryDeployTaskParameters.builder().build(),
        K8sDelegateTaskParams.builder().build(), Mockito.mock(ExecutionLogCallback.class));

    verify(k8sTaskHelper, times(1)).deleteSkippedManifestFiles(anyString(), any(ExecutionLogCallback.class));
    verify(k8sTaskHelper, times(1))
        .renderTemplate(any(K8sDelegateTaskParams.class), any(K8sDelegateManifestConfig.class), anyString(), anyList(),
            anyString(), anyString(), any(), any(K8sTaskParameters.class));
    verify(k8sTaskHelper, times(1)).setNamespaceToKubernetesResourcesIfRequired(asList(deployment), "default");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testInitException() throws Exception {
    doThrow(new RuntimeException())
        .when(k8sTaskHelper)
        .renderTemplate(any(K8sDelegateTaskParams.class), any(K8sDelegateManifestConfig.class), anyString(), anyList(),
            anyString(), anyString(), any(), any(K8sTaskParameters.class));
    final boolean success = k8sCanaryDeployTaskHandler.init(K8sCanaryDeployTaskParameters.builder().build(),
        K8sDelegateTaskParams.builder().build(), mock(ExecutionLogCallback.class));
    assertThat(success).isFalse();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void prepareForCanary() throws Exception {
    cannotDeployMoreThan1Workload();
    cannotDeployMoreThanEmptyWorkload();
    deploySingleWorkload();
  }

  private void deploySingleWorkload() throws Exception {
    List<KubernetesResource> resources = asList(
        KubernetesResource.builder()
            .resourceId(KubernetesResourceId.builder().versioned(true).name("object-1").kind("configMap").build())
            .build(),
        deployment);
    doReturn(2)
        .when(k8sTaskHelper)
        .getCurrentReplicas(any(Kubectl.class), any(KubernetesResourceId.class), any(K8sDelegateTaskParams.class));
    on(k8sCanaryDeployTaskHandler).set("resources", resources);
    on(k8sCanaryDeployTaskHandler).set("releaseHistory", releaseHistory);
    on(k8sCanaryDeployTaskHandler).set("releaseName", "release-01");

    boolean success = k8sCanaryDeployTaskHandler.prepareForCanary(K8sDelegateTaskParams.builder().build(),
        K8sCanaryDeployTaskParameters.builder().instanceUnitType(COUNT).instances(4).build(),
        mock(ExecutionLogCallback.class));

    KubernetesResource canaryWorkload = (KubernetesResource) on(k8sCanaryDeployTaskHandler).get("canaryWorkload");
    Map matchLabels = (Map) canaryWorkload.getField("spec.selector.matchLabels");
    Map podsSpecLabels = (Map) canaryWorkload.getField("spec.template.metadata.labels");
    assertThat(success).isTrue();
    assertThat(canaryWorkload.getResourceId().getName()).endsWith("canary");
    assertThat((int) on(k8sCanaryDeployTaskHandler).get("targetInstances")).isEqualTo(4);
    assertThat(matchLabels.get("harness.io/track")).isEqualTo("canary");
    assertThat(podsSpecLabels.get("harness.io/track")).isEqualTo("canary");
    assertThat(podsSpecLabels.get("harness.io/release-name")).isEqualTo("release-01");
    assertThat(canaryWorkload.getReplicaCount()).isEqualTo(4);
  }

  private void cannotDeployMoreThanEmptyWorkload() {
    List<KubernetesResource> resources = asList(
        KubernetesResource.builder()
            .resourceId(KubernetesResourceId.builder().versioned(true).name("object-1").kind("configMap").build())
            .build());
    on(k8sCanaryDeployTaskHandler).set("resources", resources);

    boolean success = k8sCanaryDeployTaskHandler.prepareForCanary(K8sDelegateTaskParams.builder().build(),
        K8sCanaryDeployTaskParameters.builder().build(), mock(ExecutionLogCallback.class));

    assertThat(success).isFalse();
  }

  private void cannotDeployMoreThan1Workload() {
    List<KubernetesResource> resources = asList(
        KubernetesResource.builder()
            .resourceId(KubernetesResourceId.builder().versioned(true).name("object-1").kind("configMap").build())
            .build(),
        KubernetesResource.builder()
            .resourceId(KubernetesResourceId.builder().versioned(false).name("object-2").kind("Deployment").build())
            .build(),
        KubernetesResource.builder()
            .resourceId(
                KubernetesResourceId.builder().versioned(false).name("object-3").kind("DeploymentConfig").build())
            .build());
    on(k8sCanaryDeployTaskHandler).set("resources", resources);

    boolean success = k8sCanaryDeployTaskHandler.prepareForCanary(K8sDelegateTaskParams.builder().build(),
        K8sCanaryDeployTaskParameters.builder().build(), mock(ExecutionLogCallback.class));

    assertThat(success).isFalse();
  }
}
