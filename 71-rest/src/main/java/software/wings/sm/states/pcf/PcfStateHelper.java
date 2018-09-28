package software.wings.sm.states.pcf;

import static software.wings.beans.DelegateTask.Builder.aDelegateTask;

import com.google.inject.Singleton;

import software.wings.api.pcf.PcfRouteUpdateStateExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.DelegateTask;
import software.wings.beans.Environment;
import software.wings.beans.PcfConfig;
import software.wings.beans.PcfInfrastructureMapping;
import software.wings.beans.TaskType;
import software.wings.beans.command.CommandUnitDetails.CommandUnitType;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest.PcfCommandType;
import software.wings.helpers.ext.pcf.request.PcfCommandRouteUpdateRequest;
import software.wings.helpers.ext.pcf.request.PcfRouteUpdateRequestConfigData;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Singleton
public class PcfStateHelper {
  public PcfCommandRequest getPcfCommandRouteUpdateRequest(PcfCommandType pcfCommandType, String commandName,
      String appId, String accountId, String activityId, PcfConfig pcfConfig, String organization, String space,
      PcfRouteUpdateRequestConfigData requestConfigData, Integer timeoutIntervalInMin) {
    timeoutIntervalInMin = timeoutIntervalInMin == null ? Integer.valueOf(5) : timeoutIntervalInMin;
    return PcfCommandRouteUpdateRequest.builder()
        .pcfCommandType(pcfCommandType)
        .commandName(commandName)
        .appId(appId)
        .accountId(accountId)
        .activityId(activityId)
        .pcfConfig(pcfConfig)
        .organization(organization)
        .space(space)
        .pcfRouteUpdateConfigData(requestConfigData)
        .timeoutIntervalInMin(timeoutIntervalInMin)
        .build();
  }

  public DelegateTask getDelegateTask(String accountId, String appId, TaskType taskType, String waitId, String envId,
      String infrastructureMappingId, Object[] parameters, long timeout) {
    return aDelegateTask()
        .withAccountId(accountId)
        .withAppId(appId)
        .withTaskType(taskType)
        .withWaitId(waitId)
        .withParameters(parameters)
        .withEnvId(envId)
        .withTimeout(TimeUnit.MINUTES.toMillis(timeout))
        .withInfrastructureMappingId(infrastructureMappingId)
        .build();
  }

  public PcfRouteUpdateStateExecutionData getRouteUpdateStateExecutionData(String activityId, String appId,
      String accountId, PcfCommandRequest pcfCommandRequest, String commandName,
      PcfRouteUpdateRequestConfigData requestConfigData) {
    return PcfRouteUpdateStateExecutionData.builder()
        .activityId(activityId)
        .accountId(accountId)
        .appId(appId)
        .pcfCommandRequest(pcfCommandRequest)
        .commandName(commandName)
        .pcfRouteUpdateRequestConfigData(requestConfigData)
        .build();
  }

  public ActivityBuilder getActivityBuilder(String appName, String appId, String commandName, Type type,
      ExecutionContext executionContext, String commandType, CommandUnitType commandUnitType, Environment environment) {
    return Activity.builder()
        .applicationName(appName)
        .appId(appId)
        .commandName(commandName)
        .type(type)
        .workflowType(executionContext.getWorkflowType())
        .workflowExecutionName(executionContext.getWorkflowExecutionName())
        .stateExecutionInstanceId(executionContext.getStateExecutionInstanceId())
        .stateExecutionInstanceName(executionContext.getStateExecutionInstanceName())
        .commandType(commandType)
        .workflowExecutionId(executionContext.getWorkflowExecutionId())
        .workflowId(executionContext.getWorkflowId())
        .commandUnits(Collections.emptyList())
        .status(ExecutionStatus.RUNNING)
        .commandUnitType(commandUnitType)
        .environmentId(environment.getUuid())
        .environmentName(environment.getName())
        .environmentType(environment.getEnvironmentType());
  }

  public ExecutionResponse queueDelegateTaskForRouteUpdate(Application app, PcfConfig pcfConfig,
      DelegateService delegateService, PcfInfrastructureMapping pcfInfrastructureMapping, String activityId,
      String envId, Integer timeoutIntervalInMinutes, String commandName,
      PcfRouteUpdateRequestConfigData requestConfigData, List<EncryptedDataDetail> encryptedDataDetails) {
    PcfCommandRequest pcfCommandRequest = getPcfCommandRouteUpdateRequest(PcfCommandType.UPDATE_ROUTE, commandName,
        app.getUuid(), app.getAccountId(), activityId, pcfConfig, pcfInfrastructureMapping.getOrganization(),
        pcfInfrastructureMapping.getSpace(), requestConfigData, timeoutIntervalInMinutes);

    PcfRouteUpdateStateExecutionData stateExecutionData = getRouteUpdateStateExecutionData(
        activityId, app.getAccountId(), app.getUuid(), pcfCommandRequest, commandName, requestConfigData);

    DelegateTask delegateTask =
        getDelegateTask(app.getAccountId(), app.getUuid(), TaskType.PCF_COMMAND_TASK, activityId, envId,
            pcfInfrastructureMapping.getUuid(), new Object[] {pcfCommandRequest, encryptedDataDetails}, 10);

    delegateService.queueTask(delegateTask);

    return ExecutionResponse.Builder.anExecutionResponse()
        .withCorrelationIds(Arrays.asList(activityId))
        .withStateExecutionData(stateExecutionData)
        .withAsync(true)
        .build();
  }

  public Activity createActivity(ExecutionContext executionContext, String commandName, String stateType,
      CommandUnitType commandUnitType, ActivityService activityService) {
    Application app = ((ExecutionContextImpl) executionContext).getApp();
    Environment env = ((ExecutionContextImpl) executionContext).getEnv();

    ActivityBuilder activityBuilder = getActivityBuilder(
        app.getName(), app.getUuid(), commandName, Type.Command, executionContext, stateType, commandUnitType, env);
    return activityService.save(activityBuilder.build());
  }
}
