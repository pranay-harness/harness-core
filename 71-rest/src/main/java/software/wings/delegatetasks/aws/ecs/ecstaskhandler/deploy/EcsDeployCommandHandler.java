package software.wings.delegatetasks.aws.ecs.ecstaskhandler.deploy;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.ResizeStrategy.RESIZE_NEW_FIRST;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.ExceptionUtils;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import software.wings.api.ContainerServiceData;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.command.EcsResizeParams;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.command.ResizeCommandUnitExecutionData;
import software.wings.beans.command.ResizeCommandUnitExecutionData.ResizeCommandUnitExecutionDataBuilder;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.cloudprovider.aws.AwsClusterService;
import software.wings.delegatetasks.aws.ecs.ecstaskhandler.EcsCommandTaskHandler;
import software.wings.helpers.ext.ecs.request.EcsCommandRequest;
import software.wings.helpers.ext.ecs.request.EcsServiceDeployRequest;
import software.wings.helpers.ext.ecs.response.EcsCommandExecutionResponse;
import software.wings.helpers.ext.ecs.response.EcsServiceDeployResponse;
import software.wings.utils.Misc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Singleton
@Slf4j
public class EcsDeployCommandHandler extends EcsCommandTaskHandler {
  @Inject private EcsDeployCommandTaskHelper ecsDeployCommandTaskHelper;
  @Inject private AwsClusterService awsClusterService;
  static final String DASH_STRING = "----------";

  public EcsCommandExecutionResponse executeTaskInternal(EcsCommandRequest ecsCommandRequest,
      List<EncryptedDataDetail> encryptedDataDetails, ExecutionLogCallback executionLogCallback) {
    EcsCommandExecutionResponse executionResponse = EcsCommandExecutionResponse.builder().build();
    EcsServiceDeployResponse ecsServiceDeployResponse = ecsDeployCommandTaskHelper.getEmptyEcsServiceDeployResponse();
    executionResponse.setEcsCommandResponse(ecsServiceDeployResponse);

    ResizeCommandUnitExecutionDataBuilder executionDataBuilder = ResizeCommandUnitExecutionData.builder();

    try {
      EcsServiceDeployRequest request = null;
      if (ecsCommandRequest instanceof EcsServiceDeployRequest) {
        request = (EcsServiceDeployRequest) ecsCommandRequest;
      } else {
        ecsServiceDeployResponse.setCommandExecutionStatus(CommandExecutionStatus.FAILURE);
        ecsServiceDeployResponse.setOutput("Invalid request Type, expected EcsServiceDeployRequest");
        executionResponse.setErrorMessage("Invalid request Type, expected EcsServiceDeployRequest");
        return executionResponse;
      }

      EcsResizeParams resizeParams = request.getEcsResizeParams();

      final boolean deployingToHundredPercent = ecsDeployCommandTaskHelper.getDeployingToHundredPercent(resizeParams);
      ContextData contextData = ContextData.builder()
                                    .encryptedDataDetails(encryptedDataDetails)
                                    .deployingToHundredPercent(deployingToHundredPercent)
                                    .resizeParams(resizeParams)
                                    .awsConfig(request.getAwsConfig())
                                    .build();

      List<ContainerServiceData> newInstanceDataList;
      List<ContainerServiceData> oldInstanceDataList;

      if (!resizeParams.isRollback()) {
        newInstanceDataList = new ArrayList<>();
        ContainerServiceData newInstanceData = ecsDeployCommandTaskHelper.getNewInstanceData(contextData);
        newInstanceDataList.add(newInstanceData);
        oldInstanceDataList = ecsDeployCommandTaskHelper.getOldInstanceData(contextData, newInstanceData);
      } else {
        // Rollback
        Map<String, Integer> originalServiceCounts =
            ecsDeployCommandTaskHelper.listOfStringArrayToMap(resizeParams.getOriginalServiceCounts());

        if (Objects.equals(ecsDeployCommandTaskHelper.getActiveServiceCounts(contextData), originalServiceCounts)) {
          // Already rolled back
          executionLogCallback.saveExecutionLog("** Rollback already complete **\n");
          executionDataBuilder.newInstanceData(emptyList()).oldInstanceData(emptyList());
          return executionResponse;
        }

        newInstanceDataList = resizeParams.getNewInstanceData();
        oldInstanceDataList = resizeParams.getOldInstanceData();

        if (resizeParams.isRollbackAllPhases()) {
          // Roll back to original counts
          executionLogCallback.saveExecutionLog("** Rolling back all phases at once **\n");
          if (isNotEmpty(newInstanceDataList)) {
            ecsDeployCommandTaskHelper.setDesiredToOriginal(newInstanceDataList, originalServiceCounts);
          }

          if (isNotEmpty(oldInstanceDataList)) {
            ecsDeployCommandTaskHelper.setDesiredToOriginal(oldInstanceDataList, originalServiceCounts);
          }
        }
      }

      executionDataBuilder.newInstanceData(newInstanceDataList).oldInstanceData(oldInstanceDataList);

      boolean resizeNewFirst = resizeParams.getResizeStrategy() == RESIZE_NEW_FIRST;
      List<ContainerServiceData> firstDataList = resizeNewFirst ? newInstanceDataList : oldInstanceDataList;
      List<ContainerServiceData> secondDataList = resizeNewFirst ? oldInstanceDataList : newInstanceDataList;

      resizeInstances(contextData, firstDataList, executionDataBuilder, executionLogCallback, resizeNewFirst);
      resizeInstances(contextData, secondDataList, executionDataBuilder, executionLogCallback, !resizeNewFirst);
    } catch (Exception ex) {
      logger.error(ExceptionUtils.getMessage(ex), ex);
      Misc.logAllMessages(ex, executionLogCallback);
      logger.error("Completed operation with errors");
      executionLogCallback.saveExecutionLog(
          format("Completed operation with errors%n%s%n", DASH_STRING), LogLevel.ERROR);
      ecsServiceDeployResponse.setCommandExecutionStatus(CommandExecutionStatus.FAILURE);
      ecsServiceDeployResponse.setOutput(ExceptionUtils.getMessage(ex));
    } finally {
      copyExexcutionDataIntoResponse(executionResponse, executionDataBuilder);
    }

    return executionResponse;
  }

  private void copyExexcutionDataIntoResponse(
      EcsCommandExecutionResponse executionResponse, ResizeCommandUnitExecutionDataBuilder executionDataBuilder) {
    ResizeCommandUnitExecutionData executionData = executionDataBuilder.build();
    EcsServiceDeployResponse ecsServiceDeployResponse =
        (EcsServiceDeployResponse) executionResponse.getEcsCommandResponse();
    ecsServiceDeployResponse.setContainerInfos(executionData.getContainerInfos());
    ecsServiceDeployResponse.setNewInstanceData(executionData.getNewInstanceData());
    ecsServiceDeployResponse.setOldInstanceData(executionData.getOldInstanceData());
    ecsServiceDeployResponse.setPreviousContainerInfos(executionData.getPreviousContainerInfos());
    executionResponse.setCommandExecutionStatus(ecsServiceDeployResponse.getCommandExecutionStatus());
    executionResponse.setErrorMessage(ecsServiceDeployResponse.getOutput());
  }

  private void resizeInstances(ContextData contextData, List<ContainerServiceData> instanceData,
      ResizeCommandUnitExecutionDataBuilder executionDataBuilder, ExecutionLogCallback executionLogCallback,
      boolean isUpsize) {
    if (isNotEmpty(instanceData)) {
      List<ContainerInfo> containerInfos =
          instanceData.stream()
              .flatMap(data -> executeResize(contextData, data, executionLogCallback).stream())
              .collect(toList());
      if (isUpsize) {
        executionDataBuilder.containerInfos(
            containerInfos.stream().filter(ContainerInfo::isNewContainer).collect(toList()));
      } else {
        executionDataBuilder.previousContainerInfos(containerInfos);
      }
      ecsDeployCommandTaskHelper.logContainerInfos(containerInfos, executionLogCallback);
      logger.info("Successfully completed resize operation");
      executionLogCallback.saveExecutionLog(format("Completed operation%n%s%n", DASH_STRING));
    }
  }

  public List<ContainerInfo> executeResize(
      ContextData contextData, ContainerServiceData containerServiceData, ExecutionLogCallback executionLogCallback) {
    EcsResizeParams resizeParams = contextData.getResizeParams();

    // As a part of Rollback, restore AutoScalingConfig if required
    if (resizeParams.isRollback()) {
      ecsDeployCommandTaskHelper.restoreAutoScalarConfigs(contextData, containerServiceData, executionLogCallback);
    } else {
      ecsDeployCommandTaskHelper.deregisterAutoScalarsIfExists(contextData, executionLogCallback);
    }

    List<ContainerInfo> containerInfos = awsClusterService.resizeCluster(resizeParams.getRegion(),
        contextData.getSettingAttribute(), contextData.getEncryptedDataDetails(), resizeParams.getClusterName(),
        containerServiceData.getName(), containerServiceData.getPreviousCount(), containerServiceData.getDesiredCount(),
        resizeParams.getServiceSteadyStateTimeout(), executionLogCallback);

    ecsDeployCommandTaskHelper.createAutoScalarConfigIfServiceReachedMaxSize(
        contextData, containerServiceData, executionLogCallback);

    return containerInfos;
  }
}
