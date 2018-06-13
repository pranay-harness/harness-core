package software.wings.beans.command;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.InstanceUnitType.PERCENTAGE;
import static software.wings.beans.ResizeStrategy.RESIZE_NEW_FIRST;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.ContainerServiceData;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.ResizeCommandUnitExecutionData.ResizeCommandUnitExecutionDataBuilder;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.exception.InvalidRequestException;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.utils.Misc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class ContainerResizeCommandUnit extends AbstractCommandUnit {
  private static final Logger logger = LoggerFactory.getLogger(ContainerResizeCommandUnit.class);

  static final String DASH_STRING = "----------";

  @Inject @Transient private transient DelegateLogService logService;

  public ContainerResizeCommandUnit(CommandUnitType commandUnitType) {
    super(commandUnitType);
    setArtifactNeeded(true);
  }

  @SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
  @Override
  public CommandExecutionStatus execute(CommandExecutionContext context) {
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback(
        logService, context.getAccountId(), context.getAppId(), context.getActivityId(), getName());
    ResizeCommandUnitExecutionDataBuilder executionDataBuilder = ResizeCommandUnitExecutionData.builder();
    CommandExecutionStatus status = CommandExecutionStatus.FAILURE;

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
        // Rollback
        Map<String, Integer> originalServiceCounts =
            listOfStringArrayToMap(contextData.resizeParams.getOriginalServiceCounts());
        Map<String, Integer> originalTrafficWeights =
            listOfStringArrayToMap(contextData.resizeParams.getOriginalTrafficWeights());

        if (Objects.equals(getActiveServiceCounts(contextData), originalServiceCounts)
            && Objects.equals(getTrafficWeights(contextData), originalTrafficWeights)) {
          // Already rolled back
          executionLogCallback.saveExecutionLog("** Rollback already complete **\n");
          executionDataBuilder.newInstanceData(emptyList()).oldInstanceData(emptyList());
          return CommandExecutionStatus.SUCCESS;
        }

        newInstanceDataList = contextData.resizeParams.getNewInstanceData();
        oldInstanceDataList = contextData.resizeParams.getOldInstanceData();

        if (contextData.resizeParams.isRollbackAllPhases()) {
          // Roll back to original counts
          executionLogCallback.saveExecutionLog("** Rolling back all phases at once **\n");
          setDesiredToOriginal(newInstanceDataList, originalServiceCounts, originalTrafficWeights);
          setDesiredToOriginal(oldInstanceDataList, originalServiceCounts, originalTrafficWeights);
        }
      }

      executionDataBuilder.newInstanceData(newInstanceDataList).oldInstanceData(oldInstanceDataList);

      boolean resizeNewFirst = contextData.resizeParams.getResizeStrategy() == RESIZE_NEW_FIRST;
      List<ContainerServiceData> firstDataList = resizeNewFirst ? newInstanceDataList : oldInstanceDataList;
      List<ContainerServiceData> secondDataList = resizeNewFirst ? oldInstanceDataList : newInstanceDataList;

      resizeInstances(contextData, firstDataList, executionDataBuilder, executionLogCallback, resizeNewFirst);
      resizeInstances(contextData, secondDataList, executionDataBuilder, executionLogCallback, !resizeNewFirst);

      List<ContainerServiceData> allData = new ArrayList<>();
      if (isNotEmpty(firstDataList)) {
        allData.addAll(firstDataList);
      }
      if (isNotEmpty(secondDataList)) {
        allData.addAll(secondDataList);
      }
      postExecution(contextData, allData, executionLogCallback);
      status = CommandExecutionStatus.SUCCESS;
    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
      Misc.logAllMessages(ex, executionLogCallback);
      logger.error("Completed operation with errors");
      executionLogCallback.saveExecutionLog(
          format("Completed operation with errors\n%s\n", DASH_STRING), LogLevel.ERROR);
      executionLogCallback.saveExecutionLog("Error: " + ex.getMessage());
    } finally {
      context.setCommandExecutionData(executionDataBuilder.build());
    }

    return status;
  }

  private static Map<String, Integer> listOfStringArrayToMap(List<String[]> listOfStringArray) {
    return Optional.ofNullable(listOfStringArray)
        .orElse(new ArrayList<>())
        .stream()
        .collect(Collectors.toMap(item -> item[0], item -> Integer.valueOf(item[1])));
  }

  private void setDesiredToOriginal(List<ContainerServiceData> newInstanceDataList,
      Map<String, Integer> originalServiceCounts, Map<String, Integer> originalTrafficWeights) {
    for (ContainerServiceData containerServiceData : newInstanceDataList) {
      containerServiceData.setDesiredCount(
          Optional.ofNullable(originalServiceCounts.get(containerServiceData.getName())).orElse(0));
      containerServiceData.setDesiredTraffic(
          Optional.ofNullable(originalTrafficWeights.get(containerServiceData.getName())).orElse(0));
    }
  }

  @SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
  private void resizeInstances(ContextData contextData, List<ContainerServiceData> instanceData,
      ResizeCommandUnitExecutionDataBuilder executionDataBuilder, ExecutionLogCallback executionLogCallback,
      boolean isUpsize) {
    if (isNotEmpty(instanceData)) {
      List<ContainerInfo> containerInfos =
          instanceData.stream()
              .flatMap(data -> executeResize(contextData, data, executionLogCallback).stream())
              .collect(toList());
      if (isUpsize) {
        executionDataBuilder.containerInfos(containerInfos);
      }
      logContainerInfos(containerInfos, executionLogCallback);
      logger.info("Successfully completed resize operation");
      executionLogCallback.saveExecutionLog(format("Completed operation\n%s\n", DASH_STRING));
    }
  }

  private void logContainerInfos(List<ContainerInfo> containerInfos, ExecutionLogCallback executionLogCallback) {
    try {
      if (isNotEmpty(containerInfos)) {
        executionLogCallback.saveExecutionLog("\nContainer IDs:");
        containerInfos.forEach(info
            -> executionLogCallback.saveExecutionLog("  " + info.getHostName()
                + (info.getHostName().equals(info.getContainerId()) ? "" : " - " + info.getContainerId())));
        executionLogCallback.saveExecutionLog("");
      }
    } catch (Exception e) {
      Misc.logAllMessages(e, executionLogCallback);
    }
  }

  private ContainerServiceData getNewInstanceData(ContextData contextData) {
    Optional<Integer> previousDesiredCount = getServiceDesiredCount(contextData);

    String containerServiceName = contextData.resizeParams.getContainerServiceName();
    if (!previousDesiredCount.isPresent()) {
      throw new InvalidRequestException("Service setup not done, service name: " + containerServiceName);
    }

    int previousCount = previousDesiredCount.get();
    int desiredCount = getNewInstancesDesiredCount(contextData);
    Integer desiredTrafficPercent = getDesiredTrafficPercent(contextData);
    if (desiredTrafficPercent == null) {
      Map<String, Integer> activeOtherControllers = getActiveServiceCounts(contextData);
      activeOtherControllers.remove(containerServiceName);
      int totalOtherInstances = activeOtherControllers.values().stream().mapToInt(Integer::intValue).sum();
      int downsizeCount = getDownsizeByAmount(contextData, totalOtherInstances, desiredCount, previousCount);
      int totalInstances = totalOtherInstances - downsizeCount + desiredCount;
      desiredTrafficPercent = (int) Math.round((desiredCount * 100.0) / totalInstances);
    }

    if (desiredCount < previousCount) {
      String msg = "Desired instance count must be greater than or equal to the current instance count: {current: "
          + previousCount + ", desired: " + desiredCount + "}";
      logger.error(msg);
      throw new InvalidRequestException(msg);
    }

    return ContainerServiceData.builder()
        .name(containerServiceName)
        .image(contextData.resizeParams.getImage())
        .previousCount(previousCount)
        .desiredCount(desiredCount)
        .previousTraffic(getPreviousTrafficPercent(contextData))
        .desiredTraffic(desiredTrafficPercent)
        .build();
  }

  private int getNewInstancesDesiredCount(ContextData contextData) {
    Preconditions.checkNotNull(contextData.resizeParams.getInstanceCount());
    int instanceCount = contextData.resizeParams.getInstanceCount();
    int totalTargetInstances = contextData.resizeParams.isUseFixedInstances()
        ? contextData.resizeParams.getFixedInstances()
        : contextData.resizeParams.getMaxInstances();
    return contextData.resizeParams.getInstanceUnitType() == PERCENTAGE
        ? (int) Math.round(Math.min(instanceCount, 100) * totalTargetInstances / 100.0)
        : Math.min(instanceCount, totalTargetInstances);
  }

  private int getDownsizeByAmount(
      ContextData contextData, int totalOtherInstances, int upsizeDesiredCount, int upsizePreviousCount) {
    Integer downsizeDesiredCount = contextData.resizeParams.getDownsizeInstanceCount();
    if (downsizeDesiredCount != null) {
      int downsizeInstanceCount = contextData.resizeParams.getDownsizeInstanceCount();
      int totalTargetInstances = contextData.resizeParams.isUseFixedInstances()
          ? contextData.resizeParams.getFixedInstances()
          : contextData.resizeParams.getMaxInstances();
      int downsizeToCount = contextData.resizeParams.getDownsizeInstanceUnitType() == PERCENTAGE
          ? (int) Math.round(Math.min(downsizeInstanceCount, 100) * totalTargetInstances / 100.0)
          : Math.min(downsizeInstanceCount, totalTargetInstances);
      return Math.max(totalOtherInstances - downsizeToCount, 0);
    } else {
      return contextData.deployingToHundredPercent ? totalOtherInstances
                                                   : Math.max(upsizeDesiredCount - upsizePreviousCount, 0);
    }
  }

  @SuppressFBWarnings("WMI_WRONG_MAP_ITERATOR")
  private List<ContainerServiceData> getOldInstanceData(ContextData contextData, ContainerServiceData newServiceData) {
    List<ContainerServiceData> oldInstanceData = new ArrayList<>();
    Map<String, Integer> previousCounts = getActiveServiceCounts(contextData);
    previousCounts.remove(newServiceData.getName());
    Map<String, String> previousImages = getActiveServiceImages(contextData);
    previousImages.remove(newServiceData.getName());
    Map<String, Integer> previousTrafficWeights = getTrafficWeights(contextData);
    previousTrafficWeights.remove(newServiceData.getName());

    int downsizeCount =
        getDownsizeByAmount(contextData, previousCounts.values().stream().mapToInt(Integer::intValue).sum(),
            newServiceData.getDesiredCount(), newServiceData.getPreviousCount());
    int runningOldInstances = previousCounts.values().stream().mapToInt(Integer::intValue).sum();
    int targetOldInstances = runningOldInstances - downsizeCount;
    double oldInstanceTrafficShare = 100.0 - newServiceData.getDesiredTraffic();

    for (String serviceName : previousCounts.keySet()) {
      String previousImage = previousImages.get(serviceName);
      int previousCount = previousCounts.get(serviceName);
      int desiredCount = Math.max(previousCount - downsizeCount, 0);
      int previousTraffic = Optional.ofNullable(previousTrafficWeights.get(serviceName)).orElse(0);
      int desiredTraffic = (int) Math.round((desiredCount * oldInstanceTrafficShare) / targetOldInstances);

      oldInstanceData.add(ContainerServiceData.builder()
                              .name(serviceName)
                              .image(previousImage)
                              .previousCount(previousCount)
                              .desiredCount(desiredCount)
                              .previousTraffic(previousTraffic)
                              .desiredTraffic(desiredTraffic)
                              .build());
      downsizeCount -= previousCount - desiredCount;
    }
    return oldInstanceData;
  }

  protected void postExecution(
      ContextData contextData, List<ContainerServiceData> allData, ExecutionLogCallback executionLogCallback) {}

  protected abstract Map<String, Integer> getActiveServiceCounts(ContextData contextData);

  protected abstract Map<String, String> getActiveServiceImages(ContextData contextData);

  protected abstract Map<String, Integer> getTrafficWeights(ContextData contextData);

  protected abstract Optional<Integer> getServiceDesiredCount(ContextData contextData);

  protected abstract int getPreviousTrafficPercent(ContextData contextData);

  protected abstract Integer getDesiredTrafficPercent(ContextData contextData);

  protected abstract List<ContainerInfo> executeResize(
      ContextData contextData, ContainerServiceData containerServiceData, ExecutionLogCallback executionLogCallback);

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

  static class ContextData {
    final SettingAttribute settingAttribute;
    final List<EncryptedDataDetail> encryptedDataDetails;
    final ContainerResizeParams resizeParams;
    final boolean deployingToHundredPercent;

    ContextData(CommandExecutionContext context) {
      settingAttribute = context.getCloudProviderSetting();
      encryptedDataDetails = context.getCloudProviderCredentials();
      resizeParams = context.getContainerResizeParams();

      if (!resizeParams.isRollback()) {
        Preconditions.checkNotNull(resizeParams.getInstanceCount());
        deployingToHundredPercent = resizeParams.getInstanceUnitType() == PERCENTAGE
            ? resizeParams.getInstanceCount() >= 100
            : (resizeParams.isUseFixedInstances()
                  && resizeParams.getInstanceCount() >= resizeParams.getFixedInstances())
                || (!resizeParams.isUseFixedInstances()
                       && resizeParams.getInstanceCount() >= resizeParams.getMaxInstances());
      } else {
        deployingToHundredPercent = false;
      }
    }
  }
}
