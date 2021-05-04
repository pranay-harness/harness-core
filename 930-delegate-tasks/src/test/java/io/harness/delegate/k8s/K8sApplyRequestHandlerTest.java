package io.harness.delegate.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.k8s.K8sConstants.MANIFEST_FILES_DIR;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.ACASIAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.k8s.beans.K8sApplyHandlerConfig;
import io.harness.delegate.k8s.exception.KubernetesExceptionHints;
import io.harness.delegate.task.k8s.ContainerDeploymentDelegateBaseHelper;
import io.harness.delegate.task.k8s.K8sApplyRequest;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sInfraDelegateConfig;
import io.harness.delegate.task.k8s.K8sTaskHelperBase;
import io.harness.delegate.task.k8s.ManifestDelegateConfig;
import io.harness.exception.InvalidRequestException;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.model.K8sDelegateTaskParams;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
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
public class K8sApplyRequestHandlerTest extends CategoryTest {
  @Mock private K8sApplyBaseHandler k8sApplyBaseHandler;
  @Mock private K8sTaskHelperBase k8sTaskHelperBase;
  @Mock private ContainerDeploymentDelegateBaseHelper containerDeploymentDelegateBaseHelper;

  @InjectMocks private K8sApplyRequestHandler requestHandler;
  @Mock ILogStreamingTaskClient iLogStreamingTaskClient;
  @Mock private LogCallback logCallback;
  @Mock private K8sInfraDelegateConfig k8sInfraDelegateConfig;
  @Mock private ManifestDelegateConfig manifestDelegateConfig;

  private final Integer timeoutIntervalInMin = 10;
  private final long timeoutIntervalInMillis = 60 * timeoutIntervalInMin * 1000;
  private final String accountId = "accountId";
  private final String namespace = "default";
  private final String releaseName = "test-release";
  private final KubernetesConfig kubernetesConfig = KubernetesConfig.builder().namespace(namespace).build();
  private final String workingDirectory = "manifest";
  private final String manifestFileDirectory = Paths.get(workingDirectory, MANIFEST_FILES_DIR).toString();
  private CommandUnitsProgress commandUnitsProgress = CommandUnitsProgress.builder().build();
  private K8sApplyHandlerConfig k8sApplyHandlerConfig;

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
            manifestDelegateConfig, manifestFileDirectory, logCallback, timeoutIntervalInMillis, accountId);
    doReturn(namespace).when(k8sInfraDelegateConfig).getNamespace();
    k8sApplyHandlerConfig = requestHandler.getK8sApplyHandlerConfig();
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldApplyManifestsSuccess() throws Exception {
    List<String> valuesList = Collections.emptyList();
    List<String> filePaths = Arrays.asList("deploy.yaml");
    K8sApplyRequest applyRequest = K8sApplyRequest.builder()
                                       .k8sInfraDelegateConfig(k8sInfraDelegateConfig)
                                       .manifestDelegateConfig(manifestDelegateConfig)
                                       .accountId(accountId)
                                       .releaseName(releaseName)
                                       .filePaths(filePaths)
                                       .timeoutIntervalInMin(timeoutIntervalInMin)
                                       .valuesYamlList(valuesList)
                                       .commandName("K8s Apply")
                                       .skipDryRun(true)
                                       .skipSteadyStateCheck(false)
                                       .build();

    K8sDelegateTaskParams delegateTaskParams =
        K8sDelegateTaskParams.builder().workingDirectory(workingDirectory).build();

    List<KubernetesResource> resources = Arrays.asList(
        KubernetesResource.builder()
            .spec("spec")
            .resourceId(
                KubernetesResourceId.builder().namespace(namespace).kind("deployment").name("test-deployment").build())
            .build());
    doReturn(resources)
        .when(k8sTaskHelperBase)
        .getResourcesFromManifests(delegateTaskParams, manifestDelegateConfig, manifestFileDirectory, filePaths,
            valuesList, releaseName, namespace, logCallback, timeoutIntervalInMin);
    doReturn(true).when(k8sApplyBaseHandler).prepare(logCallback, false, k8sApplyHandlerConfig, true);
    doReturn(true)
        .when(k8sTaskHelperBase)
        .applyManifests(
            any(Kubectl.class), eq(resources), eq(delegateTaskParams), eq(logCallback), eq(true), eq(false));
    doReturn(true)
        .when(k8sApplyBaseHandler)
        .steadyStateCheck(
            false, namespace, delegateTaskParams, timeoutIntervalInMillis, logCallback, k8sApplyHandlerConfig, false);

    K8sDeployResponse response = requestHandler.executeTaskInternal(
        applyRequest, delegateTaskParams, iLogStreamingTaskClient, commandUnitsProgress);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(SUCCESS);

    verify(k8sTaskHelperBase, times(1))
        .getResourcesFromManifests(eq(delegateTaskParams), eq(manifestDelegateConfig), eq(manifestFileDirectory),
            eq(filePaths), eq(valuesList), eq(releaseName), eq(namespace), eq(logCallback), eq(timeoutIntervalInMin));
    verify(k8sApplyBaseHandler, times(1)).prepare(eq(logCallback), eq(false), eq(k8sApplyHandlerConfig), eq(true));
    verify(k8sTaskHelperBase, times(1))
        .applyManifests(
            any(Kubectl.class), eq(resources), eq(delegateTaskParams), eq(logCallback), eq(true), eq(false));
    verify(k8sApplyBaseHandler, times(1))
        .steadyStateCheck(
            false, namespace, delegateTaskParams, timeoutIntervalInMillis, logCallback, k8sApplyHandlerConfig, false);
    verify(k8sApplyBaseHandler, times(1)).wrapUp(eq(delegateTaskParams), eq(logCallback), any(Kubectl.class));
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldApplyManifestsSkipDryRunSuccess() throws Exception {
    List<String> valuesList = Collections.emptyList();
    List<String> filePaths = Arrays.asList("deploy.yaml");
    K8sApplyRequest applyRequest = K8sApplyRequest.builder()
                                       .k8sInfraDelegateConfig(k8sInfraDelegateConfig)
                                       .manifestDelegateConfig(manifestDelegateConfig)
                                       .accountId(accountId)
                                       .releaseName(releaseName)
                                       .filePaths(filePaths)
                                       .timeoutIntervalInMin(timeoutIntervalInMin)
                                       .valuesYamlList(valuesList)
                                       .commandName("K8s Apply")
                                       .skipDryRun(false)
                                       .skipSteadyStateCheck(false)
                                       .build();

    K8sDelegateTaskParams delegateTaskParams =
        K8sDelegateTaskParams.builder().workingDirectory(workingDirectory).build();

    List<KubernetesResource> resources = Arrays.asList(
        KubernetesResource.builder()
            .spec("spec")
            .resourceId(
                KubernetesResourceId.builder().namespace(namespace).kind("deployment").name("test-deployment").build())
            .build());
    doReturn(resources)
        .when(k8sTaskHelperBase)
        .getResourcesFromManifests(delegateTaskParams, manifestDelegateConfig, manifestFileDirectory, filePaths,
            valuesList, releaseName, namespace, logCallback, timeoutIntervalInMin);
    doReturn(true)
        .when(k8sTaskHelperBase)
        .dryRunManifests(any(Kubectl.class), eq(resources), eq(delegateTaskParams), eq(logCallback));
    doReturn(true).when(k8sApplyBaseHandler).prepare(logCallback, false, k8sApplyHandlerConfig, true);
    doReturn(true)
        .when(k8sTaskHelperBase)
        .applyManifests(
            any(Kubectl.class), eq(resources), eq(delegateTaskParams), eq(logCallback), eq(true), eq(false));
    doReturn(true)
        .when(k8sApplyBaseHandler)
        .steadyStateCheck(
            false, namespace, delegateTaskParams, timeoutIntervalInMillis, logCallback, k8sApplyHandlerConfig, false);

    K8sDeployResponse response = requestHandler.executeTaskInternal(
        applyRequest, delegateTaskParams, iLogStreamingTaskClient, commandUnitsProgress);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(SUCCESS);

    verify(k8sTaskHelperBase, times(1))
        .getResourcesFromManifests(eq(delegateTaskParams), eq(manifestDelegateConfig), eq(manifestFileDirectory),
            eq(filePaths), eq(valuesList), eq(releaseName), eq(namespace), eq(logCallback), eq(timeoutIntervalInMin));
    verify(k8sTaskHelperBase, times(1))
        .dryRunManifests(any(Kubectl.class), eq(resources), eq(delegateTaskParams), eq(logCallback));
    verify(k8sApplyBaseHandler, times(1)).prepare(eq(logCallback), eq(false), eq(k8sApplyHandlerConfig), eq(true));
    verify(k8sTaskHelperBase, times(1))
        .applyManifests(
            any(Kubectl.class), eq(resources), eq(delegateTaskParams), eq(logCallback), eq(true), eq(false));
    verify(k8sApplyBaseHandler, times(1))
        .steadyStateCheck(
            false, namespace, delegateTaskParams, timeoutIntervalInMillis, logCallback, k8sApplyHandlerConfig, false);
    verify(k8sApplyBaseHandler, times(1)).wrapUp(eq(delegateTaskParams), eq(logCallback), any(Kubectl.class));
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldApplyManifestsSkipDryRunFailure() throws Exception {
    List<String> valuesList = Collections.emptyList();
    List<String> filePaths = Arrays.asList("deploy.yaml");
    K8sApplyRequest applyRequest = K8sApplyRequest.builder()
                                       .k8sInfraDelegateConfig(k8sInfraDelegateConfig)
                                       .manifestDelegateConfig(manifestDelegateConfig)
                                       .accountId(accountId)
                                       .releaseName(releaseName)
                                       .filePaths(filePaths)
                                       .timeoutIntervalInMin(timeoutIntervalInMin)
                                       .valuesYamlList(valuesList)
                                       .commandName("K8s Apply")
                                       .skipDryRun(false)
                                       .skipSteadyStateCheck(false)
                                       .build();

    K8sDelegateTaskParams delegateTaskParams =
        K8sDelegateTaskParams.builder().workingDirectory(workingDirectory).build();

    List<KubernetesResource> resources = Arrays.asList(
        KubernetesResource.builder()
            .spec("spec")
            .resourceId(
                KubernetesResourceId.builder().namespace(namespace).kind("deployment").name("test-deployment").build())
            .build());
    doReturn(resources)
        .when(k8sTaskHelperBase)
        .getResourcesFromManifests(delegateTaskParams, manifestDelegateConfig, manifestFileDirectory, filePaths,
            valuesList, releaseName, namespace, logCallback, timeoutIntervalInMin);
    doReturn(false)
        .when(k8sTaskHelperBase)
        .dryRunManifests(any(Kubectl.class), eq(resources), eq(delegateTaskParams), eq(logCallback));

    assertThatThrownBy(()
                           -> requestHandler.executeTaskInternal(
                               applyRequest, delegateTaskParams, iLogStreamingTaskClient, commandUnitsProgress))
        .hasMessage(KubernetesExceptionHints.DRY_RUN_MANIFEST_FAILED);

    verify(k8sTaskHelperBase, times(1))
        .getResourcesFromManifests(eq(delegateTaskParams), eq(manifestDelegateConfig), eq(manifestFileDirectory),
            eq(filePaths), eq(valuesList), eq(releaseName), eq(namespace), eq(logCallback), eq(timeoutIntervalInMin));
    verify(k8sTaskHelperBase, times(1))
        .dryRunManifests(any(Kubectl.class), eq(resources), eq(delegateTaskParams), eq(logCallback));
    verify(k8sApplyBaseHandler, times(0)).prepare(eq(logCallback), eq(false), eq(k8sApplyHandlerConfig), eq(true));
    verify(k8sTaskHelperBase, times(0))
        .applyManifests(any(Kubectl.class), eq(resources), eq(delegateTaskParams), eq(logCallback), eq(true));
    verify(k8sApplyBaseHandler, times(0))
        .steadyStateCheck(
            false, namespace, delegateTaskParams, timeoutIntervalInMillis, logCallback, k8sApplyHandlerConfig, false);
    verify(k8sApplyBaseHandler, times(0)).wrapUp(eq(delegateTaskParams), eq(logCallback), any(Kubectl.class));
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldApplyManifestsSkipDryRunFailureException() throws Exception {
    List<String> valuesList = Collections.emptyList();
    List<String> filePaths = Arrays.asList("deploy.yaml");
    K8sApplyRequest applyRequest = K8sApplyRequest.builder()
                                       .k8sInfraDelegateConfig(k8sInfraDelegateConfig)
                                       .manifestDelegateConfig(manifestDelegateConfig)
                                       .accountId(accountId)
                                       .releaseName(releaseName)
                                       .filePaths(filePaths)
                                       .timeoutIntervalInMin(timeoutIntervalInMin)
                                       .valuesYamlList(valuesList)
                                       .commandName("K8s Apply")
                                       .skipDryRun(false)
                                       .skipSteadyStateCheck(false)
                                       .build();

    K8sDelegateTaskParams delegateTaskParams =
        K8sDelegateTaskParams.builder().workingDirectory(workingDirectory).build();
    RuntimeException dryRunException = new RuntimeException("Dry run manifest failed");

    List<KubernetesResource> resources = Arrays.asList(
        KubernetesResource.builder()
            .spec("spec")
            .resourceId(
                KubernetesResourceId.builder().namespace(namespace).kind("deployment").name("test-deployment").build())
            .build());
    doReturn(resources)
        .when(k8sTaskHelperBase)
        .getResourcesFromManifests(delegateTaskParams, manifestDelegateConfig, manifestFileDirectory, filePaths,
            valuesList, releaseName, namespace, logCallback, timeoutIntervalInMin);
    doThrow(dryRunException)
        .when(k8sTaskHelperBase)
        .dryRunManifests(any(Kubectl.class), eq(resources), eq(delegateTaskParams), eq(logCallback));

    assertThatThrownBy(()
                           -> requestHandler.executeTaskInternal(
                               applyRequest, delegateTaskParams, iLogStreamingTaskClient, commandUnitsProgress))
        .isSameAs(dryRunException);

    verify(k8sTaskHelperBase, times(1))
        .getResourcesFromManifests(eq(delegateTaskParams), eq(manifestDelegateConfig), eq(manifestFileDirectory),
            eq(filePaths), eq(valuesList), eq(releaseName), eq(namespace), eq(logCallback), eq(timeoutIntervalInMin));
    verify(k8sTaskHelperBase, times(1))
        .dryRunManifests(any(Kubectl.class), eq(resources), eq(delegateTaskParams), eq(logCallback));
    verify(k8sApplyBaseHandler, times(0)).prepare(eq(logCallback), eq(false), eq(k8sApplyHandlerConfig), eq(true));
    verify(k8sTaskHelperBase, times(0))
        .applyManifests(
            any(Kubectl.class), eq(resources), eq(delegateTaskParams), eq(logCallback), eq(true), eq(false));
    verify(k8sApplyBaseHandler, times(0))
        .steadyStateCheck(
            false, namespace, delegateTaskParams, timeoutIntervalInMillis, logCallback, k8sApplyHandlerConfig, false);
    verify(k8sApplyBaseHandler, times(0)).wrapUp(eq(delegateTaskParams), eq(logCallback), any(Kubectl.class));
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldNotApplyManifestsWhenGetResourcesThrowsException() throws Exception {
    List<String> valuesList = Collections.emptyList();
    List<String> filePaths = Arrays.asList("deploy.yaml");
    K8sApplyRequest applyRequest = K8sApplyRequest.builder()
                                       .k8sInfraDelegateConfig(k8sInfraDelegateConfig)
                                       .manifestDelegateConfig(manifestDelegateConfig)
                                       .accountId(accountId)
                                       .releaseName(releaseName)
                                       .filePaths(filePaths)
                                       .timeoutIntervalInMin(timeoutIntervalInMin)
                                       .valuesYamlList(valuesList)
                                       .commandName("K8s Apply")
                                       .skipDryRun(false)
                                       .skipSteadyStateCheck(false)
                                       .build();

    K8sDelegateTaskParams delegateTaskParams =
        K8sDelegateTaskParams.builder().workingDirectory(workingDirectory).build();
    RuntimeException getResourceException = new RuntimeException("Get resources from manifests failed");

    List<KubernetesResource> resources = Arrays.asList(
        KubernetesResource.builder()
            .spec("spec")
            .resourceId(
                KubernetesResourceId.builder().namespace(namespace).kind("deployment").name("test-deployment").build())
            .build());
    doThrow(getResourceException)
        .when(k8sTaskHelperBase)
        .getResourcesFromManifests(delegateTaskParams, manifestDelegateConfig, manifestFileDirectory, filePaths,
            valuesList, releaseName, namespace, logCallback, timeoutIntervalInMin);

    assertThatThrownBy(()
                           -> requestHandler.executeTaskInternal(
                               applyRequest, delegateTaskParams, iLogStreamingTaskClient, commandUnitsProgress))
        .isSameAs(getResourceException);

    verify(k8sTaskHelperBase, times(1))
        .getResourcesFromManifests(eq(delegateTaskParams), eq(manifestDelegateConfig), eq(manifestFileDirectory),
            eq(filePaths), eq(valuesList), eq(releaseName), eq(namespace), eq(logCallback), eq(timeoutIntervalInMin));
    verify(k8sTaskHelperBase, times(0))
        .dryRunManifests(any(Kubectl.class), eq(resources), eq(delegateTaskParams), eq(logCallback));
    verify(k8sApplyBaseHandler, times(0)).prepare(eq(logCallback), eq(false), eq(k8sApplyHandlerConfig), eq(true));
    verify(k8sTaskHelperBase, times(0))
        .applyManifests(any(Kubectl.class), eq(resources), eq(delegateTaskParams), eq(logCallback), eq(true));
    verify(k8sApplyBaseHandler, times(0))
        .steadyStateCheck(
            false, namespace, delegateTaskParams, timeoutIntervalInMillis, logCallback, k8sApplyHandlerConfig, false);
    verify(k8sApplyBaseHandler, times(0)).wrapUp(eq(delegateTaskParams), eq(logCallback), any(Kubectl.class));
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldNotApplyManifestsIfFilePathIsEmpty() throws Exception {
    List<String> valuesList = Collections.emptyList();
    List<String> filePaths = Collections.emptyList();
    K8sApplyRequest applyRequest = K8sApplyRequest.builder()
                                       .k8sInfraDelegateConfig(k8sInfraDelegateConfig)
                                       .manifestDelegateConfig(manifestDelegateConfig)
                                       .accountId(accountId)
                                       .releaseName(releaseName)
                                       .filePaths(filePaths)
                                       .timeoutIntervalInMin(timeoutIntervalInMin)
                                       .valuesYamlList(valuesList)
                                       .commandName("K8s Apply")
                                       .skipDryRun(true)
                                       .skipSteadyStateCheck(false)
                                       .build();

    K8sDelegateTaskParams delegateTaskParams =
        K8sDelegateTaskParams.builder().workingDirectory(workingDirectory).build();

    assertThatThrownBy(()
                           -> requestHandler.executeTaskInternal(
                               applyRequest, delegateTaskParams, iLogStreamingTaskClient, commandUnitsProgress))
        .hasMessage(KubernetesExceptionHints.MISSING_APPLY_FILES_PATH);

    verify(k8sTaskHelperBase, times(0))
        .getResourcesFromManifests(eq(delegateTaskParams), eq(manifestDelegateConfig), eq(manifestFileDirectory),
            eq(filePaths), eq(valuesList), eq(releaseName), eq(namespace), eq(logCallback), eq(timeoutIntervalInMin));
    verify(k8sApplyBaseHandler, times(0)).prepare(eq(logCallback), eq(false), eq(k8sApplyHandlerConfig), eq(true));
    verify(k8sApplyBaseHandler, times(0))
        .steadyStateCheck(
            false, namespace, delegateTaskParams, timeoutIntervalInMillis, logCallback, k8sApplyHandlerConfig, false);
    verify(k8sApplyBaseHandler, times(0)).wrapUp(eq(delegateTaskParams), eq(logCallback), any(Kubectl.class));
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldNotApplyIfFailToPrepare() throws Exception {
    List<String> valuesList = Collections.emptyList();
    List<String> filePaths = Arrays.asList("deploy.yaml");
    K8sApplyRequest applyRequest = K8sApplyRequest.builder()
                                       .k8sInfraDelegateConfig(k8sInfraDelegateConfig)
                                       .manifestDelegateConfig(manifestDelegateConfig)
                                       .accountId(accountId)
                                       .releaseName(releaseName)
                                       .filePaths(filePaths)
                                       .timeoutIntervalInMin(timeoutIntervalInMin)
                                       .valuesYamlList(valuesList)
                                       .commandName("K8s Apply")
                                       .skipDryRun(true)
                                       .skipSteadyStateCheck(false)
                                       .build();
    InvalidRequestException prepareException = new InvalidRequestException("Failed to prepare");

    K8sDelegateTaskParams delegateTaskParams =
        K8sDelegateTaskParams.builder().workingDirectory(workingDirectory).build();

    List<KubernetesResource> resources = Arrays.asList(
        KubernetesResource.builder()
            .spec("spec")
            .resourceId(
                KubernetesResourceId.builder().namespace(namespace).kind("deployment").name("test-deployment").build())
            .build());
    doReturn(resources)
        .when(k8sTaskHelperBase)
        .getResourcesFromManifests(delegateTaskParams, manifestDelegateConfig, manifestFileDirectory, filePaths,
            valuesList, releaseName, namespace, logCallback, timeoutIntervalInMin);
    doThrow(prepareException).when(k8sApplyBaseHandler).prepare(logCallback, false, k8sApplyHandlerConfig, true);

    assertThatThrownBy(()
                           -> requestHandler.executeTaskInternal(
                               applyRequest, delegateTaskParams, iLogStreamingTaskClient, commandUnitsProgress))
        .isSameAs(prepareException);

    verify(k8sTaskHelperBase, times(1))
        .getResourcesFromManifests(eq(delegateTaskParams), eq(manifestDelegateConfig), eq(manifestFileDirectory),
            eq(filePaths), eq(valuesList), eq(releaseName), eq(namespace), eq(logCallback), eq(timeoutIntervalInMin));
    verify(k8sApplyBaseHandler, times(1)).prepare(eq(logCallback), eq(false), eq(k8sApplyHandlerConfig), eq(true));
    verify(k8sTaskHelperBase, times(0))
        .applyManifests(any(Kubectl.class), eq(resources), eq(delegateTaskParams), eq(logCallback), eq(true));
    verify(k8sApplyBaseHandler, times(0))
        .steadyStateCheck(
            false, namespace, delegateTaskParams, timeoutIntervalInMillis, logCallback, k8sApplyHandlerConfig, false);
    verify(k8sApplyBaseHandler, times(0)).wrapUp(eq(delegateTaskParams), eq(logCallback), any(Kubectl.class));
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldFailIfApplyManifestsFails() throws Exception {
    List<String> valuesList = Collections.emptyList();
    List<String> filePaths = Arrays.asList("deploy.yaml");
    K8sApplyRequest applyRequest = K8sApplyRequest.builder()
                                       .k8sInfraDelegateConfig(k8sInfraDelegateConfig)
                                       .manifestDelegateConfig(manifestDelegateConfig)
                                       .accountId(accountId)
                                       .releaseName(releaseName)
                                       .filePaths(filePaths)
                                       .timeoutIntervalInMin(timeoutIntervalInMin)
                                       .valuesYamlList(valuesList)
                                       .commandName("K8s Apply")
                                       .skipDryRun(true)
                                       .skipSteadyStateCheck(false)
                                       .build();

    K8sDelegateTaskParams delegateTaskParams =
        K8sDelegateTaskParams.builder().workingDirectory(workingDirectory).build();

    List<KubernetesResource> resources = Arrays.asList(
        KubernetesResource.builder()
            .spec("spec")
            .resourceId(
                KubernetesResourceId.builder().namespace(namespace).kind("deployment").name("test-deployment").build())
            .build());
    doReturn(resources)
        .when(k8sTaskHelperBase)
        .getResourcesFromManifests(delegateTaskParams, manifestDelegateConfig, manifestFileDirectory, filePaths,
            valuesList, releaseName, namespace, logCallback, timeoutIntervalInMin);
    doReturn(true).when(k8sApplyBaseHandler).prepare(logCallback, false, k8sApplyHandlerConfig, true);
    doReturn(false)
        .when(k8sTaskHelperBase)
        .applyManifests(
            any(Kubectl.class), eq(resources), eq(delegateTaskParams), eq(logCallback), eq(true), eq(false));

    assertThatThrownBy(()
                           -> requestHandler.executeTaskInternal(
                               applyRequest, delegateTaskParams, iLogStreamingTaskClient, commandUnitsProgress))
        .hasMessage(KubernetesExceptionHints.APPLY_MANIFEST_FAILED);

    verify(k8sTaskHelperBase, times(1))
        .getResourcesFromManifests(eq(delegateTaskParams), eq(manifestDelegateConfig), eq(manifestFileDirectory),
            eq(filePaths), eq(valuesList), eq(releaseName), eq(namespace), eq(logCallback), eq(timeoutIntervalInMin));
    verify(k8sApplyBaseHandler, times(1)).prepare(eq(logCallback), eq(false), eq(k8sApplyHandlerConfig), eq(true));
    verify(k8sTaskHelperBase, times(1))
        .applyManifests(
            any(Kubectl.class), eq(resources), eq(delegateTaskParams), eq(logCallback), eq(true), eq(false));
    verify(k8sApplyBaseHandler, times(0))
        .steadyStateCheck(
            false, namespace, delegateTaskParams, timeoutIntervalInMillis, logCallback, k8sApplyHandlerConfig, false);
    verify(k8sApplyBaseHandler, times(0)).wrapUp(eq(delegateTaskParams), eq(logCallback), any(Kubectl.class));
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldNotApplyManifestsIfFailsSteadyCheck() throws Exception {
    List<String> valuesList = Collections.emptyList();
    List<String> filePaths = Arrays.asList("deploy.yaml");
    K8sApplyRequest applyRequest = K8sApplyRequest.builder()
                                       .k8sInfraDelegateConfig(k8sInfraDelegateConfig)
                                       .manifestDelegateConfig(manifestDelegateConfig)
                                       .accountId(accountId)
                                       .releaseName(releaseName)
                                       .filePaths(filePaths)
                                       .timeoutIntervalInMin(timeoutIntervalInMin)
                                       .valuesYamlList(valuesList)
                                       .commandName("K8s Apply")
                                       .skipDryRun(true)
                                       .skipSteadyStateCheck(false)
                                       .build();

    K8sDelegateTaskParams delegateTaskParams =
        K8sDelegateTaskParams.builder().workingDirectory(workingDirectory).build();

    List<KubernetesResource> resources = Arrays.asList(
        KubernetesResource.builder()
            .spec("spec")
            .resourceId(
                KubernetesResourceId.builder().namespace(namespace).kind("deployment").name("test-deployment").build())
            .build());
    doReturn(resources)
        .when(k8sTaskHelperBase)
        .getResourcesFromManifests(delegateTaskParams, manifestDelegateConfig, manifestFileDirectory, filePaths,
            valuesList, releaseName, namespace, logCallback, timeoutIntervalInMin);
    doReturn(true).when(k8sApplyBaseHandler).prepare(logCallback, false, k8sApplyHandlerConfig, true);
    doReturn(true)
        .when(k8sTaskHelperBase)
        .applyManifests(
            any(Kubectl.class), eq(resources), eq(delegateTaskParams), eq(logCallback), eq(true), eq(false));
    doReturn(false)
        .when(k8sApplyBaseHandler)
        .steadyStateCheck(
            false, namespace, delegateTaskParams, timeoutIntervalInMillis, logCallback, k8sApplyHandlerConfig, false);

    assertThatThrownBy(()
                           -> requestHandler.executeTaskInternal(
                               applyRequest, delegateTaskParams, iLogStreamingTaskClient, commandUnitsProgress))
        .hasMessage(KubernetesExceptionHints.WAIT_FOR_STEADY_STATE_FAILED);

    verify(k8sTaskHelperBase, times(1))
        .getResourcesFromManifests(eq(delegateTaskParams), eq(manifestDelegateConfig), eq(manifestFileDirectory),
            eq(filePaths), eq(valuesList), eq(releaseName), eq(namespace), eq(logCallback), eq(timeoutIntervalInMin));
    verify(k8sApplyBaseHandler, times(1)).prepare(eq(logCallback), eq(false), eq(k8sApplyHandlerConfig), eq(true));
    verify(k8sTaskHelperBase, times(1))
        .applyManifests(
            any(Kubectl.class), eq(resources), eq(delegateTaskParams), eq(logCallback), eq(true), eq(false));
    verify(k8sApplyBaseHandler, times(1))
        .steadyStateCheck(
            false, namespace, delegateTaskParams, timeoutIntervalInMillis, logCallback, k8sApplyHandlerConfig, false);
    verify(k8sApplyBaseHandler, times(0)).wrapUp(eq(delegateTaskParams), eq(logCallback), any(Kubectl.class));
  }
}
