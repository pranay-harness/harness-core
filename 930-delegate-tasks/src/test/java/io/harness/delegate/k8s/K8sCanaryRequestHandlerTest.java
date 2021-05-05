package io.harness.delegate.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.delegate.k8s.K8sTestHelper.deployment;
import static io.harness.k8s.K8sConstants.MANIFEST_FILES_DIR;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.ABOSII;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FileData;
import io.harness.beans.NGInstanceUnitType;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.k8s.beans.K8sCanaryHandlerConfig;
import io.harness.delegate.k8s.exception.KubernetesExceptionHints;
import io.harness.delegate.task.k8s.ContainerDeploymentDelegateBaseHelper;
import io.harness.delegate.task.k8s.K8sCanaryDeployRequest;
import io.harness.delegate.task.k8s.K8sCanaryDeployRequest.K8sCanaryDeployRequestBuilder;
import io.harness.delegate.task.k8s.K8sCanaryDeployResponse;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sInfraDelegateConfig;
import io.harness.delegate.task.k8s.K8sTaskHelperBase;
import io.harness.delegate.task.k8s.ManifestDelegateConfig;
import io.harness.delegate.task.k8s.exception.K8sCanaryDataException;
import io.harness.exception.InvalidArgumentsException;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.manifest.ManifestHelper;
import io.harness.k8s.model.K8sDelegateTaskParams;
import io.harness.k8s.model.K8sPod;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.k8s.model.Release;
import io.harness.k8s.model.ReleaseHistory;
import io.harness.logging.LogCallback;
import io.harness.rule.Owner;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(CDP)
public class K8sCanaryRequestHandlerTest extends CategoryTest {
  @Mock private K8sTaskHelperBase k8sTaskHelperBase;
  @Mock private K8sCanaryBaseHandler k8sCanaryBaseHandler;
  @Mock private ContainerDeploymentDelegateBaseHelper containerDeploymentDelegateBaseHelper;

  @InjectMocks private K8sCanaryRequestHandler k8sCanaryRequestHandler;

  @Mock ILogStreamingTaskClient iLogStreamingTaskClient;
  @Mock private LogCallback logCallback;
  @Mock private K8sInfraDelegateConfig k8sInfraDelegateConfig;
  @Mock private ManifestDelegateConfig manifestDelegateConfig;

  private final Integer timeoutIntervalInMin = 10;
  private final long timeoutIntervalInMillis = 60 * timeoutIntervalInMin * 1000;
  private final String accountId = "accountId";
  private final String namespace = "default";
  private final KubernetesConfig kubernetesConfig = KubernetesConfig.builder().namespace(namespace).build();
  private final String workingDirectory = "manifest";
  private final String invalidWorkingDirectory = "invalid";
  private final String manifestFileDirectory = Paths.get(workingDirectory, MANIFEST_FILES_DIR).toString();
  private final String invalidManifestFileDirectory = Paths.get(invalidWorkingDirectory, MANIFEST_FILES_DIR).toString();

  private K8sCanaryHandlerConfig k8sCanaryHandlerConfig;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    doReturn(kubernetesConfig)
        .when(containerDeploymentDelegateBaseHelper)
        .createKubernetesConfig(k8sInfraDelegateConfig);
    doReturn(logCallback)
        .when(k8sTaskHelperBase)
        .getLogCallback(eq(iLogStreamingTaskClient), anyString(), anyBoolean(), any());
    doReturn(true)
        .when(k8sTaskHelperBase)
        .fetchManifestFilesAndWriteToDirectory(
            manifestDelegateConfig, manifestFileDirectory, logCallback, timeoutIntervalInMillis, accountId, false);
    doReturn(false)
        .when(k8sTaskHelperBase)
        .fetchManifestFilesAndWriteToDirectory(manifestDelegateConfig, invalidManifestFileDirectory, logCallback,
            timeoutIntervalInMillis, accountId, false);
    doReturn(true)
        .when(k8sTaskHelperBase)
        .applyManifests(any(Kubectl.class), anyListOf(KubernetesResource.class), any(K8sDelegateTaskParams.class),
            eq(logCallback), anyBoolean(), eq(false));
    k8sCanaryHandlerConfig = k8sCanaryRequestHandler.getK8sCanaryHandlerConfig();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testDryRunIsSkipped() throws Exception {
    testDryRun(true);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testDryRunIsNotSkipped() throws Exception {
    testDryRun(false);
  }

  public void testDryRun(boolean skipDryRun) throws Exception {
    String releaseName = "releaseName";
    List<String> valuesYamlList = emptyList();
    List<String> openshiftParamList = emptyList();
    List<FileData> manifestFiles = emptyList();
    List<KubernetesResource> kubernetesResources = emptyList();
    K8sCanaryDeployRequest k8sCanaryDeployRequest = K8sCanaryDeployRequest.builder()
                                                        .releaseName(releaseName)
                                                        .k8sInfraDelegateConfig(k8sInfraDelegateConfig)
                                                        .manifestDelegateConfig(manifestDelegateConfig)
                                                        .valuesYamlList(valuesYamlList)
                                                        .openshiftParamList(openshiftParamList)
                                                        .skipDryRun(skipDryRun)
                                                        .timeoutIntervalInMin(timeoutIntervalInMin)
                                                        .build();
    K8sDelegateTaskParams delegateTaskParams =
        K8sDelegateTaskParams.builder().workingDirectory(workingDirectory).build();

    when(k8sTaskHelperBase.renderTemplate(delegateTaskParams, manifestDelegateConfig, manifestFileDirectory,
             valuesYamlList, releaseName, namespace, logCallback, timeoutIntervalInMin))
        .thenReturn(manifestFiles);

    when(k8sTaskHelperBase.readManifests(manifestFiles, logCallback)).thenReturn(kubernetesResources);
    when(k8sTaskHelperBase.getReleaseHistoryDataFromConfigMap(kubernetesConfig, releaseName)).thenReturn(null);
    doNothing().when(k8sTaskHelperBase).deleteSkippedManifestFiles(null, logCallback);
    doReturn(true)
        .when(k8sTaskHelperBase)
        .dryRunManifests(
            any(Kubectl.class), anyListOf(KubernetesResource.class), eq(delegateTaskParams), eq(logCallback));

    k8sCanaryRequestHandler.init(k8sCanaryDeployRequest, delegateTaskParams, logCallback);
    int wantedDryRunInvocations = skipDryRun ? 0 : 1;
    verify(k8sTaskHelperBase, times(wantedDryRunInvocations)).dryRunManifests(any(), any(), any(), any());
    verify(k8sTaskHelperBase, times(1)).readManifests(manifestFiles, logCallback);
    verify(k8sTaskHelperBase, times(1))
        .renderTemplate(delegateTaskParams, manifestDelegateConfig, null, valuesYamlList, releaseName, namespace,
            logCallback, timeoutIntervalInMin);
    verify(k8sTaskHelperBase, times(1)).deleteSkippedManifestFiles(null, logCallback);
    verify(k8sTaskHelperBase, times(1)).getReleaseHistoryDataFromConfigMap(kubernetesConfig, releaseName);
    verify(k8sCanaryBaseHandler, times(1))
        .updateDestinationRuleManifestFilesWithSubsets(kubernetesResources, kubernetesConfig, logCallback);
    verify(k8sCanaryBaseHandler, times(1))
        .updateVirtualServiceManifestFilesWithRoutes(kubernetesResources, kubernetesConfig, logCallback);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void invalidTypeOfTaskParams() {
    assertThatExceptionOfType(InvalidArgumentsException.class)
        .isThrownBy(() -> k8sCanaryRequestHandler.executeTaskInternal(null, null, null, null))
        .withMessageContaining("INVALID_ARGUMENT");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void failureInFetchingManifestFiles() throws Exception {
    K8sCanaryDeployRequest k8sCanaryDeployRequest = K8sCanaryDeployRequest.builder()
                                                        .manifestDelegateConfig(manifestDelegateConfig)
                                                        .accountId(accountId)
                                                        .timeoutIntervalInMin(timeoutIntervalInMin)
                                                        .releaseName("releaseName")
                                                        .build();

    try {
      k8sCanaryRequestHandler.executeTask(k8sCanaryDeployRequest,
          K8sDelegateTaskParams.builder().workingDirectory(invalidWorkingDirectory).build(), iLogStreamingTaskClient,
          null);
    } catch (Exception ex) {
      Exception enhancedException = k8sCanaryRequestHandler.handleTaskFailure(k8sCanaryDeployRequest, ex);
      assertThat(enhancedException).isInstanceOf(K8sCanaryDataException.class);
      assertThat(((K8sCanaryDataException) enhancedException).getCanaryWorkload()).isNull();
    }

    k8sCanaryHandlerConfig.setCanaryWorkload(
        KubernetesResource.builder()
            .resourceId(KubernetesResourceId.builder().namespace("default").name("canary").kind("Deployment").build())
            .build());
    try {
      k8sCanaryRequestHandler.executeTask(k8sCanaryDeployRequest,
          K8sDelegateTaskParams.builder().workingDirectory(".").build(), iLogStreamingTaskClient, null);
    } catch (Exception ex) {
      Exception enhancedException = k8sCanaryRequestHandler.handleTaskFailure(k8sCanaryDeployRequest, ex);
      assertThat(enhancedException).isInstanceOf(K8sCanaryDataException.class);
      assertThat(((K8sCanaryDataException) enhancedException).getCanaryWorkload())
          .isEqualTo("default/Deployment/canary");
    }
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testExecute() throws Exception {
    String releaseName = "releaseName";
    K8sCanaryDeployRequest canaryDeployRequest = K8sCanaryDeployRequest.builder()
                                                     .releaseName(releaseName)
                                                     .timeoutIntervalInMin(timeoutIntervalInMin)
                                                     .manifestDelegateConfig(manifestDelegateConfig)
                                                     .accountId(accountId)
                                                     .build();
    K8sDelegateTaskParams delegateTaskParams =
        K8sDelegateTaskParams.builder().workingDirectory(workingDirectory).build();
    K8sCanaryRequestHandler spyRequestHandler = spy(k8sCanaryRequestHandler);
    K8sCanaryHandlerConfig k8sCanaryHandlerConfig = spyRequestHandler.getK8sCanaryHandlerConfig();

    ReleaseHistory releaseHistory = ReleaseHistory.createNew();
    releaseHistory.setReleases(asList(Release.builder().number(2).build()));
    k8sCanaryHandlerConfig.setCanaryWorkload(ManifestHelper.processYaml(K8sTestConstants.DEPLOYMENT_YAML).get(0));
    k8sCanaryHandlerConfig.setResources(Collections.emptyList());
    k8sCanaryHandlerConfig.setReleaseHistory(releaseHistory);
    k8sCanaryHandlerConfig.setCurrentRelease(releaseHistory.getLatestRelease());
    k8sCanaryHandlerConfig.setTargetInstances(3);
    k8sCanaryHandlerConfig.setKubernetesConfig(kubernetesConfig);

    doNothing().when(spyRequestHandler).init(canaryDeployRequest, delegateTaskParams, logCallback);
    doNothing().when(spyRequestHandler).prepareForCanary(canaryDeployRequest, delegateTaskParams, logCallback);
    doReturn(true)
        .when(k8sTaskHelperBase)
        .doStatusCheck(any(Kubectl.class), eq(k8sCanaryHandlerConfig.getCanaryWorkload().getResourceId()),
            eq(delegateTaskParams), eq(logCallback), eq(false));
    doReturn(Arrays.asList(K8sPod.builder().build()))
        .when(k8sCanaryBaseHandler)
        .getAllPods(k8sCanaryHandlerConfig, releaseName, timeoutIntervalInMillis);

    K8sDeployResponse k8sDeployResponse =
        spyRequestHandler.executeTask(canaryDeployRequest, delegateTaskParams, iLogStreamingTaskClient, null);
    verify(k8sCanaryBaseHandler, times(1)).wrapUp(any(Kubectl.class), eq(delegateTaskParams), eq(logCallback));
    verify(k8sTaskHelperBase, times(1))
        .saveReleaseHistoryInConfigMap(kubernetesConfig, releaseName, releaseHistory.getAsYaml());
    K8sCanaryDeployResponse canaryDeployResponse = (K8sCanaryDeployResponse) k8sDeployResponse.getK8sNGTaskResponse();
    assertThat(k8sDeployResponse.getCommandExecutionStatus()).isEqualTo(SUCCESS);
    assertThat(canaryDeployResponse.getCanaryWorkload()).isEqualTo("Deployment/deployment");
    assertThat(canaryDeployResponse.getCurrentInstances()).isEqualTo(3);
    assertThat(canaryDeployResponse.getReleaseNumber()).isEqualTo(2);
    assertThat(canaryDeployResponse.getK8sPodList()).hasSize(1);

    // do status check fails
    doReturn(false)
        .when(k8sTaskHelperBase)
        .doStatusCheck(any(Kubectl.class), eq(k8sCanaryHandlerConfig.getCanaryWorkload().getResourceId()),
            eq(delegateTaskParams), eq(logCallback), eq(false));

    assertThatThrownBy(
        () -> spyRequestHandler.executeTask(canaryDeployRequest, delegateTaskParams, iLogStreamingTaskClient, null))
        .hasMessage(KubernetesExceptionHints.WAIT_FOR_STEADY_STATE_FAILED);

    // apply manifests fails
    doReturn(false)
        .when(k8sTaskHelperBase)
        .applyManifests(
            any(Kubectl.class), eq(emptyList()), eq(delegateTaskParams), eq(logCallback), eq(true), eq(false));

    assertThatThrownBy(
        () -> spyRequestHandler.executeTask(canaryDeployRequest, delegateTaskParams, iLogStreamingTaskClient, null))
        .hasMessage(KubernetesExceptionHints.APPLY_MANIFEST_FAILED);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testInit() throws Exception {
    String releaseName = "releaseName";
    List<KubernetesResource> deployment = ManifestHelper.processYaml(K8sTestConstants.DEPLOYMENT_YAML);
    List<FileData> manifestFiles = emptyList();
    List<String> valuesYamlList = emptyList();
    List<String> openshiftParamList = emptyList();
    K8sCanaryDeployRequest canaryDeployRequest = K8sCanaryDeployRequest.builder()
                                                     .timeoutIntervalInMin(timeoutIntervalInMin)
                                                     .releaseName(releaseName)
                                                     .manifestDelegateConfig(manifestDelegateConfig)
                                                     .k8sInfraDelegateConfig(k8sInfraDelegateConfig)
                                                     .valuesYamlList(valuesYamlList)
                                                     .openshiftParamList(openshiftParamList)
                                                     .build();
    K8sDelegateTaskParams delegateTaskParams =
        K8sDelegateTaskParams.builder().workingDirectory(workingDirectory).build();
    k8sCanaryRequestHandler.getK8sCanaryHandlerConfig().setManifestFilesDirectory(manifestFileDirectory);

    doReturn(manifestFiles)
        .when(k8sTaskHelperBase)
        .renderTemplate(delegateTaskParams, manifestDelegateConfig, manifestFileDirectory, valuesYamlList, releaseName,
            namespace, logCallback, timeoutIntervalInMin);
    doReturn(deployment).when(k8sTaskHelperBase).readManifests(manifestFiles, logCallback);
    doReturn(true)
        .when(k8sTaskHelperBase)
        .dryRunManifests(any(), eq(deployment), eq(delegateTaskParams), eq(logCallback));
    k8sCanaryRequestHandler.init(canaryDeployRequest, delegateTaskParams, logCallback);

    verify(k8sTaskHelperBase, times(1)).deleteSkippedManifestFiles(manifestFileDirectory, logCallback);
    verify(k8sTaskHelperBase, times(1))
        .renderTemplate(delegateTaskParams, manifestDelegateConfig, manifestFileDirectory, valuesYamlList, releaseName,
            namespace, logCallback, timeoutIntervalInMin);
    verify(k8sTaskHelperBase, times(1)).setNamespaceToKubernetesResourcesIfRequired(deployment, "default");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testInitException() throws Exception {
    String releaseName = "releaseName";
    List<String> valuesYamlList = singletonList("value");
    K8sCanaryDeployRequest canaryDeployRequest = K8sCanaryDeployRequest.builder()
                                                     .timeoutIntervalInMin(timeoutIntervalInMin)
                                                     .releaseName(releaseName)
                                                     .manifestDelegateConfig(manifestDelegateConfig)
                                                     .valuesYamlList(valuesYamlList)
                                                     .k8sInfraDelegateConfig(k8sInfraDelegateConfig)
                                                     .build();
    K8sDelegateTaskParams delegateTaskParams =
        K8sDelegateTaskParams.builder().workingDirectory(workingDirectory).build();
    RuntimeException thrownException = new RuntimeException();

    doThrow(thrownException)
        .when(k8sTaskHelperBase)
        .renderTemplate(delegateTaskParams, manifestDelegateConfig, manifestFileDirectory, valuesYamlList, releaseName,
            namespace, logCallback, timeoutIntervalInMin);
    k8sCanaryRequestHandler.getK8sCanaryHandlerConfig().setManifestFilesDirectory(manifestFileDirectory);

    assertThatThrownBy(() -> k8sCanaryRequestHandler.init(canaryDeployRequest, delegateTaskParams, logCallback))
        .isEqualTo(thrownException);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testPrepareForCanaryCount() throws Exception {
    K8sCanaryHandlerConfig canaryHandlerConfig = k8sCanaryRequestHandler.getK8sCanaryHandlerConfig();
    K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().build();
    K8sCanaryDeployRequest deployRequest = K8sCanaryDeployRequest.builder()
                                               .instanceUnitType(NGInstanceUnitType.COUNT)
                                               .instances(4)
                                               .skipResourceVersioning(true)
                                               .build();
    doReturn(true)
        .when(k8sCanaryBaseHandler)
        .prepareForCanary(canaryHandlerConfig, delegateTaskParams, true, logCallback, true);
    doReturn(1).when(k8sCanaryBaseHandler).getCurrentInstances(canaryHandlerConfig, delegateTaskParams, logCallback);

    k8sCanaryRequestHandler.prepareForCanary(deployRequest, delegateTaskParams, logCallback);
    verify(k8sCanaryBaseHandler, times(1)).updateTargetInstances(canaryHandlerConfig, 4, logCallback);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testPrepareForCanaryPercentage() throws Exception {
    Integer currentInstances = 4;
    K8sCanaryHandlerConfig k8sCanaryHandlerConfig = k8sCanaryRequestHandler.getK8sCanaryHandlerConfig();
    K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().build();
    K8sCanaryDeployRequest deployRequest =
        K8sCanaryDeployRequest.builder().instanceUnitType(NGInstanceUnitType.PERCENTAGE).instances(70).build();
    doReturn(true)
        .when(k8sCanaryBaseHandler)
        .prepareForCanary(k8sCanaryHandlerConfig, delegateTaskParams, false, logCallback, true);
    doReturn(currentInstances)
        .when(k8sCanaryBaseHandler)
        .getCurrentInstances(k8sCanaryHandlerConfig, delegateTaskParams, logCallback);
    doReturn(3).when(k8sTaskHelperBase).getTargetInstancesForCanary(70, currentInstances, logCallback);

    k8sCanaryRequestHandler.prepareForCanary(deployRequest, delegateTaskParams, logCallback);
    verify(k8sTaskHelperBase, times(1)).getTargetInstancesForCanary(70, currentInstances, logCallback);
    verify(k8sCanaryBaseHandler, times(1)).updateTargetInstances(k8sCanaryHandlerConfig, 3, logCallback);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void shouldGetReleaseDataFromConfigMapUsingK8sClient() throws Exception {
    final String releaseName = "releaseName";
    final K8sCanaryDeployRequest deployRequest = K8sCanaryDeployRequest.builder()
                                                     .valuesYamlList(emptyList())
                                                     .releaseName(releaseName)
                                                     .k8sInfraDelegateConfig(k8sInfraDelegateConfig)
                                                     .skipDryRun(true)
                                                     .build();
    final K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().build();

    when(k8sTaskHelperBase.renderTemplate(delegateTaskParams, manifestDelegateConfig, manifestFileDirectory,
             emptyList(), releaseName, namespace, logCallback, timeoutIntervalInMin))
        .thenReturn(emptyList());
    when(k8sTaskHelperBase.readManifests(emptyList(), logCallback)).thenReturn(emptyList());
    when(k8sTaskHelperBase.getReleaseHistoryDataFromConfigMap(kubernetesConfig, releaseName)).thenReturn(null);

    k8sCanaryRequestHandler.init(deployRequest, delegateTaskParams, logCallback);
    verify(k8sTaskHelperBase, times(1)).getReleaseHistoryDataFromConfigMap(kubernetesConfig, releaseName);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void shouldSaveReleaseHistoryUsingK8sClient() throws Exception {
    final KubernetesResource deployment = ManifestHelper.processYaml(K8sTestConstants.DEPLOYMENT_YAML).get(0);
    final K8sCanaryDeployRequestBuilder deployRequestBase = K8sCanaryDeployRequest.builder()
                                                                .valuesYamlList(emptyList())
                                                                .timeoutIntervalInMin(timeoutIntervalInMin)
                                                                .accountId(accountId)
                                                                .manifestDelegateConfig(manifestDelegateConfig)
                                                                .k8sInfraDelegateConfig(k8sInfraDelegateConfig);

    final K8sDelegateTaskParams delegateTaskParams =
        K8sDelegateTaskParams.builder().workingDirectory(workingDirectory).build();
    final K8sCanaryDeployRequest successDeployRequest = deployRequestBase.releaseName("success").build();
    K8sCanaryRequestHandler spyCanaryRequestHandler = spy(k8sCanaryRequestHandler);
    K8sCanaryHandlerConfig handlerConfig = spyCanaryRequestHandler.getK8sCanaryHandlerConfig();
    doNothing()
        .when(spyCanaryRequestHandler)
        .init(any(K8sCanaryDeployRequest.class), eq(delegateTaskParams), eq(logCallback));
    doNothing()
        .when(spyCanaryRequestHandler)
        .prepareForCanary(any(K8sCanaryDeployRequest.class), eq(delegateTaskParams), eq(logCallback));
    doReturn(true)
        .when(k8sTaskHelperBase)
        .doStatusCheck(
            any(Kubectl.class), eq(deployment.getResourceId()), eq(delegateTaskParams), eq(logCallback), eq(false));

    handlerConfig.setCanaryWorkload(deployment);
    handlerConfig.setResources(emptyList());
    handlerConfig.setKubernetesConfig(kubernetesConfig);
    ReleaseHistory releaseHist = ReleaseHistory.createNew();
    releaseHist.setReleases(asList(Release.builder().number(2).build()));
    handlerConfig.setReleaseHistory(releaseHist);
    handlerConfig.setCurrentRelease(releaseHist.getLatestRelease());
    handlerConfig.setTargetInstances(3);

    spyCanaryRequestHandler.executeTask(successDeployRequest, delegateTaskParams, iLogStreamingTaskClient, null);
    verify(k8sTaskHelperBase, times(1))
        .saveReleaseHistoryInConfigMap(kubernetesConfig, "success", releaseHist.getAsYaml());

    doReturn(false)
        .when(k8sTaskHelperBase)
        .doStatusCheck(
            any(Kubectl.class), eq(deployment.getResourceId()), eq(delegateTaskParams), eq(logCallback), eq(false));
    K8sCanaryDeployRequest statusCheckFailRequest = deployRequestBase.releaseName("failStatusCheck").build();

    try {
      spyCanaryRequestHandler.executeTask(statusCheckFailRequest, delegateTaskParams, iLogStreamingTaskClient, null);
    } catch (Exception e) {
      assertThat(e).hasMessage(KubernetesExceptionHints.WAIT_FOR_STEADY_STATE_FAILED);
      assertThat(spyCanaryRequestHandler.handleTaskFailure(statusCheckFailRequest, e))
          .isInstanceOf(K8sCanaryDataException.class);
    }

    verify(k8sCanaryBaseHandler, times(1)).failAndSaveKubernetesRelease(handlerConfig, "failStatusCheck");

    doReturn(false)
        .when(k8sTaskHelperBase)
        .applyManifests(
            any(Kubectl.class), eq(emptyList()), eq(delegateTaskParams), eq(logCallback), eq(true), eq(false));
    K8sCanaryDeployRequest applyManifestFailRequest = deployRequestBase.releaseName("failApplyManifest").build();

    try {
      spyCanaryRequestHandler.executeTask(applyManifestFailRequest, delegateTaskParams, iLogStreamingTaskClient, null);
    } catch (Exception e) {
      assertThat(e).hasMessage(KubernetesExceptionHints.APPLY_MANIFEST_FAILED);
      assertThat(spyCanaryRequestHandler.handleTaskFailure(applyManifestFailRequest, e))
          .isInstanceOf(K8sCanaryDataException.class);
    }

    verify(k8sCanaryBaseHandler, times(1)).failAndSaveKubernetesRelease(handlerConfig, "failApplyManifest");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testCanaryWorkloadDeployedFlagPreApplyManifest() throws Exception {
    final K8sCanaryRequestHandler spyCanaryRequestHandler = spy(k8sCanaryRequestHandler);
    final K8sCanaryDeployRequest request = K8sCanaryDeployRequest.builder()
                                               .valuesYamlList(emptyList())
                                               .timeoutIntervalInMin(timeoutIntervalInMin)
                                               .accountId(accountId)
                                               .manifestDelegateConfig(manifestDelegateConfig)
                                               .k8sInfraDelegateConfig(k8sInfraDelegateConfig)
                                               .releaseName("releaseName")
                                               .build();
    final K8sDelegateTaskParams delegateTaskParams =
        K8sDelegateTaskParams.builder().workingDirectory(workingDirectory).build();

    doNothing()
        .when(spyCanaryRequestHandler)
        .init(any(K8sCanaryDeployRequest.class), eq(delegateTaskParams), eq(logCallback));
    doThrow(new RuntimeException())
        .when(spyCanaryRequestHandler)
        .prepareForCanary(any(K8sCanaryDeployRequest.class), eq(delegateTaskParams), eq(logCallback));

    try {
      spyCanaryRequestHandler.executeTask(request, delegateTaskParams, iLogStreamingTaskClient, null);
    } catch (Exception ex) {
      Exception enhancedException = spyCanaryRequestHandler.handleTaskFailure(request, ex);
      assertThat(enhancedException).isInstanceOf(K8sCanaryDataException.class);
      assertThat(((K8sCanaryDataException) enhancedException).isCanaryWorkloadDeployed()).isFalse();
    }
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testCanaryWorkloadDeployedFlagPostApplyManifest() throws Exception {
    final K8sCanaryRequestHandler spyCanaryRequestHandler = spy(k8sCanaryRequestHandler);
    final K8sCanaryDeployRequest request = K8sCanaryDeployRequest.builder()
                                               .valuesYamlList(emptyList())
                                               .timeoutIntervalInMin(timeoutIntervalInMin)
                                               .accountId(accountId)
                                               .manifestDelegateConfig(manifestDelegateConfig)
                                               .k8sInfraDelegateConfig(k8sInfraDelegateConfig)
                                               .releaseName("releaseName")
                                               .build();

    spyCanaryRequestHandler.getK8sCanaryHandlerConfig().setCanaryWorkload(deployment());
    final K8sDelegateTaskParams delegateTaskParams =
        K8sDelegateTaskParams.builder().workingDirectory(workingDirectory).build();

    doNothing()
        .when(spyCanaryRequestHandler)
        .init(any(K8sCanaryDeployRequest.class), eq(delegateTaskParams), eq(logCallback));
    doNothing()
        .when(spyCanaryRequestHandler)
        .prepareForCanary(any(K8sCanaryDeployRequest.class), eq(delegateTaskParams), eq(logCallback));
    doReturn(false)
        .when(k8sTaskHelperBase)
        .doStatusCheck(any(Kubectl.class), eq(k8sCanaryHandlerConfig.getCanaryWorkload().getResourceId()),
            eq(delegateTaskParams), eq(logCallback), eq(false));

    try {
      spyCanaryRequestHandler.executeTask(request, delegateTaskParams, iLogStreamingTaskClient, null);
    } catch (Exception ex) {
      Exception enhancedException = spyCanaryRequestHandler.handleTaskFailure(request, ex);
      assertThat(enhancedException).isInstanceOf(K8sCanaryDataException.class);
      assertThat(((K8sCanaryDataException) enhancedException).isCanaryWorkloadDeployed()).isTrue();
    }
  }
}