package software.wings.delegatetasks.k8s.taskhandler;

import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.delegatetasks.k8s.K8sDelegateTaskParams;
import software.wings.helpers.ext.k8s.request.K8sApplyTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class K8sTaskHandlerTest extends WingsBaseTest {
  @Spy private K8sTaskHandler k8sTaskHandler;

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void testExceptionHandlingInExecuteTask() throws Exception {
    K8sTaskParameters k8sTaskParameters = K8sApplyTaskParameters.builder().build();
    K8sDelegateTaskParams k8sDelegateTaskParams = K8sDelegateTaskParams.builder().build();
    K8sTaskExecutionResponse response;

    // handle IO exception
    doThrow(new IOException())
        .when(k8sTaskHandler)
        .executeTaskInternal(any(K8sTaskParameters.class), any(K8sDelegateTaskParams.class));

    response = k8sTaskHandler.executeTask(k8sTaskParameters, k8sDelegateTaskParams);
    assertThat(response.getCommandExecutionStatus()).isEqualTo(FAILURE);
    assertThat(response.getErrorMessage()).contains("Could not complete k8s task due to IO exception");

    // handle thread interrupts
    doThrow(new InterruptedException())
        .when(k8sTaskHandler)
        .executeTaskInternal(any(K8sTaskParameters.class), any(K8sDelegateTaskParams.class));

    response = k8sTaskHandler.executeTask(k8sTaskParameters, k8sDelegateTaskParams);
    assertThat(response.getCommandExecutionStatus()).isEqualTo(FAILURE);
    assertThat(response.getErrorMessage()).contains("Interrupted while waiting for k8s task to complete");

    // handle timeout exceptions
    doThrow(new TimeoutException())
        .when(k8sTaskHandler)
        .executeTaskInternal(any(K8sTaskParameters.class), any(K8sDelegateTaskParams.class));

    response = k8sTaskHandler.executeTask(k8sTaskParameters, k8sDelegateTaskParams);
    assertThat(response.getCommandExecutionStatus()).isEqualTo(FAILURE);
    assertThat(response.getErrorMessage()).contains("Timed out while waiting for k8s task to complete");

    // hanlde wings exceptions
    doThrow(new InvalidRequestException("k8s-error"))
        .when(k8sTaskHandler)
        .executeTaskInternal(any(K8sTaskParameters.class), any(K8sDelegateTaskParams.class));

    response = k8sTaskHandler.executeTask(k8sTaskParameters, k8sDelegateTaskParams);
    assertThat(response.getCommandExecutionStatus()).isEqualTo(FAILURE);
    assertThat(response.getErrorMessage()).contains("k8s-error");

    // handle other exceptions
    doThrow(new Exception())
        .when(k8sTaskHandler)
        .executeTaskInternal(any(K8sTaskParameters.class), any(K8sDelegateTaskParams.class));

    response = k8sTaskHandler.executeTask(k8sTaskParameters, k8sDelegateTaskParams);
    assertThat(response.getCommandExecutionStatus()).isEqualTo(FAILURE);
    assertThat(response.getErrorMessage()).contains("Failed to complete k8s task");
  }
}