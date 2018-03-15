package software.wings.beans.command;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.InstanceUnitType.PERCENTAGE;
import static software.wings.beans.ResizeStrategy.RESIZE_NEW_FIRST;

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
import software.wings.beans.command.ResizeCommandUnitExecutionData.ResizeCommandUnitExecutionDataBuilder;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.exception.WingsException;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.utils.Misc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class ContainerResizeCommandUnit extends AbstractCommandUnit {
  private static final Logger logger = LoggerFactory.getLogger(ContainerResizeCommandUnit.class);

  private static final String DASH_STRING = "----------";

  @Inject @Transient private transient DelegateLogService logService;

  public ContainerResizeCommandUnit(CommandUnitType commandUnitType) {
    super(commandUnitType);
    setArtifactNeeded(true);
  }

  @Override
  public CommandExecutionStatus execute(CommandExecutionContext context) {
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback(context, getName());
    executionLogCallback.setLogService(logService);
    ResizeCommandUnitExecutionDataBuilder executionDataBuilder = ResizeCommandUnitExecutionData.builder();

    try {
      ContextData contextData = new ContextData(context);

      List<ContainerServiceData> newInstanceDataList;
      List<ContainerServiceData> oldInstanceDataList;
      if (!contextData.resizeParams.isRollback()) {
        newInstanceDataList = new ArrayList<>();
        ContainerServiceData newInstanceData = getNewInstanceData(contextData);
        newInstanceDataList.add(newInstanceData);
        oldInstanceDataList = getOldInstanceData(contextData, newInstanceData);
      } else {
        newInstanceDataList = contextData.resizeParams.getNewInstanceData();
        oldInstanceDataList = contextData.resizeParams.getOldInstanceData();
      }

      executionDataBuilder.newInstanceData(newInstanceDataList).oldInstanceData(oldInstanceDataList);

      boolean resizeNewFirst = contextData.resizeParams.getResizeStrategy() == RESIZE_NEW_FIRST;
      List<ContainerServiceData> firstDataList = resizeNewFirst ? newInstanceDataList : oldInstanceDataList;
      List<ContainerServiceData> secondDataList = resizeNewFirst ? oldInstanceDataList : newInstanceDataList;

      boolean executionSucceeded =
          resizeInstances(contextData, firstDataList, executionDataBuilder, executionLogCallback, resizeNewFirst)
          && resizeInstances(contextData, secondDataList, executionDataBuilder, executionLogCallback, !resizeNewFirst);

      return executionSucceeded ? CommandExecutionStatus.SUCCESS : CommandExecutionStatus.FAILURE;
    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
      Misc.logAllMessages(ex, executionLogCallback);
      return CommandExecutionStatus.FAILURE;
    } finally {
      context.setCommandExecutionData(executionDataBuilder.build());
    }
  }

  private boolean resizeInstances(ContextData contextData, List<ContainerServiceData> instanceData,
      ResizeCommandUnitExecutionDataBuilder executionDataBuilder, ExecutionLogCallback executionLogCallback,
      boolean isUpsize) {
    if (isNotEmpty(instanceData)) {
      int totalDesiredCount = instanceData.stream().mapToInt(ContainerServiceData::getDesiredCount).sum();
      List<ContainerInfo> containerInfos =
          instanceData.stream()
              .flatMap(data -> executeResize(contextData, totalDesiredCount, data, executionLogCallback).stream())
              .collect(toList());
      if (isUpsize) {
        executionDataBuilder.containerInfos(containerInfos);
      }
      return allContainersSuccess(containerInfos, totalDesiredCount, executionLogCallback);
    } else {
      return true;
    }
  }

  private boolean allContainersSuccess(
      List<ContainerInfo> containerInfos, int totalDesiredCount, ExecutionLogCallback executionLogCallback) {
    boolean success = false;
    boolean allContainersSuccess =
        containerInfos.stream().allMatch(info -> info.getStatus() == ContainerInfo.Status.SUCCESS);
    if (containerInfos.size() == totalDesiredCount && allContainersSuccess) {
      success = true;
      logger.info("Successfully completed resize operation");
      executionLogCallback.saveExecutionLog(
          String.format("Completed resize operation\n%s\n", DASH_STRING), LogLevel.INFO);
    } else {
      if (containerInfos.size() != totalDesiredCount) {
        executionLogCallback.saveExecutionLog(
            String.format("Expected data for %d container%s but got %d", totalDesiredCount,
                totalDesiredCount == 1 ? "" : "s", containerInfos.size()),
            LogLevel.ERROR);
      }
      if (!allContainersSuccess) {
        List<ContainerInfo> failed =
            containerInfos.stream().filter(info -> info.getStatus() != ContainerInfo.Status.SUCCESS).collect(toList());
        executionLogCallback.saveExecutionLog(
            String.format("The following container%s did not have success status: %s", failed.size() == 1 ? "" : "s",
                failed.stream().map(ContainerInfo::getContainerId).collect(toList())),
            LogLevel.ERROR);
      }
      logger.error("Completed operation with errors");
      executionLogCallback.saveExecutionLog(
          String.format("Completed operation with errors\n%s\n", DASH_STRING), LogLevel.ERROR);
    }
    return success;
  }

  private ContainerServiceData getNewInstanceData(ContextData contextData) {
    Optional<Integer> previousDesiredCount = getServiceDesiredCount(contextData);

    if (!previousDesiredCount.isPresent()) {
      throw new WingsException(ErrorCode.INVALID_REQUEST)
          .addParam(
              "message", "Service setup not done, service name: " + contextData.resizeParams.getContainerServiceName());
    }

    int previousCount = previousDesiredCount.get();
    int desiredCount = getNewInstancesDesiredCount(contextData);

    if (desiredCount < previousCount) {
      String msg = "Desired instance count must be greater than or equal to the current instance count: {current: "
          + previousCount + ", desired: " + desiredCount + "}";
      logger.error(msg);
      throw new WingsException(ErrorCode.INVALID_REQUEST).addParam("message", msg);
    }

    return ContainerServiceData.builder()
        .name(contextData.resizeParams.getContainerServiceName())
        .previousCount(previousCount)
        .desiredCount(desiredCount)
        .build();
  }

  private int getNewInstancesDesiredCount(ContextData contextData) {
    int instanceCount = contextData.resizeParams.getInstanceCount();
    if (contextData.resizeParams.getInstanceUnitType() == PERCENTAGE) {
      int totalInstancesAvailable;
      if (contextData.resizeParams.isUseFixedInstances()) {
        totalInstancesAvailable = contextData.resizeParams.getFixedInstances();
      } else {
        totalInstancesAvailable =
            getActiveServiceCounts(contextData).values().stream().mapToInt(Integer::intValue).sum();
        if (totalInstancesAvailable == 0) {
          return contextData.resizeParams.getMaxInstances();
        }
      }
      return (int) Math.round(Math.min(instanceCount, 100) * totalInstancesAvailable / 100.0);
    } else {
      if (contextData.resizeParams.isUseFixedInstances()) {
        return Math.min(instanceCount, contextData.resizeParams.getFixedInstances());
      } else {
        return instanceCount;
      }
    }
  }

  private List<ContainerServiceData> getOldInstanceData(ContextData contextData, ContainerServiceData newServiceData) {
    List<ContainerServiceData> desiredCounts = new ArrayList<>();
    Map<String, Integer> previousCounts = getActiveServiceCounts(contextData);
    previousCounts.remove(newServiceData.getName());

    int downsizeCount = contextData.deployingToHundredPercent
        ? previousCounts.values().stream().mapToInt(Integer::intValue).sum()
        : Math.max(newServiceData.getDesiredCount() - newServiceData.getPreviousCount(), 0);

    for (String serviceName : previousCounts.keySet()) {
      int previousCount = previousCounts.get(serviceName);
      int desiredCount = Math.max(previousCount - downsizeCount, 0);
      if (previousCount != desiredCount) {
        desiredCounts.add(ContainerServiceData.builder()
                              .name(serviceName)
                              .previousCount(previousCount)
                              .desiredCount(desiredCount)
                              .build());
      }
      downsizeCount -= previousCount - desiredCount;
    }
    return desiredCounts;
  }

  protected abstract Map<String, Integer> getActiveServiceCounts(ContextData contextData);

  protected abstract Optional<Integer> getServiceDesiredCount(ContextData contextData);

  protected abstract List<ContainerInfo> executeResize(ContextData contextData, int totalDesiredCount,
      ContainerServiceData containerServiceData, ExecutionLogCallback executionLogCallback);

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

  protected static class ContextData {
    final SettingAttribute settingAttribute;
    final List<EncryptedDataDetail> encryptedDataDetails;
    final ContainerResizeParams resizeParams;
    final boolean deployingToHundredPercent;

    ContextData(CommandExecutionContext context) {
      settingAttribute = context.getCloudProviderSetting();
      encryptedDataDetails = context.getCloudProviderCredentials();
      resizeParams = context.getContainerResizeParams();

      deployingToHundredPercent = resizeParams.getInstanceUnitType() == PERCENTAGE
          ? resizeParams.getInstanceCount() >= 100
          : resizeParams.isUseFixedInstances() && resizeParams.getInstanceCount() >= resizeParams.getFixedInstances();
    }
  }
}
