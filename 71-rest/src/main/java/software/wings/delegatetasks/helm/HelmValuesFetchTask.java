package software.wings.delegatetasks.helm;

import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static java.lang.String.format;
import static software.wings.beans.Log.LogColor.White;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.Log.LogLevel.WARN;
import static software.wings.beans.Log.LogWeight.Bold;
import static software.wings.beans.Log.color;
import static software.wings.beans.command.K8sDummyCommandUnit.FetchFiles;

import com.google.inject.Inject;

import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.TaskParameters;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.beans.DelegateTaskPackage;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.helpers.ext.helm.request.HelmChartConfigParams;
import software.wings.helpers.ext.helm.request.HelmValuesFetchTaskParameters;
import software.wings.helpers.ext.helm.response.HelmValuesFetchTaskResponse;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@Slf4j
public class HelmValuesFetchTask extends AbstractDelegateRunnableTask {
  @Inject private HelmTaskHelper helmTaskHelper;
  @Inject private DelegateLogService delegateLogService;

  public HelmValuesFetchTask(
      DelegateTaskPackage delegateTaskPackage, Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, consumer, preExecute);
  }

  @Override
  public HelmValuesFetchTaskResponse run(TaskParameters parameters) {
    HelmValuesFetchTaskParameters taskParams = (HelmValuesFetchTaskParameters) parameters;
    logger.info(
        format("Running HelmValuesFetchTask for account %s app %s", taskParams.getAccountId(), taskParams.getAppId()));

    ExecutionLogCallback executionLogCallback = getExecutionLogCallback(taskParams, FetchFiles);
    try {
      executionLogCallback.saveExecutionLog(color("\nFetching values.yaml from helm chart for Service", White, Bold));

      HelmChartConfigParams helmChartConfigParams = taskParams.getHelmChartConfigTaskParams();

      String valuesFileContent =
          helmTaskHelper.getValuesYamlFromChart(helmChartConfigParams, taskParams.getTimeoutInMillis());
      helmTaskHelper.printHelmChartInfoInExecutionLogs(helmChartConfigParams, executionLogCallback);

      if (null == valuesFileContent) {
        executionLogCallback.saveExecutionLog("No values.yaml found", WARN, SUCCESS);
      } else {
        executionLogCallback.saveExecutionLog("\nSuccessfully fetched values.yaml", INFO, SUCCESS);
      }

      return HelmValuesFetchTaskResponse.builder()
          .commandExecutionStatus(SUCCESS)
          .valuesFileContent(valuesFileContent)
          .build();
    } catch (Exception e) {
      logger.error("HelmValuesFetchTask execution failed with exception ", e);
      executionLogCallback.saveExecutionLog(e.getMessage(), ERROR, CommandExecutionStatus.FAILURE);

      return HelmValuesFetchTaskResponse.builder()
          .commandExecutionStatus(FAILURE)
          .errorMessage("Execution failed with Exception: " + e.getMessage())
          .build();
    }
  }

  private ExecutionLogCallback getExecutionLogCallback(HelmValuesFetchTaskParameters taskParams, String commandUnit) {
    return new ExecutionLogCallback(
        delegateLogService, taskParams.getAccountId(), taskParams.getAppId(), taskParams.getActivityId(), commandUnit);
  }

  @Override
  public HelmValuesFetchTaskResponse run(Object[] parameters) {
    throw new NotImplementedException("Not implemented");
  }
}
