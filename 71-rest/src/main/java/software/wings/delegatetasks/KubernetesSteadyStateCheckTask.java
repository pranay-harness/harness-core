package software.wings.delegatetasks;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.container.KubernetesSteadyStateCheckParams;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.sm.states.KubernetesSteadyStateCheckResponse;
import software.wings.utils.Misc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
@Slf4j
public class KubernetesSteadyStateCheckTask extends AbstractDelegateRunnableTask {
  @Inject private DelegateLogService delegateLogService;
  @Inject private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Inject private TimeLimiter timeLimiter;

  public KubernetesSteadyStateCheckTask(String delegateId, DelegateTask delegateTask,
      Consumer<DelegateTaskResponse> consumer, Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public KubernetesSteadyStateCheckResponse run(TaskParameters parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public KubernetesSteadyStateCheckResponse run(Object[] parameters) {
    KubernetesSteadyStateCheckParams kubernetesSteadyStateCheckParams =
        (KubernetesSteadyStateCheckParams) parameters[0];

    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback(delegateLogService,
        kubernetesSteadyStateCheckParams.getAccountId(), kubernetesSteadyStateCheckParams.getAppId(),
        kubernetesSteadyStateCheckParams.getActivityId(), kubernetesSteadyStateCheckParams.getCommandName());

    if (containerDeploymentDelegateHelper.getControllerCountByLabels(
            kubernetesSteadyStateCheckParams.getContainerServiceParams(),
            containerDeploymentDelegateHelper.getKubernetesConfig(
                kubernetesSteadyStateCheckParams.getContainerServiceParams()),
            kubernetesSteadyStateCheckParams.getLabels())
        == 0) {
      String msg = String.format(
          "No controller found for the specified Labels: %s", kubernetesSteadyStateCheckParams.getLabels());
      executionLogCallback.saveExecutionLog(msg, LogLevel.ERROR, CommandExecutionStatus.FAILURE);
      return KubernetesSteadyStateCheckResponse.builder().executionStatus(ExecutionStatus.FAILED).build();
    }

    List<ContainerInfo> containerInfos = new ArrayList<>();
    try {
      timeLimiter.callWithTimeout(
          ()
              -> containerInfos.addAll(doSteadyStateCheck(kubernetesSteadyStateCheckParams, executionLogCallback)),
          kubernetesSteadyStateCheckParams.getTimeoutMillis(), TimeUnit.MILLISECONDS, true);

      executionLogCallback.saveExecutionLog(
          "Command finished with status " + ExecutionStatus.SUCCESS, LogLevel.INFO, CommandExecutionStatus.SUCCESS);

      return KubernetesSteadyStateCheckResponse.builder()
          .executionStatus(ExecutionStatus.SUCCESS)
          .containerInfoList(containerInfos)
          .build();
    } catch (UncheckedTimeoutException e) {
      String msg = "Timed out waiting for controller to reach steady state";
      logger.error(msg, e);
      executionLogCallback.saveExecutionLog(msg, LogLevel.ERROR, CommandExecutionStatus.FAILURE);
      return KubernetesSteadyStateCheckResponse.builder().executionStatus(ExecutionStatus.FAILED).build();
    } catch (WingsException e) {
      Misc.logAllMessages(e, executionLogCallback);
      throw e;
    } catch (Exception e) {
      logger.error("Exception in KubernetesSteadyStateCheck", e);
      Misc.logAllMessages(e, executionLogCallback);
      executionLogCallback.saveExecutionLog("Exception occurred while waiting for controller to reach steady state",
          LogLevel.ERROR, CommandExecutionStatus.FAILURE);
      return KubernetesSteadyStateCheckResponse.builder().executionStatus(ExecutionStatus.FAILED).build();
    }
  }

  private List<ContainerInfo> doSteadyStateCheck(
      KubernetesSteadyStateCheckParams kubernetesSteadyStateCheckParams, ExecutionLogCallback executionLogCallback) {
    KubernetesConfig kubernetesConfig = containerDeploymentDelegateHelper.getKubernetesConfig(
        kubernetesSteadyStateCheckParams.getContainerServiceParams());

    return containerDeploymentDelegateHelper.getContainerInfosWhenReadyByLabels(
        kubernetesSteadyStateCheckParams.getContainerServiceParams(), kubernetesConfig, executionLogCallback,
        kubernetesSteadyStateCheckParams.getLabels(), Collections.emptyList());
  }
}
