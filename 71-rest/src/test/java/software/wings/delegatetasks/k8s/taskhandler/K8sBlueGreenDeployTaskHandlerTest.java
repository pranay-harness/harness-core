package software.wings.delegatetasks.k8s.taskhandler;

import static io.harness.k8s.K8sConstants.MANIFEST_FILES_DIR;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.ACASIAN;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.BOJANA;
import static io.harness.rule.OwnerRule.ROHITKARELIA;
import static io.harness.rule.OwnerRule.YOGESH;

import static software.wings.delegatetasks.k8s.K8sTestConstants.DEPLOYMENT_YAML;
import static software.wings.delegatetasks.k8s.K8sTestConstants.PRIMARY_SERVICE_YAML;
import static software.wings.delegatetasks.k8s.K8sTestConstants.SERVICE_YAML;
import static software.wings.delegatetasks.k8s.K8sTestConstants.STAGE_SERVICE_YAML;
import static software.wings.delegatetasks.k8s.K8sTestConstants.STATEFUL_SET_YAML;
import static software.wings.delegatetasks.k8s.K8sTestHelper.configMap;
import static software.wings.delegatetasks.k8s.K8sTestHelper.deployment;
import static software.wings.delegatetasks.k8s.K8sTestHelper.primaryService;
import static software.wings.delegatetasks.k8s.K8sTestHelper.service;
import static software.wings.delegatetasks.k8s.K8sTestHelper.stageService;
import static software.wings.utils.WingsTestConstants.LONG_TIMEOUT_INTERVAL;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.delegate.task.k8s.K8sTaskHelperBase;
import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.exception.InvalidArgumentsException;
import io.harness.k8s.KubernetesContainerService;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.manifest.ManifestHelper;
import io.harness.k8s.model.HarnessLabelValues;
import io.harness.k8s.model.HarnessLabels;
import io.harness.k8s.model.K8sDelegateTaskParams;
import io.harness.k8s.model.K8sPod;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.k8s.model.Release;
import io.harness.k8s.model.Release.KubernetesResourceIdRevision;
import io.harness.k8s.model.ReleaseHistory;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.k8s.K8sTaskHelper;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.helm.request.HelmChartConfigParams;
import software.wings.helpers.ext.helm.response.HelmChartInfo;
import software.wings.helpers.ext.k8s.request.K8sBlueGreenDeployTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig;
import software.wings.helpers.ext.k8s.response.K8sBlueGreenDeployResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskResponse;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServiceBuilder;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Data;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class K8sBlueGreenDeployTaskHandlerTest extends WingsBaseTest {
  @Mock private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Mock private KubernetesContainerService kubernetesContainerService;
  @Mock private K8sTaskHelper k8sTaskHelper;
  @Mock public K8sTaskHelperBase k8sTaskHelperBase;
  @Mock private ExecutionLogCallback executionLogCallback;
  @Mock private ReleaseHistory releaseHistory;
  @Mock private Kubectl client;

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void executeInternalSkeleton() throws Exception {
    K8sBlueGreenDeployTaskHandler spyHandler = spy(k8sBlueGreenDeployTaskHandler);
    doReturn(true)
        .when(k8sTaskHelper)
        .fetchManifestFilesAndWriteToDirectory(
            any(K8sDelegateManifestConfig.class), anyString(), any(ExecutionLogCallback.class), anyLong());
    doReturn(executionLogCallback)
        .when(k8sTaskHelper)
        .getExecutionLogCallback(any(K8sBlueGreenDeployTaskParameters.class), anyString());
    doReturn(true)
        .when(spyHandler)
        .init(any(K8sBlueGreenDeployTaskParameters.class), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class));
    doReturn(true)
        .when(spyHandler)
        .prepareForBlueGreen(any(K8sBlueGreenDeployTaskParameters.class), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class));
    on(spyHandler).set("managedWorkload", deployment());
    on(spyHandler).set("currentRelease", new Release());
    on(spyHandler).set("primaryService", primaryService());
    on(spyHandler).set("stageService", stageService());
    doReturn(true)
        .when(k8sTaskHelperBase)
        .applyManifests(any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class), anyBoolean());
    doReturn(true)
        .when(k8sTaskHelperBase)
        .doStatusCheck(any(Kubectl.class), any(KubernetesResourceId.class), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class));
    on(spyHandler)
        .set("resources", new ArrayList<>(asList(primaryService(), deployment(), stageService(), configMap())));
    doReturn("latest-rev")
        .when(k8sTaskHelperBase)
        .getLatestRevision(any(Kubectl.class), eq(deployment().getResourceId()), any(K8sDelegateTaskParams.class));

    spyHandler.executeTaskInternal(
        K8sBlueGreenDeployTaskParameters.builder().k8sTaskType(K8sTaskType.BLUE_GREEN_DEPLOY).build(),
        K8sDelegateTaskParams.builder()
            .workingDirectory("./working-dir")
            .kubectlPath("kubectl")
            .kubeconfigPath("kubeconfig")
            .build());

    ArgumentCaptor<K8sBlueGreenDeployResponse> captor = ArgumentCaptor.forClass(K8sBlueGreenDeployResponse.class);

    verify(k8sTaskHelper, times(1)).getK8sTaskExecutionResponse(captor.capture(), eq(SUCCESS));
    final K8sBlueGreenDeployResponse k8sTaskResponse = captor.getValue();
    assertThat(k8sTaskResponse.getPrimaryServiceName()).isEqualTo(primaryService().getResourceId().getName());
    assertThat(k8sTaskResponse.getStageServiceName()).isEqualTo(stageService().getResourceId().getName());
    assertThat(k8sTaskResponse.getReleaseNumber()).isEqualTo(0);
    assertThat(((Release) on(spyHandler).get("currentRelease")).getManagedWorkloadRevision()).isEqualTo("latest-rev");

    K8sBlueGreenDeployTaskParameters deployTaskParams =
        K8sBlueGreenDeployTaskParameters.builder().releaseName("releaseName-statusCheck").build();
    K8sDelegateTaskParams taskParams = K8sDelegateTaskParams.builder().build();

    doReturn(false)
        .when(k8sTaskHelperBase)
        .doStatusCheck(any(Kubectl.class), any(KubernetesResourceId.class), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class));
    K8sTaskExecutionResponse response = spyHandler.executeTaskInternal(deployTaskParams, taskParams);
    assertThat(response.getCommandExecutionStatus()).isEqualTo(FAILURE);
    verify(k8sTaskHelperBase, times(2))
        .saveReleaseHistoryInConfigMap(
            any(KubernetesConfig.class), eq("releaseName-statusCheck"), anyString(), eq(false));

    deployTaskParams.setReleaseName("releaseName-apply");
    doReturn(false)
        .when(k8sTaskHelperBase)
        .applyManifests(any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class), anyBoolean());
    response = spyHandler.executeTaskInternal(deployTaskParams, taskParams);
    assertThat(response.getCommandExecutionStatus()).isEqualTo(FAILURE);
    verify(k8sTaskHelperBase)
        .saveReleaseHistoryInConfigMap(any(KubernetesConfig.class), eq("releaseName-apply"), anyString(), eq(false));
  }

  @InjectMocks private K8sBlueGreenDeployTaskHandler k8sBlueGreenDeployTaskHandler;

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testDryRunIsSkipped() throws Exception {
    K8sBlueGreenDeployTaskParameters blueGreenDeployTaskParams =
        K8sBlueGreenDeployTaskParameters.builder().skipDryRun(true).build();
    K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().build();
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback();

    when(containerDeploymentDelegateHelper.getKubernetesConfig(any(K8sClusterConfig.class), eq(false)))
        .thenReturn(KubernetesConfig.builder().build());
    doNothing().when(k8sTaskHelperBase).deleteSkippedManifestFiles(any(), any());
    when(k8sTaskHelperBase.getReleaseHistoryDataFromConfigMap(any(), any(), anyBoolean())).thenReturn(null);
    when(k8sTaskHelper.renderTemplate(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(emptyList());
    doNothing().when(k8sTaskHelperBase).setNamespaceToKubernetesResourcesIfRequired(any(), any());
    when(k8sTaskHelperBase.readManifests(any(), any())).thenReturn(emptyList());

    k8sBlueGreenDeployTaskHandler.init(blueGreenDeployTaskParams, delegateTaskParams, executionLogCallback);
    verify(k8sTaskHelperBase, times(0)).dryRunManifests(any(), any(), any(), any());
    verify(k8sTaskHelperBase, times(1)).readManifests(any(), any());
    verify(k8sTaskHelper, times(1)).renderTemplate(any(), any(), any(), any(), any(), any(), any(), any());
    verify(k8sTaskHelperBase, times(1)).setNamespaceToKubernetesResourcesIfRequired(any(), any());
    verify(k8sTaskHelperBase, times(1)).deleteSkippedManifestFiles(any(), any());
    verify(k8sTaskHelperBase, times(1)).getReleaseHistoryDataFromConfigMap(any(), any(), anyBoolean());
    verify(containerDeploymentDelegateHelper, times(1)).getKubernetesConfig(any(K8sClusterConfig.class), eq(false));
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testDryRunIsNotSkipped() throws Exception {
    K8sBlueGreenDeployTaskParameters blueGreenDeployTaskParams =
        K8sBlueGreenDeployTaskParameters.builder().skipDryRun(false).build();
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback();
    K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().build();

    when(containerDeploymentDelegateHelper.getKubernetesConfig(any(K8sClusterConfig.class), eq(false)))
        .thenReturn(KubernetesConfig.builder().build());
    doNothing().when(k8sTaskHelperBase).deleteSkippedManifestFiles(any(), any());
    when(k8sTaskHelperBase.getReleaseHistoryDataFromConfigMap(any(), any(), anyBoolean())).thenReturn(null);
    doNothing().when(k8sTaskHelperBase).setNamespaceToKubernetesResourcesIfRequired(any(), any());
    when(k8sTaskHelper.renderTemplate(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(emptyList());
    when(k8sTaskHelperBase.readManifests(any(), any())).thenReturn(emptyList());

    k8sBlueGreenDeployTaskHandler.init(blueGreenDeployTaskParams, delegateTaskParams, executionLogCallback);
    verify(k8sTaskHelperBase, times(1)).dryRunManifests(any(), any(), any(), any());
    verify(k8sTaskHelperBase, times(1)).readManifests(any(), any());
    verify(k8sTaskHelper, times(1)).renderTemplate(any(), any(), any(), any(), any(), any(), any(), any());
    verify(k8sTaskHelperBase, times(1)).setNamespaceToKubernetesResourcesIfRequired(any(), any());
    verify(k8sTaskHelperBase, times(1)).deleteSkippedManifestFiles(any(), any());
    verify(k8sTaskHelperBase, times(1)).getReleaseHistoryDataFromConfigMap(any(), any(), anyBoolean());
    verify(containerDeploymentDelegateHelper, times(1)).getKubernetesConfig(any(K8sClusterConfig.class), eq(false));
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetPrimaryColorFromService() {
    K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().build();

    List<KubernetesResource> resources = new ArrayList<>();
    resources.addAll(ManifestHelper.processYaml(DEPLOYMENT_YAML));
    resources.addAll(ManifestHelper.processYaml(PRIMARY_SERVICE_YAML));
    resources.addAll(ManifestHelper.processYaml(STAGE_SERVICE_YAML));
    on(k8sBlueGreenDeployTaskHandler).set("resources", resources);
    V1Service primaryService = new V1ServiceBuilder()
                                   .withNewSpec()
                                   .withSelector(ImmutableMap.of(HarnessLabels.color, HarnessLabelValues.colorGreen))
                                   .endSpec()
                                   .build();
    V1Service stageService = new V1ServiceBuilder()
                                 .withNewSpec()
                                 .withSelector(ImmutableMap.of(HarnessLabels.color, HarnessLabelValues.colorBlue))
                                 .endSpec()
                                 .build();

    when(kubernetesContainerService.getService(null, "primary-service")).thenReturn(primaryService);
    when(kubernetesContainerService.getService(null, "stage-service")).thenReturn(stageService);

    k8sBlueGreenDeployTaskHandler.prepareForBlueGreen(
        K8sBlueGreenDeployTaskParameters.builder().deprecateFabric8Enabled(true).build(), delegateTaskParams,
        executionLogCallback);

    String foundPrimaryColor = on(k8sBlueGreenDeployTaskHandler).get("primaryColor");
    assertThat(foundPrimaryColor).isEqualTo(HarnessLabelValues.colorGreen);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testMissingLabelInServiceUsing() {
    K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().build();

    List<KubernetesResource> kubernetesResources = new ArrayList<>();
    kubernetesResources.addAll(ManifestHelper.processYaml(DEPLOYMENT_YAML));
    kubernetesResources.addAll(ManifestHelper.processYaml(SERVICE_YAML));

    V1Service service = new V1ServiceBuilder()
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

    when(kubernetesContainerService.getService(null, "servicename")).thenReturn(service);

    boolean result = k8sBlueGreenDeployTaskHandler.prepareForBlueGreen(
        K8sBlueGreenDeployTaskParameters.builder().deprecateFabric8Enabled(true).build(), delegateTaskParams,
        executionLogCallback);
    assertThat(result).isFalse();

    verify(kubernetesContainerService, times(2)).getService(any(), any());
    verify(releaseHistory, times(0)).createNewRelease(any());
    verify(executionLogCallback, times(1))
        .saveExecutionLog(
            "Found conflicting service [servicename] in the cluster. For blue/green deployment, the label [harness.io/color] is required in service selector. Delete this existing service to proceed",
            ERROR, FAILURE);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testMissingLabelInServiceUsingFabric8() {
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

    when(kubernetesContainerService.getServiceFabric8(null, "servicename")).thenReturn(service);

    boolean result = k8sBlueGreenDeployTaskHandler.prepareForBlueGreen(
        K8sBlueGreenDeployTaskParameters.builder().build(), delegateTaskParams, executionLogCallback);
    assertThat(result).isFalse();

    verify(kubernetesContainerService, times(2)).getServiceFabric8(any(), any());
    verify(releaseHistory, times(0)).createNewRelease(any());
    verify(executionLogCallback, times(1))
        .saveExecutionLog(
            "Found conflicting service [servicename] in the cluster. For blue/green deployment, the label [harness.io/color] is required in service selector. Delete this existing service to proceed",
            ERROR, FAILURE);
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
    verify(k8sTaskHelperBase, times(1))
        .delete(client, delegateTaskParams, asList(kubernetesResource.getResourceId()), executionLogCallback, true);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testSupportedWorkloadsInBgWorkflow() {
    K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().build();

    List<KubernetesResource> kubernetesResources = new ArrayList<>();
    kubernetesResources.addAll(ManifestHelper.processYaml(STATEFUL_SET_YAML));

    on(k8sBlueGreenDeployTaskHandler).set("resources", kubernetesResources);

    boolean result = k8sBlueGreenDeployTaskHandler.prepareForBlueGreen(
        K8sBlueGreenDeployTaskParameters.builder().build(), delegateTaskParams, executionLogCallback);
    assertThat(result).isFalse();

    verify(executionLogCallback, times(1))
        .saveExecutionLog(
            "\nNo workload found in the Manifests. Can't do  Blue/Green Deployment. Only Deployment and DeploymentConfig (OpenShift) workloads are supported in Blue/Green workflow type.",
            ERROR, FAILURE);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void prepareIfOnlyPrimaryServiceGiven() throws IOException {
    on(k8sBlueGreenDeployTaskHandler).set("resources", new ArrayList<>(asList(primaryService(), deployment())));
    k8sBlueGreenDeployTaskHandler.prepareForBlueGreen(K8sBlueGreenDeployTaskParameters.builder().build(),
        K8sDelegateTaskParams.builder().build(), executionLogCallback);

    K8sBGStateVariables stateVariables = new K8sBGStateVariables().invoke(k8sBlueGreenDeployTaskHandler);

    assertThat(stateVariables.getResources()).hasSize(3);
    assertThat(stateVariables.getPrimaryService()).isNotNull();
    assertThat(stateVariables.getStageService()).isNotNull();
    assertThat(stateVariables.getPrimaryService()).isNotEqualTo(stateVariables.getStageService());
    assertThat(stateVariables.getStageService().getResourceId().getName()).endsWith("-stage");
    assertThat(stateVariables.getPrimaryColor()).isEqualTo(HarnessLabelValues.colorDefault);
    assertThat(stateVariables.getStageColor()).isEqualTo(HarnessLabelValues.colorBlue);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void prepareIfOnlyStageServiceGiven() throws IOException {
    on(k8sBlueGreenDeployTaskHandler).set("resources", new ArrayList<>(asList(stageService(), deployment())));
    k8sBlueGreenDeployTaskHandler.prepareForBlueGreen(K8sBlueGreenDeployTaskParameters.builder().build(),
        K8sDelegateTaskParams.builder().build(), executionLogCallback);

    K8sBGStateVariables stateVariables = new K8sBGStateVariables().invoke(k8sBlueGreenDeployTaskHandler);

    assertThat(stateVariables.getResources()).hasSize(2);
    assertThat(stateVariables.getPrimaryService()).isNotNull();
    assertThat(stateVariables.getStageService()).isNotNull();
    assert stateVariables.getStageService() == stateVariables.getPrimaryService();
    assertThat(stateVariables.getPrimaryColor()).isEqualTo(HarnessLabelValues.colorDefault);
    assertThat(stateVariables.getStageColor()).isEqualTo(HarnessLabelValues.colorBlue);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void prepareIfPrimarySecondaryServiceNotGiven() throws IOException {
    on(k8sBlueGreenDeployTaskHandler).set("resources", new ArrayList<>(asList(service(), deployment())));
    k8sBlueGreenDeployTaskHandler.prepareForBlueGreen(K8sBlueGreenDeployTaskParameters.builder().build(),
        K8sDelegateTaskParams.builder().build(), executionLogCallback);

    K8sBGStateVariables stateVariables = new K8sBGStateVariables().invoke(k8sBlueGreenDeployTaskHandler);

    assertThat(stateVariables.getResources()).hasSize(3);
    assertThat(stateVariables.getPrimaryService()).isNotNull();
    assertThat(stateVariables.getStageService()).isNotNull();
    assertThat(stateVariables.getPrimaryService()).isNotEqualTo(stateVariables.getStageService());
    assertThat(stateVariables.getStageService().getResourceId().getName()).endsWith("-stage");
    assertThat(stateVariables.getPrimaryColor()).isEqualTo(HarnessLabelValues.colorDefault);
    assertThat(stateVariables.getStageColor()).isEqualTo(HarnessLabelValues.colorBlue);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void prepareIfPrimaryServiceExistsInCluster() throws IOException {
    Service clusterPrimary = new Service();
    ServiceSpec spec = new ServiceSpec();
    spec.setSelector(ImmutableMap.of(HarnessLabels.color, "blue"));
    clusterPrimary.setSpec(spec);
    doReturn(clusterPrimary)
        .when(kubernetesContainerService)
        .getServiceFabric8(any(KubernetesConfig.class), eq(primaryService().getResourceId().getName()));
    on(k8sBlueGreenDeployTaskHandler)
        .set("resources", new ArrayList<>(asList(primaryService(), stageService(), deployment())));
    on(k8sBlueGreenDeployTaskHandler).set("releaseHistory", ReleaseHistory.createNew());
    on(k8sBlueGreenDeployTaskHandler).set("releaseName", "release-name");

    k8sBlueGreenDeployTaskHandler.prepareForBlueGreen(K8sBlueGreenDeployTaskParameters.builder().build(),
        K8sDelegateTaskParams.builder().build(), executionLogCallback);

    K8sBGStateVariables stateVariables = new K8sBGStateVariables().invoke(k8sBlueGreenDeployTaskHandler);

    assertThat(stateVariables.getResources()).hasSize(3);
    assertThat(stateVariables.getPrimaryService()).isNotNull();
    assertThat(stateVariables.getStageService()).isNotNull();
    assertThat(stateVariables.getPrimaryService()).isNotEqualTo(stateVariables.getStageService());
    assertThat(stateVariables.getPrimaryColor()).isEqualTo(HarnessLabelValues.colorBlue);
    assertThat(stateVariables.getStageColor()).isEqualTo(HarnessLabelValues.colorGreen);
    assertThat(stateVariables.getManagedWorkload().getResourceId().getName()).endsWith("-green");
    assertThat(((Map) stateVariables.getPrimaryService().getField("spec.selector")).get("harness.io/color"))
        .isEqualTo("blue");

    assertThat(((Map) stateVariables.getStageService().getField("spec.selector")).get("harness.io/color"))
        .isEqualTo("green");

    assertThat(
        ((Map) stateVariables.getManagedWorkload().getField("spec.selector.matchLabels")).get("harness.io/color"))
        .isEqualTo("green");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void moreThan1ServiceInManifest() throws IOException {
    on(k8sBlueGreenDeployTaskHandler).set("resources", new ArrayList<>(asList(service(), service(), deployment())));

    final boolean success =
        k8sBlueGreenDeployTaskHandler.prepareForBlueGreen(K8sBlueGreenDeployTaskParameters.builder().build(),
            K8sDelegateTaskParams.builder().build(), executionLogCallback);

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(executionLogCallback, atLeast(1)).saveExecutionLog(captor.capture(), eq(ERROR), eq(FAILURE));
    assertThat(success).isFalse();
    assertThat(captor.getValue()).contains("Could not locate a Primary Service in Manifests");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void noServiceInManifests() throws IOException {
    on(k8sBlueGreenDeployTaskHandler).set("resources", new ArrayList<>(asList(deployment())));

    final boolean success =
        k8sBlueGreenDeployTaskHandler.prepareForBlueGreen(K8sBlueGreenDeployTaskParameters.builder().build(),
            K8sDelegateTaskParams.builder().build(), executionLogCallback);

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(executionLogCallback, atLeast(1)).saveExecutionLog(captor.capture(), eq(ERROR), eq(FAILURE));
    assertThat(success).isFalse();
    assertThat(captor.getValue()).contains("No service is found in manifests");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void moreThan1Workload() throws IOException {
    on(k8sBlueGreenDeployTaskHandler).set("resources", new ArrayList<>(asList(deployment(), deployment())));
    assertThat(k8sBlueGreenDeployTaskHandler.prepareForBlueGreen(K8sBlueGreenDeployTaskParameters.builder().build(),
                   K8sDelegateTaskParams.builder().build(), executionLogCallback))
        .isFalse();

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(executionLogCallback, atLeast(1)).saveExecutionLog(captor.capture(), eq(ERROR), eq(FAILURE));
    assertThat(captor.getValue()).contains("There are multiple workloads in the Service Manifests you are deploying");
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void testOnlyOneWorkloadSupportedInBgWorkflow() {
    K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().build();

    List<KubernetesResource> kubernetesResources = new ArrayList<>();
    kubernetesResources.addAll(ManifestHelper.processYaml(DEPLOYMENT_YAML));
    kubernetesResources.addAll(ManifestHelper.processYaml(DEPLOYMENT_YAML));

    on(k8sBlueGreenDeployTaskHandler).set("resources", kubernetesResources);

    boolean result = k8sBlueGreenDeployTaskHandler.prepareForBlueGreen(
        K8sBlueGreenDeployTaskParameters.builder().build(), delegateTaskParams, executionLogCallback);
    assertThat(result).isFalse();

    verify(executionLogCallback, times(1))
        .saveExecutionLog(
            "\nThere are multiple workloads in the Service Manifests you are deploying. Blue/Green Workflows support a single Deployment or DeploymentConfig (OpenShift) workload only. To deploy additional workloads in Manifests, annotate them with harness.io/direct-apply: true",
            ERROR, FAILURE);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGetAllPods() throws Exception {
    KubernetesResource kubernetesResource =
        KubernetesResource.builder().resourceId(KubernetesResourceId.builder().namespace("default").build()).build();
    on(k8sBlueGreenDeployTaskHandler).set("managedWorkload", kubernetesResource);
    on(k8sBlueGreenDeployTaskHandler).set("releaseName", "releaseName");
    on(k8sBlueGreenDeployTaskHandler).set("stageColor", "stageColor");
    on(k8sBlueGreenDeployTaskHandler).set("primaryColor", "primaryColor");
    on(k8sBlueGreenDeployTaskHandler).set("isDeprecateFabric8Enabled", true);

    testGetAllPodsWithStageAndPrimary();
    testGetAllPodsWitNoPrimary();
  }

  private void testGetAllPodsWitNoPrimary() throws Exception {
    when(k8sTaskHelperBase.getPodDetailsWithColor(
             any(KubernetesConfig.class), anyString(), anyString(), eq("stageColor"), anyLong()))
        .thenReturn(asList(podWithName("stage-1"), podWithName("stage-2")));
    when(k8sTaskHelperBase.getPodDetailsWithColor(
             any(KubernetesConfig.class), anyString(), anyString(), eq("primaryColor"), anyLong()))
        .thenReturn(emptyList());

    final List<K8sPod> allPods = k8sBlueGreenDeployTaskHandler.getAllPods(LONG_TIMEOUT_INTERVAL);

    assertThat(allPods).hasSize(2);
    assertThat(allPods.stream().filter(K8sPod::isNewPod).count()).isEqualTo(2);
  }

  private void testGetAllPodsWithStageAndPrimary() throws Exception {
    when(k8sTaskHelperBase.getPodDetailsWithColor(
             any(KubernetesConfig.class), anyString(), anyString(), eq("stageColor"), anyLong()))
        .thenReturn(asList(podWithName("stage-1"), podWithName("stage-2")));
    when(k8sTaskHelperBase.getPodDetailsWithColor(
             any(KubernetesConfig.class), anyString(), anyString(), eq("primaryColor"), anyLong()))
        .thenReturn(asList(podWithName("primary-1"), podWithName("primary-2")));

    final List<K8sPod> allPods = k8sBlueGreenDeployTaskHandler.getAllPods(LONG_TIMEOUT_INTERVAL);

    assertThat(allPods).hasSize(4);
    assertThat(allPods.stream().filter(K8sPod::isNewPod).map(K8sPod::getName).collect(Collectors.toList()))
        .containsExactlyInAnyOrder("stage-1", "stage-2");
    assertThat(allPods.stream().filter(pod -> !pod.isNewPod()).map(K8sPod::getName).collect(Collectors.toList()))
        .containsExactlyInAnyOrder("primary-1", "primary-2");
  }

  private K8sPod podWithName(String name) {
    return K8sPod.builder().name(name).build();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testAssignHelmChartInfo() throws Exception {
    K8sBlueGreenDeployTaskHandler handler = spy(k8sBlueGreenDeployTaskHandler);
    K8sDelegateManifestConfig manifestConfig = K8sDelegateManifestConfig.builder()
                                                   .manifestStoreTypes(StoreType.HelmChartRepo)
                                                   .helmChartConfigParams(HelmChartConfigParams.builder().build())
                                                   .build();
    K8sBlueGreenDeployTaskParameters deployTaskParameters =
        K8sBlueGreenDeployTaskParameters.builder().k8sDelegateManifestConfig(manifestConfig).build();
    K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().workingDirectory(".").build();
    HelmChartInfo helmChartInfo = HelmChartInfo.builder().name("chart").version("1.0.0").build();

    doReturn(true)
        .when(k8sTaskHelper)
        .fetchManifestFilesAndWriteToDirectory(
            any(K8sDelegateManifestConfig.class), anyString(), any(ExecutionLogCallback.class), anyLong());
    doReturn(executionLogCallback)
        .when(k8sTaskHelper)
        .getExecutionLogCallback(any(K8sBlueGreenDeployTaskParameters.class), anyString());
    doReturn(true).when(handler).init(
        any(K8sBlueGreenDeployTaskParameters.class), any(K8sDelegateTaskParams.class), any(ExecutionLogCallback.class));
    doReturn(true).when(handler).prepareForBlueGreen(
        any(K8sBlueGreenDeployTaskParameters.class), any(K8sDelegateTaskParams.class), any(ExecutionLogCallback.class));
    doReturn(true)
        .when(k8sTaskHelperBase)
        .applyManifests(any(Kubectl.class), anyListOf(KubernetesResource.class), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class), anyBoolean());
    doReturn(true)
        .when(k8sTaskHelperBase)
        .doStatusCheck(any(Kubectl.class), any(KubernetesResourceId.class), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class));
    doReturn(helmChartInfo)
        .when(k8sTaskHelper)
        .getHelmChartDetails(manifestConfig, Paths.get(".", MANIFEST_FILES_DIR).toString());
    doAnswer(invocation
        -> K8sTaskExecutionResponse.builder()
               .k8sTaskResponse(invocation.getArgumentAt(0, K8sBlueGreenDeployResponse.class))
               .build())
        .when(k8sTaskHelper)
        .getK8sTaskExecutionResponse(any(K8sTaskResponse.class), any(CommandExecutionStatus.class));
    on(handler).set("managedWorkload", deployment());
    on(handler).set("currentRelease", new Release());
    on(handler).set("primaryService", primaryService());
    on(handler).set("stageService", stageService());
    on(handler).set("resources", new ArrayList<>(asList(primaryService(), deployment(), stageService(), configMap())));

    K8sTaskExecutionResponse response = handler.executeTask(deployTaskParameters, delegateTaskParams);
    K8sBlueGreenDeployResponse deployResponse = (K8sBlueGreenDeployResponse) response.getK8sTaskResponse();

    assertThat(deployResponse.getHelmChartInfo()).isEqualTo(helmChartInfo);
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void invalidTypeOfTaskParams() {
    assertThatExceptionOfType(InvalidArgumentsException.class)
        .isThrownBy(() -> k8sBlueGreenDeployTaskHandler.executeTaskInternal(null, null))
        .withMessageContaining("INVALID_ARGUMENT");
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testFailWorkloadStatusCheck() throws Exception {
    K8sBlueGreenDeployTaskHandler handler = spy(k8sBlueGreenDeployTaskHandler);
    K8sDelegateManifestConfig manifestConfig = K8sDelegateManifestConfig.builder()
                                                   .manifestStoreTypes(StoreType.HelmChartRepo)
                                                   .helmChartConfigParams(HelmChartConfigParams.builder().build())
                                                   .build();
    K8sBlueGreenDeployTaskParameters deployTaskParameters =
        K8sBlueGreenDeployTaskParameters.builder().k8sDelegateManifestConfig(manifestConfig).build();
    K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().workingDirectory(".").build();

    doReturn(true)
        .when(k8sTaskHelper)
        .fetchManifestFilesAndWriteToDirectory(
            any(K8sDelegateManifestConfig.class), anyString(), any(ExecutionLogCallback.class), anyLong());
    doReturn(executionLogCallback)
        .when(k8sTaskHelper)
        .getExecutionLogCallback(any(K8sBlueGreenDeployTaskParameters.class), anyString());
    doReturn(true).when(handler).init(
        any(K8sBlueGreenDeployTaskParameters.class), any(K8sDelegateTaskParams.class), any(ExecutionLogCallback.class));
    doReturn(true).when(handler).prepareForBlueGreen(
        any(K8sBlueGreenDeployTaskParameters.class), any(K8sDelegateTaskParams.class), any(ExecutionLogCallback.class));
    doReturn(true)
        .when(k8sTaskHelperBase)
        .applyManifests(any(Kubectl.class), anyListOf(KubernetesResource.class), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class), anyBoolean());
    doReturn(false)
        .when(k8sTaskHelperBase)
        .doStatusCheck(any(Kubectl.class), any(KubernetesResourceId.class), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class));

    doAnswer(invocation
        -> K8sTaskExecutionResponse.builder()
               .k8sTaskResponse(invocation.getArgumentAt(0, K8sBlueGreenDeployResponse.class))
               .build())
        .when(k8sTaskHelper)
        .getK8sTaskExecutionResponse(any(K8sTaskResponse.class), any(CommandExecutionStatus.class));
    on(handler).set("managedWorkload", deployment());
    on(handler).set("currentRelease", new Release());
    on(handler).set("primaryService", primaryService());
    on(handler).set("stageService", stageService());
    on(handler).set("resources", new ArrayList<>(asList(primaryService(), deployment(), stageService(), configMap())));

    K8sTaskExecutionResponse response = handler.executeTask(deployTaskParameters, delegateTaskParams);
    assertThat(FAILURE).isEqualTo(response.getCommandExecutionStatus());
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void shouldFetchReleaseDataUsingFabric8() throws Exception {
    K8sBlueGreenDeployTaskParameters blueGreenDeployTaskParams =
        K8sBlueGreenDeployTaskParameters.builder().skipDryRun(true).build();
    K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().build();
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback();

    when(containerDeploymentDelegateHelper.getKubernetesConfig(any(K8sClusterConfig.class), anyBoolean()))
        .thenReturn(KubernetesConfig.builder().build());
    doNothing().when(k8sTaskHelperBase).deleteSkippedManifestFiles(any(), any());
    when(k8sTaskHelperBase.getReleaseHistoryDataFromConfigMap(any(), any(), anyBoolean())).thenReturn(null);
    when(k8sTaskHelper.renderTemplate(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(emptyList());
    doNothing().when(k8sTaskHelperBase).setNamespaceToKubernetesResourcesIfRequired(any(), any());
    when(k8sTaskHelperBase.readManifests(any(), any())).thenReturn(emptyList());

    k8sBlueGreenDeployTaskHandler.init(blueGreenDeployTaskParams, delegateTaskParams, executionLogCallback);
    verify(k8sTaskHelperBase, times(1)).getReleaseHistoryDataFromConfigMap(any(), any(), eq(false));
    verify(k8sTaskHelperBase, times(0)).getReleaseHistoryDataFromConfigMap(any(), any(), eq(true));
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void shouldFetchReleaseDataUsingK8sClient() throws Exception {
    K8sBlueGreenDeployTaskParameters blueGreenDeployTaskParams =
        K8sBlueGreenDeployTaskParameters.builder().deprecateFabric8Enabled(true).skipDryRun(true).build();
    K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().build();
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback();

    when(containerDeploymentDelegateHelper.getKubernetesConfig(any(K8sClusterConfig.class), anyBoolean()))
        .thenReturn(KubernetesConfig.builder().build());
    doNothing().when(k8sTaskHelperBase).deleteSkippedManifestFiles(any(), any());
    when(k8sTaskHelperBase.getReleaseHistoryDataFromConfigMap(any(), any(), anyBoolean())).thenReturn(null);
    when(k8sTaskHelper.renderTemplate(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(emptyList());
    doNothing().when(k8sTaskHelperBase).setNamespaceToKubernetesResourcesIfRequired(any(), any());
    when(k8sTaskHelperBase.readManifests(any(), any())).thenReturn(emptyList());

    k8sBlueGreenDeployTaskHandler.init(blueGreenDeployTaskParams, delegateTaskParams, executionLogCallback);
    verify(k8sTaskHelperBase, times(1)).getReleaseHistoryDataFromConfigMap(any(), any(), eq(true));
    verify(k8sTaskHelperBase, times(0)).getReleaseHistoryDataFromConfigMap(any(), any(), eq(false));
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void shouldSaveReleaseHistoryUsingK8sClient() throws Exception {
    K8sBlueGreenDeployTaskHandler spyHandler = spy(k8sBlueGreenDeployTaskHandler);
    doReturn(true)
        .when(k8sTaskHelper)
        .fetchManifestFilesAndWriteToDirectory(
            any(K8sDelegateManifestConfig.class), anyString(), any(ExecutionLogCallback.class), anyLong());
    doReturn(executionLogCallback)
        .when(k8sTaskHelper)
        .getExecutionLogCallback(any(K8sBlueGreenDeployTaskParameters.class), anyString());
    doReturn(true)
        .when(spyHandler)
        .init(any(K8sBlueGreenDeployTaskParameters.class), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class));
    doReturn(true)
        .when(spyHandler)
        .prepareForBlueGreen(any(K8sBlueGreenDeployTaskParameters.class), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class));
    on(spyHandler).set("managedWorkload", deployment());
    on(spyHandler).set("currentRelease", new Release());
    on(spyHandler).set("primaryService", primaryService());
    on(spyHandler).set("stageService", stageService());
    doReturn(true)
        .when(k8sTaskHelperBase)
        .applyManifests(any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class), anyBoolean());
    doReturn(true)
        .when(k8sTaskHelperBase)
        .doStatusCheck(any(Kubectl.class), any(KubernetesResourceId.class), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class));
    on(spyHandler)
        .set("resources", new ArrayList<>(asList(primaryService(), deployment(), stageService(), configMap())));
    doReturn("latest-rev")
        .when(k8sTaskHelperBase)
        .getLatestRevision(any(Kubectl.class), eq(deployment().getResourceId()), any(K8sDelegateTaskParams.class));

    spyHandler.executeTaskInternal(K8sBlueGreenDeployTaskParameters.builder()
                                       .releaseName("release-success")
                                       .deprecateFabric8Enabled(true)
                                       .k8sTaskType(K8sTaskType.BLUE_GREEN_DEPLOY)
                                       .build(),
        K8sDelegateTaskParams.builder()
            .workingDirectory("./working-dir")
            .kubectlPath("kubectl")
            .kubeconfigPath("kubeconfig")
            .build());

    verify(k8sTaskHelper, times(1)).getK8sTaskExecutionResponse(any(K8sBlueGreenDeployResponse.class), eq(SUCCESS));
    verify(k8sTaskHelperBase, times(2))
        .saveReleaseHistoryInConfigMap(any(KubernetesConfig.class), eq("release-success"), anyString(), eq(true));
    verify(k8sTaskHelperBase, times(0))
        .saveReleaseHistoryInConfigMap(any(KubernetesConfig.class), eq("release-success"), anyString(), eq(false));

    K8sBlueGreenDeployTaskParameters deployTaskParams = K8sBlueGreenDeployTaskParameters.builder()
                                                            .deprecateFabric8Enabled(true)
                                                            .releaseName("releaseName-statusCheck")
                                                            .build();
    K8sDelegateTaskParams taskParams = K8sDelegateTaskParams.builder().build();

    doReturn(false)
        .when(k8sTaskHelperBase)
        .doStatusCheck(any(Kubectl.class), any(KubernetesResourceId.class), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class));
    K8sTaskExecutionResponse response = spyHandler.executeTaskInternal(deployTaskParams, taskParams);
    assertThat(response.getCommandExecutionStatus()).isEqualTo(FAILURE);
    verify(k8sTaskHelperBase, times(2))
        .saveReleaseHistoryInConfigMap(
            any(KubernetesConfig.class), eq("releaseName-statusCheck"), anyString(), eq(true));
    verify(k8sTaskHelperBase, times(0))
        .saveReleaseHistoryInConfigMap(
            any(KubernetesConfig.class), eq("releaseName-statusCheck"), anyString(), eq(false));

    deployTaskParams.setReleaseName("releaseName-apply");
    doReturn(false)
        .when(k8sTaskHelperBase)
        .applyManifests(any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class), anyBoolean());
    response = spyHandler.executeTaskInternal(deployTaskParams, taskParams);
    assertThat(response.getCommandExecutionStatus()).isEqualTo(FAILURE);
    verify(k8sTaskHelperBase)
        .saveReleaseHistoryInConfigMap(any(KubernetesConfig.class), eq("releaseName-apply"), anyString(), eq(true));
    verify(k8sTaskHelperBase, times(0))
        .saveReleaseHistoryInConfigMap(any(KubernetesConfig.class), eq("releaseName-apply"), anyString(), eq(false));
  }

  @Data
  private class K8sBGStateVariables {
    private List<KubernetesResource> resources;
    private KubernetesResource primaryService;
    private KubernetesResource stageService;
    private KubernetesResource managedWorkload;
    private String primaryColor;
    private String stageColor;

    public K8sBGStateVariables invoke(K8sBlueGreenDeployTaskHandler handler) {
      resources = on(handler).get("resources");
      primaryService = on(handler).get("primaryService");
      stageService = on(handler).get("stageService");
      managedWorkload = on(handler).get("managedWorkload");
      primaryColor = on(handler).get("primaryColor");
      stageColor = on(handler).get("stageColor");
      return this;
    }
  }
}
