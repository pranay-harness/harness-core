package software.wings.beans.command;

import software.wings.beans.SettingAttribute;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.Validator;

import java.util.List;
import javax.inject.Inject;

/**
 * Created by peeyushaggarwal on 2/3/17.
 */
public class ResizeCommandUnit extends ContainerOrchestrationCommandUnit {
  @Inject private transient DelegateLogService logService;

  public ResizeCommandUnit() {
    super(CommandUnitType.RESIZE);
  }

  @Override
  public ExecutionResult execute(CommandExecutionContext context) {
    SettingAttribute cloudProviderSetting = context.getCloudProviderSetting();
    Validator.equalCheck(cloudProviderSetting.getValue().getType(), SettingVariableTypes.AWS.name());
    String clusterName = context.getClusterName();
    String serviceName = context.getServiceName();
    Integer desiredCount = context.getDesiredCount();
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback(context, getName());
    executionLogCallback.setLogService(logService);
    List<String> containerInstanceArns = ecsClusterService.resizeCluster(
        cloudProviderSetting, clusterName, serviceName, desiredCount, executionLogCallback);
    return ExecutionResult.SUCCESS;
  }
}
