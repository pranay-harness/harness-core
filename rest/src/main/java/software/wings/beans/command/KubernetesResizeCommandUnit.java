package software.wings.beans.command;

import software.wings.api.DeploymentType;
import software.wings.beans.ErrorCode;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.exception.WingsException;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.Validator;

import javax.inject.Inject;

import static java.lang.String.format;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static software.wings.beans.command.KubernetesResizeCommandUnitExecutionData.Builder.aKubernetesResizeCommandUnitExecutionData;

/**
 * Created by brett on 3/3/17
 */
public class KubernetesResizeCommandUnit extends ContainerOrchestrationCommandUnit {
  @Inject private transient DelegateLogService logService;

  public KubernetesResizeCommandUnit() {
    super(CommandUnitType.RESIZE_KUBERNENTES);
    setDeploymentType(DeploymentType.KUBERNETES.name());
  }

  @Override
  public CommandExecutionStatus execute(CommandExecutionContext context) {
    SettingAttribute cloudProviderSetting = context.getCloudProviderSetting();
    Validator.equalCheck(cloudProviderSetting.getValue().getType(), SettingVariableTypes.GCP.name());
    String clusterName = context.getClusterName();
    String serviceName = context.getServiceName();
    Integer desiredCount = context.getDesiredCount();
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback(context, getName());
    executionLogCallback.setLogService(logService);

    executionLogCallback.saveExecutionLog(format("Begin execution of command: %s", getName()), INFO);
    CommandExecutionStatus commandExecutionStatus = FAILURE;

    try {
      kubernetesContainerService.setControllerPodCount(cloudProviderSetting, serviceName, desiredCount);
      context.setCommandExecutionData(aKubernetesResizeCommandUnitExecutionData().build());
      commandExecutionStatus = SUCCESS;
    } catch (Exception ex) {
      executionLogCallback.saveExecutionLog("Command execution failed", ERROR);
      throw new WingsException(ErrorCode.UNKNOWN_ERROR, "", ex);
    }
    return commandExecutionStatus;
  }
}
