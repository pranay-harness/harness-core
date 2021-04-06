package io.harness.delegate.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.delegate.k8s.K8sTestHelper.deployment;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.rule.OwnerRule.ABOSII;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FileData;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.k8s.ContainerDeploymentDelegateBaseHelper;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sInfraDelegateConfig;
import io.harness.delegate.task.k8s.K8sRollingDeployRequest;
import io.harness.delegate.task.k8s.K8sTaskHelperBase;
import io.harness.delegate.task.k8s.ManifestDelegateConfig;
import io.harness.exception.InvalidRequestException;
import io.harness.k8s.KubernetesContainerService;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.model.K8sDelegateTaskParams;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.logging.LogCallback;
import io.harness.rule.Owner;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

@OwnedBy(CDP)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class K8sRollingRequestHandlerTest extends CategoryTest {
  @Mock KubernetesContainerService kubernetesContainerService;
  @Mock K8sTaskHelperBase taskHelperBase;
  @Mock ContainerDeploymentDelegateBaseHelper containerDeploymentDelegateBaseHelper;

  @InjectMocks @Spy K8sRollingBaseHandler baseHandler;
  @InjectMocks K8sRollingRequestHandler rollingRequestHandler;

  @Mock ILogStreamingTaskClient logStreamingTaskClient;
  @Mock LogCallback logCallback;

  final CommandUnitsProgress commandUnitsProgress = CommandUnitsProgress.builder().build();

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);

    doReturn(logCallback)
        .when(taskHelperBase)
        .getLogCallback(eq(logStreamingTaskClient), anyString(), anyBoolean(), eq(commandUnitsProgress));

    doReturn(true)
        .when(taskHelperBase)
        .fetchManifestFilesAndWriteToDirectory(
            any(ManifestDelegateConfig.class), anyString(), any(LogCallback.class), anyLong(), anyString());

    doReturn(true)
        .when(taskHelperBase)
        .applyManifests(any(Kubectl.class), anyListOf(KubernetesResource.class), any(K8sDelegateTaskParams.class),
            any(LogCallback.class), anyBoolean());

    doReturn(true)
        .when(taskHelperBase)
        .doStatusCheckForAllResources(any(Kubectl.class), anyListOf(KubernetesResourceId.class),
            any(K8sDelegateTaskParams.class), anyString(), any(LogCallback.class), anyBoolean());

    doReturn(KubernetesConfig.builder().namespace("default").build())
        .when(containerDeploymentDelegateBaseHelper)
        .createKubernetesConfig(any(K8sInfraDelegateConfig.class));

    doReturn(true)
        .when(taskHelperBase)
        .dryRunManifests(any(Kubectl.class), anyListOf(KubernetesResource.class), any(K8sDelegateTaskParams.class),
            any(LogCallback.class));
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testExecuteTask() throws Exception {
    K8sRollingDeployRequest rollingDeployRequest = K8sRollingDeployRequest.builder().releaseName("releaseName").build();
    K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().build();

    doReturn(singletonList(deployment()))
        .when(taskHelperBase)
        .readManifestAndOverrideLocalSecrets(anyListOf(FileData.class), eq(logCallback), anyBoolean());

    K8sDeployResponse response = rollingRequestHandler.executeTask(
        rollingDeployRequest, delegateTaskParams, logStreamingTaskClient, commandUnitsProgress);
    assertThat(response.getCommandExecutionStatus()).isEqualTo(SUCCESS);
    assertThat(response.getK8sNGTaskResponse()).isNotNull();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testExecuteTaskInternalGetPodsFailed() throws Exception {
    K8sRollingDeployRequest rollingDeployRequest = K8sRollingDeployRequest.builder().releaseName("releaseName").build();
    K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().build();
    InvalidRequestException thrownException = new InvalidRequestException("Failed to get pods");

    doReturn(singletonList(deployment()))
        .when(taskHelperBase)
        .readManifestAndOverrideLocalSecrets(anyListOf(FileData.class), eq(logCallback), anyBoolean());

    doReturn(emptyList())
        .when(baseHandler)
        .getExistingPods(anyLong(), anyListOf(KubernetesResource.class), any(KubernetesConfig.class), anyString(),
            any(LogCallback.class));
    doThrow(thrownException)
        .when(baseHandler)
        .getPods(anyLong(), anyListOf(KubernetesResource.class), any(KubernetesConfig.class), anyString());

    assertThatThrownBy(()
                           -> rollingRequestHandler.executeTaskInternal(
                               rollingDeployRequest, delegateTaskParams, logStreamingTaskClient, commandUnitsProgress))
        .isEqualTo(thrownException);

    verify(logCallback).saveExecutionLog(thrownException.getMessage(), ERROR, FAILURE);
    verify(kubernetesContainerService, times(2))
        .saveReleaseHistoryInConfigMap(any(KubernetesConfig.class), anyString(), anyString());
  }
}