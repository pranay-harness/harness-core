package io.harness.delegate.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.delegate.k8s.K8sTestHelper.deployment;
import static io.harness.delegate.k8s.K8sTestHelper.service;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.rule.OwnerRule.ABOSII;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
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
import io.harness.delegate.task.k8s.K8sBGDeployRequest;
import io.harness.delegate.task.k8s.K8sInfraDelegateConfig;
import io.harness.delegate.task.k8s.K8sTaskHelperBase;
import io.harness.delegate.task.k8s.ManifestDelegateConfig;
import io.harness.exception.InvalidRequestException;
import io.harness.k8s.KubernetesContainerService;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.model.HarnessLabelValues;
import io.harness.k8s.model.K8sDelegateTaskParams;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.logging.LogCallback;
import io.harness.rule.Owner;

import java.util.ArrayList;
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
public class K8sBGRequestHandlerTest extends CategoryTest {
  @Mock ContainerDeploymentDelegateBaseHelper containerDeploymentDelegateBaseHelper;
  @Mock K8sTaskHelperBase k8sTaskHelperBase;
  @Mock KubernetesContainerService kubernetesContainerService;

  @Mock LogCallback logCallback;
  @Mock ILogStreamingTaskClient logStreamingTaskClient;
  @Mock KubernetesConfig kubernetesConfig;

  @Spy @InjectMocks K8sBGBaseHandler k8sBGBaseHandler;
  @Spy @InjectMocks K8sBGRequestHandler k8sBGRequestHandler;

  K8sDelegateTaskParams k8sDelegateTaskParams;
  CommandUnitsProgress commandUnitsProgress;

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);

    k8sDelegateTaskParams = K8sDelegateTaskParams.builder().build();
    commandUnitsProgress = CommandUnitsProgress.builder().build();

    doReturn(logCallback)
        .when(k8sTaskHelperBase)
        .getLogCallback(eq(logStreamingTaskClient), anyString(), anyBoolean(), eq(commandUnitsProgress));
    doReturn(true)
        .when(k8sTaskHelperBase)
        .fetchManifestFilesAndWriteToDirectory(
            any(ManifestDelegateConfig.class), anyString(), eq(logCallback), anyLong(), anyString());
    doReturn(true)
        .when(k8sTaskHelperBase)
        .applyManifests(any(Kubectl.class), anyListOf(KubernetesResource.class), eq(k8sDelegateTaskParams),
            eq(logCallback), anyBoolean());
    doReturn(true)
        .when(k8sTaskHelperBase)
        .doStatusCheck(any(Kubectl.class), any(KubernetesResourceId.class), eq(k8sDelegateTaskParams), eq(logCallback));

    doReturn(true)
        .when(k8sTaskHelperBase)
        .dryRunManifests(
            any(Kubectl.class), anyListOf(KubernetesResource.class), eq(k8sDelegateTaskParams), eq(logCallback));

    doReturn(kubernetesConfig)
        .when(containerDeploymentDelegateBaseHelper)
        .createKubernetesConfig(any(K8sInfraDelegateConfig.class));
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void shouldCatchGetPodDetailsException() throws Exception {
    K8sBGDeployRequest k8sBGDeployRequest =
        K8sBGDeployRequest.builder().skipResourceVersioning(true).releaseName("releaseName").build();
    InvalidRequestException thrownException = new InvalidRequestException("Failed to get pod details");

    doReturn(HarnessLabelValues.colorBlue)
        .when(k8sBGBaseHandler)
        .getPrimaryColor(any(KubernetesResource.class), eq(kubernetesConfig), eq(logCallback));
    doReturn(new ArrayList<>(asList(deployment(), service())))
        .when(k8sTaskHelperBase)
        .readManifests(anyListOf(FileData.class), eq(logCallback));
    doThrow(thrownException)
        .when(k8sBGBaseHandler)
        .getAllPods(anyLong(), eq(kubernetesConfig), any(KubernetesResource.class), eq(HarnessLabelValues.colorBlue),
            eq(HarnessLabelValues.colorGreen), eq("releaseName"));

    assertThatThrownBy(()
                           -> k8sBGRequestHandler.executeTaskInternal(
                               k8sBGDeployRequest, k8sDelegateTaskParams, logStreamingTaskClient, commandUnitsProgress))
        .isEqualTo(thrownException);

    verify(logCallback, atLeastOnce()).saveExecutionLog(thrownException.getMessage(), ERROR, FAILURE);
    verify(k8sTaskHelperBase, times(2))
        .saveReleaseHistoryInConfigMap(any(KubernetesConfig.class), anyString(), anyString());
  }
}