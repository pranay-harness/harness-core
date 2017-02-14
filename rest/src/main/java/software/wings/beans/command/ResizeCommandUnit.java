package software.wings.beans.command;

import software.wings.beans.SettingAttribute;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.Validator;

import java.util.List;

/**
 * Created by peeyushaggarwal on 2/3/17.
 */
public class ResizeCommandUnit extends ContainerOrchestrationCommandUnit {
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
    List<String> containerInstanceArns =
        clusterService.resizeCluster(cloudProviderSetting, clusterName, serviceName, desiredCount);
    return ExecutionResult.SUCCESS;
  }
}
