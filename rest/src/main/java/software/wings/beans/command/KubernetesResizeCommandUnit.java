package software.wings.beans.command;

import static java.lang.String.format;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static software.wings.beans.command.ResizeCommandUnitExecutionData.ResizeCommandUnitExecutionDataBuilder.aResizeCommandUnitExecutionData;

import com.google.common.collect.Multimap;

import software.wings.api.DeploymentType;
import software.wings.beans.ErrorCode;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.exception.WingsException;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.Validator;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

/**
 * Created by brett on 3/3/17
 */
public class KubernetesResizeCommandUnit extends ContainerOrchestrationCommandUnit {
  @Inject private transient DelegateLogService logService;

  public KubernetesResizeCommandUnit() {
    super(CommandUnitType.RESIZE_KUBERNETES);
    setDeploymentType(DeploymentType.KUBERNETES.name());
  }

  @Override
  public CommandExecutionStatus execute(CommandExecutionContext context) {
    SettingAttribute cloudProviderSetting = context.getCloudProviderSetting();
    Validator.equalCheck(cloudProviderSetting.getValue().getType(), SettingVariableTypes.GCP.name());
    String clusterName = context.getClusterName();
    String replicationControllerName = context.getServiceName();
    Integer desiredCount = context.getDesiredCount();
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback(context, getName());
    executionLogCallback.setLogService(logService);

    executionLogCallback.saveExecutionLog(format("Begin execution of command: %s", getName()), INFO);
    CommandExecutionStatus commandExecutionStatus = FAILURE;

    try {
      KubernetesConfig kubernetesConfig = gkeClusterService.getCluster(cloudProviderSetting, clusterName);
      kubernetesContainerService.setControllerPodCount(kubernetesConfig, replicationControllerName, desiredCount);
      Multimap<String, String> podInfo =
          kubernetesContainerService.getPodInfo(kubernetesConfig, replicationControllerName, desiredCount);
      List<String> podNames = new ArrayList<>(podInfo.get("podNames"));
      List<String> containerIds = new ArrayList<>(podInfo.get("containerIds"));
      context.setCommandExecutionData(
          aResizeCommandUnitExecutionData().withHostNames(podNames).withContainerIds(containerIds).build());
      commandExecutionStatus = SUCCESS;
    } catch (Exception ex) {
      executionLogCallback.saveExecutionLog("Command execution failed", ERROR);
      throw new WingsException(ErrorCode.UNKNOWN_ERROR, "", ex);
    }
    return commandExecutionStatus;
  }
}
