package software.wings.beans.command;

import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;

import com.google.inject.Inject;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.ContainerServiceData;
import software.wings.beans.ErrorCode;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.exception.WingsException;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by peeyushaggarwal on 2/1/17.
 */
public abstract class ContainerResizeCommandUnit extends AbstractCommandUnit {
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
    String clusterName = context.getClusterName();
    String namespace = context.getNamespace();
    List<ContainerServiceData> desiredCounts = context.getDesiredCounts();
    String region = context.getRegion();
    int serviceSteadyStateTimeout = context.getEcsServiceSteadyStateTimeout();

    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback(context, getName());
    executionLogCallback.setLogService(logService);
    CommandExecutionStatus commandExecutionStatus = FAILURE;

    try {
      List<ContainerInfo> containerInfos = new ArrayList<>();
      desiredCounts.forEach(dc
          -> containerInfos.addAll(executeInternal(region, cloudProviderSetting, cloudProviderCredentials, clusterName,
              namespace, dc.getName(), dc.getPreviousCount(), dc.getDesiredCount(), serviceSteadyStateTimeout,
              executionLogCallback)));
      context.setCommandExecutionData(ResizeCommandUnitExecutionData.builder().containerInfos(containerInfos).build());
      boolean allContainersSuccess =
          containerInfos.stream().allMatch(info -> info.getStatus() == ContainerInfo.Status.SUCCESS);
      int totalDesiredCount = desiredCounts.stream().mapToInt(ContainerServiceData::getDesiredCount).sum();
      if (containerInfos.size() == totalDesiredCount && allContainersSuccess) {
        commandExecutionStatus = SUCCESS;
      }
    } catch (Exception ex) {
      executionLogCallback.saveExecutionLog(ex.getMessage(), LogLevel.ERROR);
      throw new WingsException(ErrorCode.UNKNOWN_ERROR, ex.getMessage(), ex);
    }
    return commandExecutionStatus;
  }

  protected abstract List<ContainerInfo> executeInternal(String region, SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, String clusterName, String namespace, String serviceName,
      int previousCount, int desiredCount, int serviceSteadyStateTimeout, ExecutionLogCallback executionLogCallback);

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static abstract class Yaml extends AbstractCommandUnit.Yaml {
    public static abstract class Builder extends AbstractCommandUnit.Yaml.Builder {
      protected Builder() {}
    }
  }
}
