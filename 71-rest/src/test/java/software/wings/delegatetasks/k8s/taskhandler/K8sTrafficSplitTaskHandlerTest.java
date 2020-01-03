package software.wings.delegatetasks.k8s.taskhandler;

import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.rule.OwnerRule.ANSHUL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Log.LogLevel.ERROR;

import com.google.common.collect.ImmutableList;

import io.harness.category.element.UnitTests;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.k8s.model.K8sExpressions;
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
import software.wings.delegatetasks.k8s.K8sTaskHelper;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;
import software.wings.helpers.ext.k8s.request.K8sTrafficSplitTaskParameters;

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
}
