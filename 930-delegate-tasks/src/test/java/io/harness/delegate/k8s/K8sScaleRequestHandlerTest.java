package io.harness.delegate.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.rule.OwnerRule.ABOSII;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.NGInstanceUnitType;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.k8s.ContainerDeploymentDelegateBaseHelper;
import io.harness.delegate.task.k8s.DirectK8sInfraDelegateConfig;
import io.harness.delegate.task.k8s.K8sInfraDelegateConfig;
import io.harness.delegate.task.k8s.K8sScaleRequest;
import io.harness.delegate.task.k8s.K8sTaskHelperBase;
import io.harness.exception.InvalidRequestException;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.model.K8sDelegateTaskParams;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.logging.LogCallback;
import io.harness.rule.Owner;

import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(CDP)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class K8sScaleRequestHandlerTest extends CategoryTest {
  @Mock ContainerDeploymentDelegateBaseHelper containerDeploymentDelegateBaseHelper;
  @Mock K8sTaskHelperBase k8sTaskHelperBase;

  @InjectMocks K8sScaleRequestHandler k8sScaleRequestHandler;

  @Mock LogCallback logCallback;
  @Mock ILogStreamingTaskClient logStreamingTaskClient;
  @Mock KubernetesConfig kubernetesConfig;

  final CommandUnitsProgress commandUnitsProgress = CommandUnitsProgress.builder().build();

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);

    doReturn(logCallback)
        .when(k8sTaskHelperBase)
        .getLogCallback(any(ILogStreamingTaskClient.class), anyString(), anyBoolean(), any(CommandUnitsProgress.class));
    doReturn(kubernetesConfig)
        .when(containerDeploymentDelegateBaseHelper)
        .createKubernetesConfig(any(K8sInfraDelegateConfig.class));
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testExecuteTaskInternalFailedGetPods() throws Exception {
    K8sScaleRequest k8sScaleRequest =
        K8sScaleRequest.builder()
            .k8sInfraDelegateConfig(DirectK8sInfraDelegateConfig.builder().namespace("default").build())
            .releaseName("releaseName")
            .timeoutIntervalInMin(1)
            .workload("default/deployment/test-resource")
            .instanceUnitType(NGInstanceUnitType.COUNT)
            .instances(4)
            .build();
    K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().build();

    InvalidRequestException thrownException = new InvalidRequestException("Failed to get pods");

    doThrow(thrownException).when(k8sTaskHelperBase).getPodDetails(kubernetesConfig, "default", "releaseName", 60000);
    assertThatThrownBy(()
                           -> k8sScaleRequestHandler.executeTaskInternal(
                               k8sScaleRequest, delegateTaskParams, logStreamingTaskClient, commandUnitsProgress))
        .isEqualTo(thrownException);
    verify(logCallback).saveExecutionLog(thrownException.getMessage(), ERROR, FAILURE);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testExecuteTaskInternalFailedGetPodsAfterScale() throws Exception {
    K8sScaleRequest k8sScaleRequest =
        K8sScaleRequest.builder()
            .k8sInfraDelegateConfig(DirectK8sInfraDelegateConfig.builder().namespace("default").build())
            .releaseName("releaseName")
            .timeoutIntervalInMin(1)
            .workload("default/deployment/test-resource")
            .instanceUnitType(NGInstanceUnitType.COUNT)
            .instances(4)
            .skipSteadyStateCheck(true)
            .build();

    K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().build();
    Supplier<Boolean> shouldThrowException = new Supplier<Boolean>() {
      private int internalCount = 0;
      @Override
      public Boolean get() {
        internalCount++;
        return internalCount > 1;
      }
    };

    InvalidRequestException thrownException = new InvalidRequestException("Failed to get pods");

    doAnswer(invocation -> {
      if (shouldThrowException.get()) {
        throw thrownException;
      }

      return emptyList();
    })
        .when(k8sTaskHelperBase)
        .getPodDetails(kubernetesConfig, "default", "releaseName", 60000);
    doReturn(true)
        .when(k8sTaskHelperBase)
        .scale(any(Kubectl.class), eq(delegateTaskParams), any(KubernetesResourceId.class), anyInt(), eq(logCallback));

    assertThatThrownBy(()
                           -> k8sScaleRequestHandler.executeTaskInternal(
                               k8sScaleRequest, delegateTaskParams, logStreamingTaskClient, commandUnitsProgress))
        .isEqualTo(thrownException);

    verify(logCallback).saveExecutionLog(thrownException.getMessage(), ERROR, FAILURE);
    verify(k8sTaskHelperBase)
        .scale(any(Kubectl.class), eq(delegateTaskParams), any(KubernetesResourceId.class), anyInt(), eq(logCallback));
  }
}