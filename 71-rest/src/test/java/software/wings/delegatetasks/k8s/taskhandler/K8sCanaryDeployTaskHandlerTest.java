package software.wings.delegatetasks.k8s.taskhandler;

import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.rule.OwnerRule.ANSHUL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.delegatetasks.k8s.K8sTestConstants.DAEMON_SET_YAML;
import static software.wings.delegatetasks.k8s.K8sTestConstants.DEPLOYMENT_DIRECT_APPLY_YAML;
import static software.wings.delegatetasks.k8s.K8sTestConstants.DEPLOYMENT_YAML;
import static software.wings.delegatetasks.k8s.K8sTestConstants.STATEFUL_SET_YAML;

import io.harness.category.element.UnitTests;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.k8s.manifest.ManifestHelper;
import io.harness.k8s.model.Kind;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.k8s.model.ReleaseHistory;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class K8sCanaryDeployTaskHandlerTest extends WingsBaseTest {
  @Mock private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Mock private KubernetesContainerService kubernetesContainerService;
  @Mock private K8sTaskHelper k8sTaskHelper;
  @Mock private ExecutionLogCallback executionLogCallback;
  @InjectMocks private K8sCanaryDeployTaskHandler k8sCanaryDeployTaskHandler;

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
}
