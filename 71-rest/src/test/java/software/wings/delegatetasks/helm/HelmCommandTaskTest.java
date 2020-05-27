package software.wings.delegatetasks.helm;

import static io.harness.rule.OwnerRule.ABOSII;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.TaskParameters;
import io.harness.rule.Owner;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.DelegateTaskPackage;
import software.wings.beans.TaskType;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.command.LogCallback;
import software.wings.beans.command.NoopExecutionCallback;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.helm.HelmCommandExecutionResponse;
import software.wings.helpers.ext.helm.HelmDeployService;
import software.wings.helpers.ext.helm.request.HelmCommandRequest;
import software.wings.helpers.ext.helm.request.HelmInstallCommandRequest;
import software.wings.helpers.ext.helm.request.HelmReleaseHistoryCommandRequest;
import software.wings.helpers.ext.helm.request.HelmRollbackCommandRequest;
import software.wings.helpers.ext.helm.response.HelmCommandResponse;
import software.wings.helpers.ext.helm.response.HelmInstallCommandResponse;
import software.wings.helpers.ext.helm.response.HelmReleaseHistoryCommandResponse;
import software.wings.service.impl.ContainerServiceParams;

import java.io.IOException;

public class HelmCommandTaskTest extends WingsBaseTest {
  @Mock private DelegateLogService delegateLogService;
  @Mock private HelmDeployService helmDeployService;
  @Mock private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Mock private HelmCommandHelper helmCommandHelper;
  @Mock private HelmCommandRequest dummyCommandRequest;

  @InjectMocks
  private final HelmCommandTask helmCommandTask = (HelmCommandTask) TaskType.HELM_COMMAND_TASK.getDelegateRunnableTask(
      DelegateTaskPackage.builder()
          .delegateId("delegateId")
          .delegateTask(DelegateTask.builder().data(TaskData.builder().async(false).build()).build())
          .build(),
      notifyResponseData -> {}, () -> true);

  @Before
  public void setup() {
    HelmCommandResponse ensureHelmInstalledResponse =
        new HelmCommandResponse(CommandExecutionStatus.SUCCESS, "Helm3 is installed at [mock]");
    doReturn(ensureHelmInstalledResponse).when(helmDeployService).ensureHelmInstalled(any(HelmCommandRequest.class));
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testInitPrerequisite() {
    String kubeConfigLocation = ".kube/config";

    doReturn(mock(LogCallback.class)).when(dummyCommandRequest).getExecutionLogCallback();
    doReturn(kubeConfigLocation)
        .when(containerDeploymentDelegateHelper)
        .createAndGetKubeConfigLocation(any(ContainerServiceParams.class));
    helmCommandTask.run(new Object[] {dummyCommandRequest});

    verify(helmDeployService, times(1)).ensureHelmInstalled(dummyCommandRequest);

    ArgumentCaptor<String> kubeLocationCaptor = ArgumentCaptor.forClass(String.class);
    verify(dummyCommandRequest, times(1)).setKubeConfigLocation(kubeLocationCaptor.capture());

    assertThat(kubeLocationCaptor.getValue()).isEqualTo(kubeConfigLocation);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testRunTaskWithInstallCommand() throws IOException {
    HelmInstallCommandRequest request = HelmInstallCommandRequest.builder().accountId("accountId").build();
    HelmInstallCommandResponse deployResponse =
        HelmInstallCommandResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).build();

    doReturn(deployResponse).when(helmDeployService).deploy(request);
    HelmCommandExecutionResponse response = helmCommandTask.run(new Object[] {request});

    verify(helmDeployService, times(1)).deploy(request);

    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    assertThat(response.getHelmCommandResponse()).isSameAs(deployResponse);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testRunTaskWithRollbackCommand() {
    HelmRollbackCommandRequest request = HelmRollbackCommandRequest.builder().accountId("accountId").build();
    HelmInstallCommandResponse rollbackResponse =
        HelmInstallCommandResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).build();

    doReturn(rollbackResponse).when(helmDeployService).rollback(request);
    HelmCommandExecutionResponse response = helmCommandTask.run(new Object[] {request});

    verify(helmDeployService, times(1)).rollback(request);

    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    assertThat(response.getHelmCommandResponse()).isSameAs(rollbackResponse);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testRunTaskWithReleaseHistoryCommand() {
    HelmReleaseHistoryCommandRequest request =
        HelmReleaseHistoryCommandRequest.builder().accountId("accountId").build();
    HelmReleaseHistoryCommandResponse releaseHistoryResponse =
        HelmReleaseHistoryCommandResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).build();

    doReturn(releaseHistoryResponse).when(helmDeployService).releaseHistory(request);
    HelmCommandExecutionResponse response = helmCommandTask.run(new Object[] {request});

    verify(helmDeployService, times(1)).releaseHistory(request);

    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    assertThat(response.getHelmCommandResponse()).isSameAs(releaseHistoryResponse);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testRunTaskWithException() throws IOException {
    HelmInstallCommandRequest request = HelmInstallCommandRequest.builder().accountId("accountId").build();

    doThrow(new IOException("Unable to deploy")).when(helmDeployService).deploy(request);
    HelmCommandExecutionResponse response = helmCommandTask.run(new Object[] {request});

    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
    assertThat(response.getErrorMessage()).isEqualTo("IOException: Unable to deploy");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testRunTaskWithFailure() throws IOException {
    HelmInstallCommandRequest request = HelmInstallCommandRequest.builder().accountId("accountId").build();
    HelmInstallCommandResponse deployResponse = HelmInstallCommandResponse.builder()
                                                    .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                                                    .output("Error while deploying")
                                                    .build();

    doReturn(deployResponse).when(helmDeployService).deploy(request);
    HelmCommandExecutionResponse response = helmCommandTask.run(new Object[] {request});

    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
    assertThat(response.getErrorMessage()).isEqualTo("Error while deploying");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetExecutionCallback() {
    HelmCommandTask helmCommandTask = getTask(false);
    HelmCommandTask asyncHelmCommandTask = getTask(true);
    ArgumentCaptor<LogCallback> logCallbackCaptor = ArgumentCaptor.forClass(LogCallback.class);

    doReturn(mock(LogCallback.class)).when(dummyCommandRequest).getExecutionLogCallback();
    helmCommandTask.run(new Object[] {dummyCommandRequest});
    verify(dummyCommandRequest, times(1)).setExecutionLogCallback(logCallbackCaptor.capture());
    assertThat(logCallbackCaptor.getValue()).isInstanceOf(NoopExecutionCallback.class);

    asyncHelmCommandTask.run(new Object[] {dummyCommandRequest});
    verify(dummyCommandRequest, times(2)).setExecutionLogCallback(logCallbackCaptor.capture());
    assertThat(logCallbackCaptor.getValue()).isInstanceOf(ExecutionLogCallback.class);
  }

  @Test(expected = NotImplementedException.class)
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testRunWithTaskParameters() {
    helmCommandTask.run(new TaskParameters() {});
  }

  private static HelmCommandTask getTask(boolean async) {
    return (HelmCommandTask) TaskType.HELM_COMMAND_TASK.getDelegateRunnableTask(
        DelegateTaskPackage.builder()
            .delegateId("delegateId")
            .delegateTask(DelegateTask.builder().data(TaskData.builder().async(async).build()).build())
            .build(),
        notifyResponseData -> {}, () -> true);
  }
}