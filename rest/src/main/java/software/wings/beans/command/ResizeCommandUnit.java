package software.wings.beans.command;

import static java.lang.String.format;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.command.CommandExecutionResult.AbstractCommandUnit.CommandExecutionStatus.FAILURE;
import static software.wings.beans.command.CommandExecutionResult.AbstractCommandUnit.CommandExecutionStatus.SUCCESS;

import software.wings.beans.ErrorCode;
import software.wings.beans.SettingAttribute;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.exception.WingsException;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.Validator;

import javax.inject.Inject;
import java.util.List;

/**
 * Created by peeyushaggarwal on 2/3/17.
 */
public class ResizeCommandUnit extends ContainerOrchestrationCommandUnit {
  @Inject private transient DelegateLogService logService;

  public ResizeCommandUnit() {
    super(CommandUnitType.RESIZE);
  }

  @Override
  public CommandExecutionStatus execute(CommandExecutionContext context) {
    SettingAttribute cloudProviderSetting = context.getCloudProviderSetting();
    Validator.equalCheck(cloudProviderSetting.getValue().getType(), SettingVariableTypes.AWS.name());
    String clusterName = context.getClusterName();
    String serviceName = context.getServiceName();
    Integer desiredCount = context.getDesiredCount();
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback(context, getName());
    executionLogCallback.setLogService(logService);

    executionLogCallback.saveExecutionLog(format("Begin execution of command: %s", getName()), INFO);
    CommandExecutionStatus commandExecutionStatus = FAILURE;

    try {
      List<String> containerInstanceArns = awsClusterService.resizeCluster(
          cloudProviderSetting, clusterName, serviceName, desiredCount, executionLogCallback);
      commandExecutionStatus = SUCCESS;
    } catch (Exception ex) {
      executionLogCallback.saveExecutionLog("Command execution failed", ERROR);
      throw new WingsException(ErrorCode.UNKNOWN_ERROR, "", ex);
    }
    return commandExecutionStatus;
  }
}
