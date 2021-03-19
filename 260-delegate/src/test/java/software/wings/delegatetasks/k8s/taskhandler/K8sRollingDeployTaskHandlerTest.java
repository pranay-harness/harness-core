package software.wings.delegatetasks.k8s.taskhandler;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.k8s.K8sConstants.MANIFEST_FILES_DIR;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.ACASIAN;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.BOJANA;
import static io.harness.rule.OwnerRule.TATHAGAT;
import static io.harness.rule.OwnerRule.TMACARI;

import static software.wings.delegatetasks.k8s.K8sTestConstants.CONFIG_MAP_YAML;
import static software.wings.delegatetasks.k8s.K8sTestConstants.DEPLOYMENT_YAML;
import static software.wings.delegatetasks.k8s.K8sTestConstants.SECRET_YAML;

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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.delegate.k8s.K8sRollingBaseHandler;
import io.harness.delegate.task.k8s.K8sTaskHelperBase;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.KubernetesYamlException;
import io.harness.k8s.KubernetesContainerService;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.manifest.ManifestHelper;
import io.harness.k8s.model.K8sDelegateTaskParams;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.ReleaseHistory;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.k8s.K8sTaskHelper;
import software.wings.delegatetasks.k8s.K8sTestHelper;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.helm.request.HelmChartConfigParams;
import software.wings.helpers.ext.helm.response.HelmChartInfo;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig;
import software.wings.helpers.ext.k8s.request.K8sRollingDeployTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sRollingDeployResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;

import com.google.common.collect.ImmutableList;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

@TargetModule(Module._930_DELEGATE_TASKS)
@OwnedBy(CDP)
public class K8sRollingDeployTaskHandlerTest extends WingsBaseTest {
  @Mock private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Mock private KubernetesContainerService kubernetesContainerService;
  @Mock private K8sTaskHelper k8sTaskHelper;
  @Mock private K8sTaskHelperBase k8sTaskHelperBase;
  @Mock private K8sRollingBaseHandler k8sRollingBaseHandler;
  @Mock private ExecutionLogCallback executionLogCallback;

  @InjectMocks private K8sRollingDeployTaskHandler k8sRollingDeployTaskHandler;
  @InjectMocks private K8sRollingBaseHandler k8sRollingDeployTaskBaseHandler;

  @Before
  public void setUp() throws Exception {
    doReturn(executionLogCallback)
        .when(k8sTaskHelper)
        .getExecutionLogCallback(any(K8sTaskParameters.class), anyString());
    doReturn(KubernetesConfig.builder().build())
        .when(containerDeploymentDelegateHelper)
        .getKubernetesConfig(any(K8sClusterConfig.class), anyBoolean());
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testDryRunIsSkipped() throws Exception {
    K8sRollingDeployTaskParameters rollingDeployTaskParams =
        K8sRollingDeployTaskParameters.builder().skipDryRun(true).build();
    K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().build();
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback();

    when(k8sTaskHelperBase.getReleaseHistoryData(any(), any())).thenReturn(null);
    doNothing().when(k8sTaskHelperBase).deleteSkippedManifestFiles(any(), any());
    when(k8sTaskHelper.renderTemplate(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(emptyList());
    doNothing().when(k8sTaskHelperBase).setNamespaceToKubernetesResourcesIfRequired(any(), any());
    when(k8sTaskHelperBase.readManifestAndOverrideLocalSecrets(any(), any(), anyBoolean())).thenReturn(emptyList());

    k8sRollingDeployTaskHandler.init(rollingDeployTaskParams, delegateTaskParams, executionLogCallback);
    verify(k8sTaskHelperBase, times(0)).dryRunManifests(any(), any(), any(), any());
    verify(k8sTaskHelperBase, times(0)).updateVirtualServiceManifestFilesWithRoutesForCanary(any(), any(), any());
    verify(k8sTaskHelperBase, times(0)).updateDestinationRuleManifestFilesWithSubsets(any(), any(), any(), any());
    verify(k8sTaskHelperBase, times(1)).readManifestAndOverrideLocalSecrets(any(), any(), anyBoolean());
    verify(k8sTaskHelper, times(1)).renderTemplate(any(), any(), any(), any(), any(), any(), any(), any());
    verify(k8sTaskHelperBase, times(1)).setNamespaceToKubernetesResourcesIfRequired(any(), any());
    verify(k8sTaskHelperBase, times(1)).deleteSkippedManifestFiles(any(), any());
    verify(k8sTaskHelperBase, times(1)).getReleaseHistoryData(any(), any());
    verify(containerDeploymentDelegateHelper, times(1)).getKubernetesConfig(any(K8sClusterConfig.class), anyBoolean());
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testDryRunIsNotSkipped() throws Exception {
    K8sRollingDeployTaskParameters rollingDeployTaskParams =
        K8sRollingDeployTaskParameters.builder().skipDryRun(false).build();
    K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().build();
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback();

    doNothing().when(k8sTaskHelperBase).setNamespaceToKubernetesResourcesIfRequired(any(), any());
    when(k8sTaskHelperBase.getReleaseHistoryData(any(), any())).thenReturn(null);
    doNothing().when(k8sTaskHelperBase).deleteSkippedManifestFiles(any(), any());
    when(k8sTaskHelper.renderTemplate(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(emptyList());
    when(k8sTaskHelperBase.readManifestAndOverrideLocalSecrets(any(), any(), anyBoolean())).thenReturn(emptyList());

    k8sRollingDeployTaskHandler.init(rollingDeployTaskParams, delegateTaskParams, executionLogCallback);
    verify(k8sTaskHelperBase, times(1)).dryRunManifests(any(), any(), any(), any());
    verify(k8sTaskHelperBase, times(0)).updateVirtualServiceManifestFilesWithRoutesForCanary(any(), any(), any());
    verify(k8sTaskHelperBase, times(0)).updateDestinationRuleManifestFilesWithSubsets(any(), any(), any(), any());
    verify(k8sTaskHelperBase, times(1)).setNamespaceToKubernetesResourcesIfRequired(any(), any());
    verify(k8sTaskHelperBase, times(1)).readManifestAndOverrideLocalSecrets(any(), any(), anyBoolean());
    verify(k8sTaskHelper, times(1)).renderTemplate(any(), any(), any(), any(), any(), any(), any(), any());
    verify(k8sTaskHelperBase, times(1)).deleteSkippedManifestFiles(any(), any());
    verify(k8sTaskHelperBase, times(1)).getReleaseHistoryData(any(), any());
    verify(containerDeploymentDelegateHelper, times(1)).getKubernetesConfig(any(K8sClusterConfig.class), anyBoolean());
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
    doReturn(true)
        .when(k8sTaskHelper)
        .fetchManifestFilesAndWriteToDirectory(
            any(K8sDelegateManifestConfig.class), anyString(), any(ExecutionLogCallback.class), anyLong());
    doReturn(true)
        .when(k8sTaskHelperBase)
        .applyManifests(any(Kubectl.class), anyListOf(KubernetesResource.class), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class), anyBoolean());
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
            any(K8sDelegateManifestConfig.class), anyString(), any(ExecutionLogCallback.class), anyLong());
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void prepareForRollingNotCanary() throws Exception {
    List<KubernetesResource> kubernetesResources = getResources();
    K8sRollingDeployTaskHandler handler = spy(k8sRollingDeployTaskHandler);
    doReturn(kubernetesResources)
        .when(k8sTaskHelperBase)
        .readManifestAndOverrideLocalSecrets(anyList(), any(ExecutionLogCallback.class), anyBoolean());
    doReturn(true)
        .when(k8sTaskHelper)
        .fetchManifestFilesAndWriteToDirectory(
            any(K8sDelegateManifestConfig.class), anyString(), any(ExecutionLogCallback.class), anyLong());
    doReturn(true)
        .when(k8sTaskHelperBase)
        .dryRunManifests(
            any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class), any(ExecutionLogCallback.class));
    doReturn(true)
        .when(k8sTaskHelperBase)
        .applyManifests(any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class), anyBoolean());
    handler.executeTaskInternal(
        K8sRollingDeployTaskParameters.builder().releaseName("releaseName").isInCanaryWorkflow(false).build(),
        K8sDelegateTaskParams.builder().build());

    verify(k8sTaskHelper, times(1))
        .fetchManifestFilesAndWriteToDirectory(
            any(K8sDelegateManifestConfig.class), anyString(), any(ExecutionLogCallback.class), anyLong());
    verify(k8sTaskHelperBase, times(1)).getReleaseHistoryData(any(KubernetesConfig.class), anyString());
    verify(containerDeploymentDelegateHelper, times(1)).getKubernetesConfig(any(K8sClusterConfig.class), anyBoolean());
    verify(k8sTaskHelperBase, times(1))
        .dryRunManifests(
            any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class), any(ExecutionLogCallback.class));
    verify(k8sTaskHelperBase, times(1))
        .applyManifests(any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class), anyBoolean());
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
    doReturn(releaseHistory).when(k8sTaskHelperBase).getReleaseHistoryData(any(KubernetesConfig.class), anyString());
    doReturn(true)
        .when(k8sTaskHelper)
        .fetchManifestFilesAndWriteToDirectory(
            any(K8sDelegateManifestConfig.class), anyString(), any(ExecutionLogCallback.class), anyLong());
    List<KubernetesResource> kubernetesResources = getResources();
    doReturn(kubernetesResources)
        .when(k8sTaskHelperBase)
        .readManifestAndOverrideLocalSecrets(anyList(), any(ExecutionLogCallback.class), anyBoolean());
    K8sRollingDeployTaskHandler handler = spy(k8sRollingDeployTaskHandler);
    doReturn(true)
        .when(k8sTaskHelperBase)
        .dryRunManifests(
            any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class), any(ExecutionLogCallback.class));
    doReturn(true)
        .when(k8sTaskHelperBase)
        .applyManifests(any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class), anyBoolean());
    handler.executeTaskInternal(
        K8sRollingDeployTaskParameters.builder().releaseName("releaseName").isInCanaryWorkflow(true).build(),
        K8sDelegateTaskParams.builder().build());

    verify(k8sTaskHelper, times(1))
        .fetchManifestFilesAndWriteToDirectory(
            any(K8sDelegateManifestConfig.class), anyString(), any(ExecutionLogCallback.class), anyLong());
    verify(k8sTaskHelperBase, times(1)).getReleaseHistoryData(any(KubernetesConfig.class), anyString());
    verify(containerDeploymentDelegateHelper, times(1)).getKubernetesConfig(any(K8sClusterConfig.class), anyBoolean());
    verify(k8sTaskHelperBase, times(1))
        .dryRunManifests(
            any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class), any(ExecutionLogCallback.class));
    verify(k8sTaskHelperBase, times(1))
        .applyManifests(any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class), anyBoolean());
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testExceptionThrownWhenGettingLoadBalancerEndpoint() throws Exception {
    String releaseHistory = "---\n"
        + "version: v1\n"
        + "releases:\n"
        + "- status: Succeeded\n"
        + "  managedWorkloads: []\n";
    doReturn(releaseHistory).when(k8sTaskHelperBase).getReleaseHistoryData(any(KubernetesConfig.class), anyString());
    doReturn(true)
        .when(k8sTaskHelper)
        .fetchManifestFilesAndWriteToDirectory(
            any(K8sDelegateManifestConfig.class), anyString(), any(ExecutionLogCallback.class), anyLong());
    List<KubernetesResource> kubernetesResources = getResources();
    doReturn(kubernetesResources)
        .when(k8sTaskHelperBase)
        .readManifestAndOverrideLocalSecrets(anyList(), any(ExecutionLogCallback.class), anyBoolean());
    K8sRollingDeployTaskHandler handler = spy(k8sRollingDeployTaskHandler);
    doReturn(true)
        .when(k8sTaskHelperBase)
        .dryRunManifests(
            any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class), any(ExecutionLogCallback.class));
    doReturn(true)
        .when(k8sTaskHelperBase)
        .applyManifests(any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class), anyBoolean());

    doReturn(true)
        .when(k8sTaskHelperBase)
        .doStatusCheckForAllResources(any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class), anyString(),
            any(ExecutionLogCallback.class), anyBoolean());

    doReturn(true)
        .when(k8sTaskHelperBase)
        .doStatusCheckForAllCustomResources(any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class), anyBoolean(), anyLong());

    doThrow(new InvalidArgumentsException("reason")).when(k8sTaskHelperBase).getLoadBalancerEndpoint(any(), any());

    assertThatExceptionOfType(InvalidArgumentsException.class)
        .isThrownBy(()
                        -> k8sRollingDeployTaskHandler.executeTaskInternal(K8sRollingDeployTaskParameters.builder()
                                                                               .releaseName("releaseName")
                                                                               .isInCanaryWorkflow(true)
                                                                               .build(),
                            K8sDelegateTaskParams.builder().build()))
        .withMessageContaining("reason");

    verify(executionLogCallback, times(1)).saveExecutionLog("reason", ERROR, FAILURE);
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testInitException() throws Exception {
    doThrow(new KubernetesYamlException("reason"))
        .when(k8sTaskHelperBase)
        .deleteSkippedManifestFiles(anyString(), any(ExecutionLogCallback.class));
    final boolean success = k8sRollingDeployTaskHandler.init(K8sRollingDeployTaskParameters.builder().build(),
        K8sDelegateTaskParams.builder().build(), Mockito.mock(ExecutionLogCallback.class));
    assertThat(success).isFalse();
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testSuccessIfPassesStatusCheck() throws Exception {
    K8sRollingDeployTaskHandler handler = spy(k8sRollingDeployTaskHandler);
    K8sDelegateManifestConfig manifestConfig = K8sDelegateManifestConfig.builder()
                                                   .manifestStoreTypes(StoreType.HelmChartRepo)
                                                   .helmChartConfigParams(HelmChartConfigParams.builder().build())
                                                   .build();
    K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().workingDirectory(".").build();
    K8sRollingDeployTaskParameters rollingDeployTaskParams = K8sRollingDeployTaskParameters.builder()
                                                                 .releaseName("RN-123")
                                                                 .k8sDelegateManifestConfig(manifestConfig)
                                                                 .k8sClusterConfig(K8sClusterConfig.builder().build())
                                                                 .skipDryRun(true)
                                                                 .build();

    on(handler).set("resources", ImmutableList.of(K8sTestHelper.deployment()));
    on(handler).set("managedWorkloads", ImmutableList.of(K8sTestHelper.deployment()));
    ReleaseHistory releaseHist = ReleaseHistory.createNew();
    releaseHist.setReleases(new ArrayList<>());
    on(handler).set("releaseHistory", releaseHist);
    on(handler).set("kubernetesConfig", KubernetesConfig.builder().build());

    doReturn(true).when(handler).init(
        any(K8sRollingDeployTaskParameters.class), any(K8sDelegateTaskParams.class), any(ExecutionLogCallback.class));
    doReturn(true)
        .when(k8sTaskHelper)
        .fetchManifestFilesAndWriteToDirectory(
            any(K8sDelegateManifestConfig.class), anyString(), any(ExecutionLogCallback.class), anyLong());
    doReturn(true)
        .when(k8sTaskHelperBase)
        .applyManifests(any(Kubectl.class), anyListOf(KubernetesResource.class), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class), anyBoolean());
    doReturn(true)
        .when(k8sTaskHelperBase)
        .doStatusCheckForAllResources(any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class), anyString(),
            any(ExecutionLogCallback.class), anyBoolean());

    doReturn(true)
        .when(k8sTaskHelperBase)
        .doStatusCheckForAllCustomResources(any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class), anyBoolean(), anyLong());

    K8sTaskExecutionResponse response = handler.executeTask(rollingDeployTaskParams, delegateTaskParams);

    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testFailCrdStatusCheck() throws Exception {
    K8sRollingDeployTaskHandler handler = spy(k8sRollingDeployTaskHandler);
    K8sDelegateManifestConfig manifestConfig = K8sDelegateManifestConfig.builder()
                                                   .manifestStoreTypes(StoreType.HelmChartRepo)
                                                   .helmChartConfigParams(HelmChartConfigParams.builder().build())
                                                   .build();
    K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().workingDirectory(".").build();
    K8sRollingDeployTaskParameters rollingDeployTaskParams = K8sRollingDeployTaskParameters.builder()
                                                                 .releaseName("RN-123")
                                                                 .k8sDelegateManifestConfig(manifestConfig)
                                                                 .k8sClusterConfig(K8sClusterConfig.builder().build())
                                                                 .skipDryRun(true)
                                                                 .build();

    on(handler).set("resources", ImmutableList.of(K8sTestHelper.deployment()));
    on(handler).set("managedWorkloads", ImmutableList.of(K8sTestHelper.deployment()));
    ReleaseHistory releaseHist = ReleaseHistory.createNew();
    releaseHist.setReleases(new ArrayList<>());
    on(handler).set("releaseHistory", releaseHist);
    on(handler).set("kubernetesConfig", KubernetesConfig.builder().build());

    doReturn(true).when(handler).init(
        any(K8sRollingDeployTaskParameters.class), any(K8sDelegateTaskParams.class), any(ExecutionLogCallback.class));
    doReturn(true)
        .when(k8sTaskHelper)
        .fetchManifestFilesAndWriteToDirectory(
            any(K8sDelegateManifestConfig.class), anyString(), any(ExecutionLogCallback.class), anyLong());
    doReturn(true)
        .when(k8sTaskHelperBase)
        .applyManifests(any(Kubectl.class), anyListOf(KubernetesResource.class), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class), anyBoolean());
    doReturn(true)
        .when(k8sTaskHelperBase)
        .doStatusCheckForAllResources(any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class), anyString(),
            any(ExecutionLogCallback.class), anyBoolean());

    doReturn(false)
        .when(k8sTaskHelperBase)
        .doStatusCheckForAllCustomResources(any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class), anyBoolean(), anyLong());

    K8sTaskExecutionResponse response = handler.executeTask(rollingDeployTaskParams, delegateTaskParams);

    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testFailWorkloadsStatusCheck() throws Exception {
    K8sRollingDeployTaskHandler handler = spy(k8sRollingDeployTaskHandler);
    K8sDelegateManifestConfig manifestConfig = K8sDelegateManifestConfig.builder()
                                                   .manifestStoreTypes(StoreType.HelmChartRepo)
                                                   .helmChartConfigParams(HelmChartConfigParams.builder().build())
                                                   .build();
    K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().workingDirectory(".").build();
    K8sRollingDeployTaskParameters rollingDeployTaskParams = K8sRollingDeployTaskParameters.builder()
                                                                 .releaseName("RN-123")
                                                                 .k8sDelegateManifestConfig(manifestConfig)
                                                                 .k8sClusterConfig(K8sClusterConfig.builder().build())
                                                                 .skipDryRun(true)
                                                                 .build();

    on(handler).set("resources", ImmutableList.of(K8sTestHelper.deployment()));
    on(handler).set("managedWorkloads", ImmutableList.of(K8sTestHelper.deployment()));
    ReleaseHistory releaseHist = ReleaseHistory.createNew();
    releaseHist.setReleases(new ArrayList<>());
    on(handler).set("releaseHistory", releaseHist);
    on(handler).set("kubernetesConfig", KubernetesConfig.builder().build());

    doReturn(true).when(handler).init(
        any(K8sRollingDeployTaskParameters.class), any(K8sDelegateTaskParams.class), any(ExecutionLogCallback.class));
    doReturn(true)
        .when(k8sTaskHelper)
        .fetchManifestFilesAndWriteToDirectory(
            any(K8sDelegateManifestConfig.class), anyString(), any(ExecutionLogCallback.class), anyLong());
    doReturn(true)
        .when(k8sTaskHelperBase)
        .applyManifests(any(Kubectl.class), anyListOf(KubernetesResource.class), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class), anyBoolean());
    doReturn(false)
        .when(k8sTaskHelperBase)
        .doStatusCheckForAllResources(any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class), anyString(),
            any(ExecutionLogCallback.class), anyBoolean());

    doReturn(true)
        .when(k8sTaskHelperBase)
        .doStatusCheckForAllCustomResources(any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class), anyBoolean(), anyLong());

    K8sTaskExecutionResponse response = handler.executeTask(rollingDeployTaskParams, delegateTaskParams);

    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
  }

  private List<KubernetesResource> getResources() {
    List<KubernetesResource> kubernetesResources = new ArrayList<>();
    kubernetesResources.addAll(ManifestHelper.processYaml(DEPLOYMENT_YAML));
    return kubernetesResources;
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testVersioningOfConfigMapAndSecret() throws Exception {
    List<KubernetesResource> kubernetesResources = new ArrayList<>();
    kubernetesResources.addAll(ManifestHelper.processYaml(CONFIG_MAP_YAML));

    K8sRollingDeployTaskHandler handler = spy(k8sRollingDeployTaskHandler);
    doReturn(kubernetesResources)
        .when(k8sTaskHelperBase)
        .readManifestAndOverrideLocalSecrets(anyList(), any(ExecutionLogCallback.class), anyBoolean());
    doReturn(true)
        .when(k8sTaskHelper)
        .fetchManifestFilesAndWriteToDirectory(
            any(K8sDelegateManifestConfig.class), anyString(), any(ExecutionLogCallback.class), anyLong());
    doReturn(true)
        .when(k8sTaskHelperBase)
        .dryRunManifests(
            any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class), any(ExecutionLogCallback.class));

    handler.executeTaskInternal(
        K8sRollingDeployTaskParameters.builder().releaseName("releaseName").isInCanaryWorkflow(false).build(),
        K8sDelegateTaskParams.builder().build());
    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    verify(k8sTaskHelperBase, times(1))
        .applyManifests(any(Kubectl.class), captor.capture(), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class), anyBoolean());
    List<KubernetesResource> kubernetesResourceList = captor.getValue();
    assertThat(kubernetesResourceList.get(0).getResourceId().isVersioned()).isFalse();

    kubernetesResources.clear();
    kubernetesResources.addAll(ManifestHelper.processYaml(SECRET_YAML));
    handler.executeTaskInternal(
        K8sRollingDeployTaskParameters.builder().releaseName("releaseName").isInCanaryWorkflow(false).build(),
        K8sDelegateTaskParams.builder().build());
    captor = ArgumentCaptor.forClass(List.class);
    verify(k8sTaskHelperBase, times(2))
        .applyManifests(any(Kubectl.class), captor.capture(), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class), anyBoolean());
    kubernetesResourceList = captor.getValue();
    assertThat(kubernetesResourceList.get(0).getResourceId().isVersioned()).isFalse();

    kubernetesResources.addAll(ManifestHelper.processYaml(CONFIG_MAP_YAML));
    kubernetesResources.addAll(ManifestHelper.processYaml(DEPLOYMENT_YAML));
    handler.executeTaskInternal(
        K8sRollingDeployTaskParameters.builder().releaseName("releaseName").isInCanaryWorkflow(false).build(),
        K8sDelegateTaskParams.builder().build());
    captor = ArgumentCaptor.forClass(List.class);
    verify(k8sTaskHelperBase, times(3))
        .applyManifests(any(Kubectl.class), captor.capture(), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class), anyBoolean());
    kubernetesResourceList = captor.getValue();
    List<String> resoucesName = kubernetesResourceList.stream()
                                    .map(resource -> resource.getResourceId().getName())
                                    .collect(Collectors.toList());
    assertThat(kubernetesResourceList.get(0).getResourceId().isVersioned()).isTrue();
    assertThat(kubernetesResourceList.get(1).getResourceId().isVersioned()).isTrue();
    assertThat(resoucesName).containsExactlyInAnyOrder("mysecret-1", "deployment", "mycm-1");
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void testSkipVersioningForAllK8sObject() throws Exception {
    List<KubernetesResource> kubernetesResources = new ArrayList<>();
    kubernetesResources.addAll(ManifestHelper.processYaml(SECRET_YAML));
    kubernetesResources.addAll(ManifestHelper.processYaml(CONFIG_MAP_YAML));
    kubernetesResources.addAll(ManifestHelper.processYaml(DEPLOYMENT_YAML));

    K8sRollingDeployTaskHandler handler = spy(k8sRollingDeployTaskHandler);
    doReturn(kubernetesResources)
        .when(k8sTaskHelperBase)
        .readManifestAndOverrideLocalSecrets(anyList(), any(ExecutionLogCallback.class), anyBoolean());
    doReturn(true)
        .when(k8sTaskHelper)
        .fetchManifestFilesAndWriteToDirectory(
            any(K8sDelegateManifestConfig.class), anyString(), any(ExecutionLogCallback.class), anyLong());
    doReturn(true)
        .when(k8sTaskHelperBase)
        .dryRunManifests(
            any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class), any(ExecutionLogCallback.class));

    handler.executeTaskInternal(K8sRollingDeployTaskParameters.builder()
                                    .releaseName("releaseName")
                                    .isInCanaryWorkflow(false)
                                    .skipVersioningForAllK8sObjects(true)
                                    .build(),
        K8sDelegateTaskParams.builder().build());

    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    verify(k8sTaskHelperBase, times(1))
        .applyManifests(any(Kubectl.class), captor.capture(), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class), anyBoolean());
    List<KubernetesResource> kubernetesResourceList = captor.getValue();

    assertThat(kubernetesResourceList.get(0).getResourceId().isVersioned()).isFalse();
    assertThat(kubernetesResourceList.get(1).getResourceId().isVersioned()).isFalse();
    assertThat(kubernetesResourceList.get(2).getResourceId().isVersioned()).isFalse();
    List<String> resoucesName = kubernetesResourceList.stream()
                                    .map(resource -> resource.getResourceId().getName())
                                    .collect(Collectors.toList());
    assertThat(resoucesName).containsExactlyInAnyOrder("mysecret", "deployment", "mycm");
  }
}
