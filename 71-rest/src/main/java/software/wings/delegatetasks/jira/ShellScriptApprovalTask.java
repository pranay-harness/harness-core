package software.wings.delegatetasks.jira;

import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.RUNNING;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.Log.LogLevel.INFO;

import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.shell.ShellScriptApprovalTaskParameters;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import software.wings.api.ShellScriptApprovalExecutionData;
import software.wings.beans.ApprovalDetails.Action;
import software.wings.beans.DelegateTaskPackage;
import software.wings.beans.command.ShellExecutionData;
import software.wings.core.local.executors.ShellExecutorConfig;
import software.wings.core.local.executors.ShellExecutorFactory;
import software.wings.core.ssh.executors.ScriptProcessExecutor;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.delegatetasks.DelegateLogService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ShellScriptApprovalTask extends AbstractDelegateRunnableTask {
  private static final String SCRIPT_APPROVAL_ENV_VARIABLE = "HARNESS_APPROVAL_STATUS";
  private static final String APPROVE_STATUS = "Approved";
  private static final String REJECTED_STATUS = "Rejected";

  @Inject private ShellExecutorFactory shellExecutorFactory;
  @Inject private DelegateLogService logService;

  public ShellScriptApprovalTask(
      DelegateTaskPackage delegateTaskPackage, Consumer<DelegateTaskResponse> consumer, Supplier<Boolean> preExecute) {
    super(delegateTaskPackage, consumer, preExecute);
  }

  @Override
  public ResponseData run(Object[] parameters) {
    return run((ShellScriptApprovalTaskParameters) parameters[0]);
  }

  @Override
  public ResponseData run(TaskParameters taskParameters) {
    ShellScriptApprovalTaskParameters parameters = (ShellScriptApprovalTaskParameters) taskParameters;

    ShellExecutorConfig shellExecutorConfig = ShellExecutorConfig.builder()
                                                  .accountId(parameters.getAccountId())
                                                  .appId(parameters.getAppId())
                                                  .executionId(parameters.getActivityId())
                                                  .commandUnitName(parameters.getCommandName())
                                                  .workingDirectory(parameters.getWorkingDirectory())
                                                  .environment(new HashMap<>())
                                                  .scriptType(parameters.getScriptType())
                                                  .build();

    ScriptProcessExecutor executor = shellExecutorFactory.getExecutor(shellExecutorConfig);
    List<String> items = new ArrayList<>();
    if (parameters.getOutputVars() != null && StringUtils.isNotEmpty(parameters.getOutputVars().trim())) {
      items = Arrays.asList(parameters.getOutputVars().split("\\s*,\\s*"));
      items.replaceAll(String::trim);
    }

    saveExecutionLog(parameters, "Starting Script Execution ...", RUNNING);
    CommandExecutionResult commandExecutionResult = executor.executeCommandString(parameters.getScript(), items);
    saveExecutionLog(parameters, "End of Script Execution ...", RUNNING);
    saveExecutionLog(parameters, "\n---------------------------------------------------\n", RUNNING);

    Action action = null;
    ExecutionStatus executionStatus = ExecutionStatus.RUNNING;
    if (SUCCESS == commandExecutionResult.getStatus()) {
      Map<String, String> sweepingOutputEnvVariables =
          ((ShellExecutionData) commandExecutionResult.getCommandExecutionData()).getSweepingOutputEnvVariables();

      if (MapUtils.isNotEmpty(sweepingOutputEnvVariables)
          && EmptyPredicate.isNotEmpty(sweepingOutputEnvVariables.get(SCRIPT_APPROVAL_ENV_VARIABLE))) {
        if (sweepingOutputEnvVariables.get(SCRIPT_APPROVAL_ENV_VARIABLE).equalsIgnoreCase(APPROVE_STATUS)) {
          action = Action.APPROVE;
          executionStatus = ExecutionStatus.SUCCESS;
        } else if (sweepingOutputEnvVariables.get(SCRIPT_APPROVAL_ENV_VARIABLE).equalsIgnoreCase(REJECTED_STATUS)) {
          action = Action.REJECT;
          executionStatus = ExecutionStatus.REJECTED;
        }
      }
    }

    String errorMessage;
    switch (executionStatus) {
      case SUCCESS:
        errorMessage = "Approved by Script";
        break;
      case REJECTED:
        errorMessage = "Rejected by Script";
        break;
      case RUNNING:
      default:
        errorMessage = "Waiting for Approval";
    }
    return ShellScriptApprovalExecutionData.builder()
        .approvalAction(action)
        .executionStatus(executionStatus)
        .errorMessage(errorMessage)
        .build();
  }

  private void saveExecutionLog(
      ShellScriptApprovalTaskParameters parameters, String line, CommandExecutionStatus commandExecutionStatus) {
    logService.save(parameters.getAccountId(),
        aLog()
            .withAppId(parameters.getAppId())
            .withActivityId(parameters.getActivityId())
            .withLogLevel(INFO)
            .withCommandUnitName(parameters.getCommandName())
            .withLogLine(line)
            .withExecutionResult(commandExecutionStatus)
            .build());
  }
}
