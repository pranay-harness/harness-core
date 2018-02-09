package software.wings.beans.command;

import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;

import com.google.inject.Inject;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.ContainerServiceData;
import software.wings.beans.ErrorCode;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.exception.WingsException;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.utils.Misc;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by peeyushaggarwal on 2/1/17.
 */
public abstract class ContainerResizeCommandUnit extends AbstractCommandUnit {
  private static final Logger logger = LoggerFactory.getLogger(ContainerResizeCommandUnit.class);

  @Inject @Transient private transient DelegateLogService logService;

  /**
   * Instantiates a new command unit.
   *
   * @param commandUnitType the command unit type
   */
  public ContainerResizeCommandUnit(CommandUnitType commandUnitType) {
    super(commandUnitType);
    setArtifactNeeded(true);
  }

  @Override
  public CommandExecutionStatus execute(CommandExecutionContext context) {
    SettingAttribute cloudProviderSetting = context.getCloudProviderSetting();
    List<EncryptedDataDetail> cloudProviderCredentials = context.getCloudProviderCredentials();
    ContainerResizeParams params = context.getContainerResizeParams();
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback(context, getName());
    executionLogCallback.setLogService(logService);
    CommandExecutionStatus commandExecutionStatus = FAILURE;

    try {
      List<ContainerInfo> containerInfos = new ArrayList<>();
      params.getDesiredCounts().forEach(containerServiceData
          -> containerInfos.addAll(executeInternal(
              cloudProviderSetting, cloudProviderCredentials, params, containerServiceData, executionLogCallback)));
      context.setCommandExecutionData(ResizeCommandUnitExecutionData.builder().containerInfos(containerInfos).build());
      boolean allContainersSuccess =
          containerInfos.stream().allMatch(info -> info.getStatus() == ContainerInfo.Status.SUCCESS);
      int totalDesiredCount = params.getDesiredCounts().stream().mapToInt(ContainerServiceData::getDesiredCount).sum();
      if (containerInfos.size() == totalDesiredCount && allContainersSuccess) {
        commandExecutionStatus = SUCCESS;
      } else {
        if (containerInfos.size() != totalDesiredCount) {
          executionLogCallback.saveExecutionLog(
              String.format("Expected data for %d container%s but got %d", totalDesiredCount,
                  totalDesiredCount == 1 ? "" : "s", containerInfos.size()),
              LogLevel.ERROR);
        }
        if (!allContainersSuccess) {
          List<ContainerInfo> failed = containerInfos.stream()
                                           .filter(info -> info.getStatus() != ContainerInfo.Status.SUCCESS)
                                           .collect(Collectors.toList());
          executionLogCallback.saveExecutionLog(
              String.format("The following container%s did not have success status: %s", failed.size() == 1 ? "" : "s",
                  failed.stream().map(ContainerInfo::getContainerId).collect(Collectors.toList())),
              LogLevel.ERROR);
        }
      }
    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
      Misc.logAllMessages(ex, executionLogCallback);
      if (ex instanceof WingsException) {
        throw ex;
      }
      throw new WingsException(ErrorCode.UNKNOWN_ERROR, ex.getMessage(), ex);
    }
    return commandExecutionStatus;
  }

  protected abstract List<ContainerInfo> executeInternal(SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, ContainerResizeParams params, ContainerServiceData serviceData,
      ExecutionLogCallback executionLogCallback);

  @Data
  @EqualsAndHashCode(callSuper = true)
  public abstract static class Yaml extends AbstractCommandUnit.Yaml {
    public Yaml(String commandUnitType) {
      super(commandUnitType);
    }

    public Yaml(String name, String commandUnitType, String deploymentType) {
      super(name, commandUnitType, deploymentType);
    }
  }
}
