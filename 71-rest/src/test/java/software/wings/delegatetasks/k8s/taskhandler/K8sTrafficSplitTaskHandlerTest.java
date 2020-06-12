package software.wings.delegatetasks.k8s.taskhandler;

import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.BOJANA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Log.LogLevel.ERROR;

import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.harness.category.element.UnitTests;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.InvalidArgumentsException;
import io.harness.k8s.model.HarnessAnnotations;
import io.harness.k8s.model.K8sExpressions;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.k8s.model.ReleaseHistory;
import io.harness.rule.Owner;
import me.snowdrop.istio.api.IstioResource;
import me.snowdrop.istio.api.networking.v1alpha3.VirtualService;
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
import software.wings.beans.k8s.istio.IstioDestinationWeight;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.delegatetasks.k8s.K8sDelegateTaskParams;
import software.wings.delegatetasks.k8s.K8sTaskHelper;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;
import software.wings.helpers.ext.k8s.request.K8sTrafficSplitTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskResponse;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class K8sTrafficSplitTaskHandlerTest extends WingsBaseTest {
  @Mock private K8sTaskHelper k8sTaskHelper;
  @Mock private ExecutionLogCallback executionLogCallback;
  @Mock private KubernetesContainerService kubernetesContainerService;
  @Mock private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @InjectMocks private K8sTrafficSplitTaskHandler k8sTrafficSplitTaskHandler;

  private static final String RELEASE_NAME = "releaseName";
  private static final String VIRTUAL_SERVICE = "virtualService";

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testNullGetManagedVirtualServiceResources() throws Exception {
    K8sTrafficSplitTaskParameters k8sTrafficSplitTaskParams =
        K8sTrafficSplitTaskParameters.builder()
            .releaseName(RELEASE_NAME)
            .virtualServiceName(K8sExpressions.virtualServiceNameExpression)
            .build();

    ReleaseHistory releaseHistory = ReleaseHistory.createNew();
    releaseHistory.createNewRelease(ImmutableList.of(
        KubernetesResourceId.builder().kind("VirtualService").name(VIRTUAL_SERVICE).namespace("default").build()));

    KubernetesConfig kubernetesConfig = new KubernetesConfig();

    on(k8sTrafficSplitTaskHandler).set("kubernetesConfig", kubernetesConfig);

    when(containerDeploymentDelegateHelper.getKubernetesConfig(any(K8sClusterConfig.class)))
        .thenReturn(KubernetesConfig.builder().build());
    when(kubernetesContainerService.fetchReleaseHistory(any(KubernetesConfig.class), anyList(), anyString()))
        .thenReturn(releaseHistory.getAsYaml());
    when(kubernetesContainerService.getIstioVirtualService(any(KubernetesConfig.class), anyList(), anyString()))
        .thenReturn(null);

    boolean status = k8sTrafficSplitTaskHandler.init(k8sTrafficSplitTaskParams, executionLogCallback);
    ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<LogLevel> logLevelCaptor = ArgumentCaptor.forClass(LogLevel.class);
    ArgumentCaptor<CommandExecutionStatus> commandExecutionStatusCaptor =
        ArgumentCaptor.forClass(CommandExecutionStatus.class);
    verify(executionLogCallback, times(2))
        .saveExecutionLog(msgCaptor.capture(), logLevelCaptor.capture(), commandExecutionStatusCaptor.capture());
    assertThat(status).isFalse();
    assertThat(logLevelCaptor.getValue()).isEqualTo(ERROR);
    assertThat(commandExecutionStatusCaptor.getValue()).isEqualTo(FAILURE);
    assertThat(msgCaptor.getAllValues().get(0)).isEqualTo("Error evaluating expression ${k8s.virtualServiceName}");
    assertThat(msgCaptor.getAllValues().get(1))
        .isEqualTo("\n"
            + "No managed VirtualService found. Atleast one VirtualService should be present and marked with annotation harness.io/managed: true");
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void invalidTypeOfTaskParams() {
    assertThatExceptionOfType(InvalidArgumentsException.class)
        .isThrownBy(() -> k8sTrafficSplitTaskHandler.executeTaskInternal(null, null))
        .withMessageContaining("INVALID_ARGUMENT");
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void initBasedOnCustomVirtualServiceNameFail() {
    K8sTrafficSplitTaskParameters k8sTrafficSplitTaskParams =
        K8sTrafficSplitTaskParameters.builder().virtualServiceName("customVirtualServiceName").build();

    boolean status = k8sTrafficSplitTaskHandler.init(k8sTrafficSplitTaskParams, executionLogCallback);
    verify(containerDeploymentDelegateHelper, times(1)).getKubernetesConfig(any(K8sClusterConfig.class));
    verify(kubernetesContainerService, times(1))
        .getIstioVirtualService(any(KubernetesConfig.class), anyList(), anyString());
    assertThat(status).isFalse();
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void applySuccess() {
    K8sTrafficSplitTaskHandler handler = Mockito.spy(k8sTrafficSplitTaskHandler);
    doReturn(true).when(handler).init(any(K8sTrafficSplitTaskParameters.class), any(ExecutionLogCallback.class));

    final K8sTaskExecutionResponse response = handler.executeTaskInternal(
        K8sTrafficSplitTaskParameters.builder().build(), K8sDelegateTaskParams.builder().build());
    verify(handler, times(1)).init(any(K8sTrafficSplitTaskParameters.class), any(ExecutionLogCallback.class));
    verify(containerDeploymentDelegateHelper, times(1)).getKubernetesConfig(any(K8sClusterConfig.class));
    verify(kubernetesContainerService, times(1))
        .createOrReplaceIstioResource(any(KubernetesConfig.class), anyList(), any(IstioResource.class));
    verify(k8sTaskHelper, times(1))
        .getK8sTaskExecutionResponse(
            any(K8sTaskResponse.class), any(CommandExecutionResult.CommandExecutionStatus.class));
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void printDestinationWeights() {
    K8sTrafficSplitTaskParameters k8sTrafficSplitTaskParams =
        K8sTrafficSplitTaskParameters.builder()
            .virtualServiceName(K8sExpressions.virtualServiceNameExpression)
            .istioDestinationWeights(Arrays.asList(new IstioDestinationWeight()))
            .build();
    when(kubernetesContainerService.fetchReleaseHistory(any(KubernetesConfig.class), anyList(), anyString()))
        .thenReturn(null);
    k8sTrafficSplitTaskHandler.executeTaskInternal(k8sTrafficSplitTaskParams, K8sDelegateTaskParams.builder().build());
    verify(containerDeploymentDelegateHelper, times(2)).getKubernetesConfig(any(K8sClusterConfig.class));
    verify(kubernetesContainerService, times(1))
        .fetchReleaseHistory(any(KubernetesConfig.class), anyList(), anyString());
    verify(kubernetesContainerService, times(1))
        .fetchReleaseHistory(any(KubernetesConfig.class), anyList(), anyString());
    verify(kubernetesContainerService, times(1))
        .createOrReplaceIstioResource(any(KubernetesConfig.class), anyList(), any(IstioResource.class));
    verify(k8sTaskHelper, times(1))
        .getK8sTaskExecutionResponse(
            any(K8sTaskResponse.class), any(CommandExecutionResult.CommandExecutionStatus.class));
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void getManagedVirtualServiceResources() throws Exception {
    K8sTrafficSplitTaskParameters k8sTrafficSplitTaskParams =
        K8sTrafficSplitTaskParameters.builder().virtualServiceName(K8sExpressions.virtualServiceNameExpression).build();
    ReleaseHistory releaseHistory = ReleaseHistory.createNew();
    releaseHistory.createNewRelease(ImmutableList.of(
        KubernetesResourceId.builder().kind("VirtualService").name(VIRTUAL_SERVICE).namespace("default").build()));
    when(kubernetesContainerService.fetchReleaseHistory(any(KubernetesConfig.class), anyList(), anyString()))
        .thenReturn(releaseHistory.getAsYaml());
    VirtualService istioVirtualService = Mockito.mock(VirtualService.class);
    ObjectMeta metadata = Mockito.mock(ObjectMeta.class);
    Map<String, String> annotations = new HashMap<>();
    annotations.put(HarnessAnnotations.managed, "true");
    when(istioVirtualService.getMetadata()).thenReturn(metadata);
    when(metadata.getAnnotations()).thenReturn(annotations);
    when(kubernetesContainerService.getIstioVirtualService(any(KubernetesConfig.class), anyList(), anyString()))
        .thenReturn(istioVirtualService);
    boolean status = k8sTrafficSplitTaskHandler.init(k8sTrafficSplitTaskParams, executionLogCallback);
    verify(containerDeploymentDelegateHelper, times(1)).getKubernetesConfig(any(K8sClusterConfig.class));
    verify(kubernetesContainerService, times(1))
        .fetchReleaseHistory(any(KubernetesConfig.class), anyList(), anyString());
    verify(kubernetesContainerService, times(2))
        .getIstioVirtualService(any(KubernetesConfig.class), anyList(), anyString());
    assertThat(status).isTrue();
  }
}
