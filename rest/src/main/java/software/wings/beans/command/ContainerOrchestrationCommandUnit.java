package software.wings.beans.command;

import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static software.wings.beans.command.ResizeCommandUnitExecutionData.ResizeCommandUnitExecutionDataBuilder.aResizeCommandUnitExecutionData;

import com.google.inject.Inject;

import org.mongodb.morphia.annotations.Transient;
import software.wings.api.ContainerServiceData;
import software.wings.beans.ErrorCode;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.exception.WingsException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by peeyushaggarwal on 2/1/17.
 */
public abstract class ContainerOrchestrationCommandUnit extends AbstractCommandUnit {
  @Inject @Transient private transient DelegateLogService logService;

  /**
   * Instantiates a new command unit.
   *
   * @param commandUnitType the command unit type
   */
  public ContainerOrchestrationCommandUnit(CommandUnitType commandUnitType) {
    super(commandUnitType);
    setArtifactNeeded(true);
  }

  @Override
  public CommandExecutionStatus execute(CommandExecutionContext context) {
    SettingAttribute cloudProviderSetting = context.getCloudProviderSetting();
    String clusterName = context.getClusterName();
    List<ContainerServiceData> desiredCounts = context.getDesiredCounts();
    String region = context.getRegion();

    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback(context, getName());
    executionLogCallback.setLogService(logService);
    CommandExecutionStatus commandExecutionStatus = FAILURE;

    try {
      List<ContainerInfo> containerInfos = new ArrayList<>();
      desiredCounts.forEach(dc
          -> containerInfos.addAll(executeInternal(region, cloudProviderSetting, clusterName, dc.getName(),
              dc.getPreviousCount(), dc.getDesiredCount(), executionLogCallback)));
      context.setCommandExecutionData(aResizeCommandUnitExecutionData().withContainerInfos(containerInfos).build());
      boolean allContainersSuccess =
          containerInfos.stream().allMatch(info -> info.getStatus() == ContainerInfo.Status.SUCCESS);
      int totalDesiredCount = desiredCounts.stream().mapToInt(ContainerServiceData::getDesiredCount).sum();
      if (containerInfos.size() == totalDesiredCount && allContainersSuccess) {
        commandExecutionStatus = SUCCESS;
      }
    } catch (Exception ex) {
      throw new WingsException(ErrorCode.UNKNOWN_ERROR, "", ex);
    }
    return commandExecutionStatus;
  }

  protected abstract List<ContainerInfo> executeInternal(String region, SettingAttribute cloudProviderSetting,
      String clusterName, String serviceName, int previousCount, int desiredCount,
      ExecutionLogCallback executionLogCallback);
}
