package software.wings.sm.states;

import static software.wings.api.CommandStateExecutionData.Builder.aCommandStateExecutionData;
import static software.wings.api.HostElement.Builder.aHostElement;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.api.InstanceElementListParam.InstanceElementListParamBuilder.anInstanceElementListParam;
import static software.wings.api.ServiceTemplateElement.Builder.aServiceTemplateElement;
import static software.wings.beans.Activity.Builder.anActivity;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.command.CommandExecutionContext.Builder.aCommandExecutionContext;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.InstanceStatusSummary.InstanceStatusSummaryBuilder.anInstanceStatusSummary;

import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.CommandStateExecutionData;
import software.wings.api.InstanceElement;
import software.wings.api.InstanceElementListParam;
import software.wings.api.PhaseElement;
import software.wings.beans.Activity;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.beans.command.CommandExecutionData;
import software.wings.beans.command.CommandExecutionResult;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.ResizeCommandUnitExecutionData;
import software.wings.common.Constants;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.State;
import software.wings.sm.WorkflowStandardParams;
import software.wings.waitnotify.NotifyResponseData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by brett on 4/7/17
 */
public abstract class CloudServiceDeploy extends State {
  @Attributes(title = "Number of instances") protected int instanceCount;

  @Inject @Transient protected transient SettingsService settingsService;

  @Inject @Transient protected transient DelegateService delegateService;

  @Inject @Transient protected transient ServiceResourceService serviceResourceService;

  @Inject @Transient protected transient ActivityService activityService;

  @Inject @Transient protected transient InfrastructureMappingService infrastructureMappingService;

  @Inject @Transient protected transient ServiceTemplateService serviceTemplateService;

  public CloudServiceDeploy(String name, String type) {
    super(name, type);
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    CommandStateExecutionData.Builder executionDataBuilder = aCommandStateExecutionData();

    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String serviceId = phaseElement.getServiceElement().getUuid();

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    Application app = workflowStandardParams.getApp();
    Environment env = workflowStandardParams.getEnv();

    Service service = serviceResourceService.get(app.getUuid(), serviceId);
    Command command =
        serviceResourceService.getCommandByName(app.getUuid(), serviceId, env.getUuid(), getCommandName()).getCommand();

    InfrastructureMapping infrastructureMapping =
        infrastructureMappingService.get(app.getUuid(), phaseElement.getInfraMappingId());
    SettingAttribute settingAttribute = settingsService.get(infrastructureMapping.getComputeProviderSettingId());

    executionDataBuilder.withServiceId(service.getUuid())
        .withServiceName(service.getName())
        .withAppId(app.getUuid())
        .withCommandName(getCommandName());

    Activity.Builder activityBuilder = anActivity()
                                           .withAppId(app.getUuid())
                                           .withApplicationName(app.getName())
                                           .withEnvironmentId(env.getUuid())
                                           .withEnvironmentName(env.getName())
                                           .withEnvironmentType(env.getEnvironmentType())
                                           .withServiceId(service.getUuid())
                                           .withServiceName(service.getName())
                                           .withCommandName(command.getName())
                                           .withType(Type.Command)
                                           .withWorkflowExecutionId(context.getWorkflowExecutionId())
                                           .withWorkflowType(context.getWorkflowType())
                                           .withWorkflowExecutionName(context.getWorkflowExecutionName())
                                           .withStateExecutionInstanceId(context.getStateExecutionInstanceId())
                                           .withStateExecutionInstanceName(context.getStateExecutionInstanceName())
                                           .withCommandType(command.getCommandUnitType().name())
                                           .withServiceVariables(context.getServiceVariables());

    Activity activity = activityService.save(activityBuilder.build());

    CommandExecutionContext commandExecutionContext =
        buildCommandExecutionContext(app, env.getUuid(), getClusterName(context), getServiceName(context),
            getServiceDesiredCount(context, settingAttribute), activity.getUuid(), settingAttribute);
    executionDataBuilder.withActivityId(activity.getUuid()).withNewContainerServiceName(getServiceName(context));

    delegateService.queueTask(aDelegateTask()
                                  .withAccountId(app.getAccountId())
                                  .withAppId(app.getAppId())
                                  .withTaskType(TaskType.COMMAND)
                                  .withWaitId(activity.getUuid())
                                  .withParameters(new Object[] {command, commandExecutionContext})
                                  .build());

    return anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(Collections.singletonList(activity.getUuid()))
        .withStateExecutionData(executionDataBuilder.build())
        .build();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    CommandStateExecutionData commandStateExecutionData = (CommandStateExecutionData) context.getStateExecutionData();

    CommandExecutionResult commandExecutionResult = ((CommandExecutionResult) response.values().iterator().next());
    if (commandExecutionResult == null || commandExecutionResult.getStatus() != CommandExecutionStatus.SUCCESS) {
      return buildEndStateExecution(commandStateExecutionData, ExecutionStatus.FAILED);
    }

    if (commandStateExecutionData.getOldContainerServiceName() == null) {
      commandStateExecutionData.setInstanceStatusSummaries(buildInstanceStatusSummaries(context, response));

      WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
      PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
      InfrastructureMapping infrastructureMapping =
          infrastructureMappingService.get(workflowStandardParams.getAppId(), phaseElement.getInfraMappingId());
      SettingAttribute settingAttribute = settingsService.get(infrastructureMapping.getComputeProviderSettingId());

      commandStateExecutionData.setOldContainerServiceName(getOldServiceName(context));

      int desiredCount = getOldServiceDesiredCount(context, settingAttribute);

      if (desiredCount < 0) {
        // Old service does not exist
        return buildEndStateExecution(commandStateExecutionData, ExecutionStatus.SUCCESS);
      }

      Application app = workflowStandardParams.getApp();
      Environment env = workflowStandardParams.getEnv();

      Command command = serviceResourceService
                            .getCommandByName(workflowStandardParams.getAppId(),
                                phaseElement.getServiceElement().getUuid(), env.getUuid(), getCommandName())
                            .getCommand();

      CommandExecutionContext commandExecutionContext =
          buildCommandExecutionContext(app, env.getUuid(), getClusterName(context), getOldServiceName(context),
              desiredCount, commandStateExecutionData.getActivityId(), settingAttribute);

      delegateService.queueTask(aDelegateTask()
                                    .withAccountId(app.getAccountId())
                                    .withAppId(app.getAppId())
                                    .withTaskType(TaskType.COMMAND)
                                    .withWaitId(commandStateExecutionData.getActivityId())
                                    .withParameters(new Object[] {command, commandExecutionContext})
                                    .build());

      return anExecutionResponse()
          .withAsync(true)
          .withCorrelationIds(Collections.singletonList(commandStateExecutionData.getActivityId()))
          .withStateExecutionData(commandStateExecutionData)
          .build();

    } else {
      return buildEndStateExecution(commandStateExecutionData, ExecutionStatus.SUCCESS);
    }
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  @Override
  public Map<String, String> validateFields() {
    Map<String, String> invalidFields = new HashMap<>();
    if (instanceCount == 0) {
      invalidFields.put("instanceCount", "instanceCount needs to be greater than 0");
    }
    return invalidFields;
  }

  private ExecutionResponse buildEndStateExecution(
      CommandStateExecutionData commandStateExecutionData, ExecutionStatus status) {
    activityService.updateStatus(
        commandStateExecutionData.getActivityId(), commandStateExecutionData.getAppId(), status);
    List<InstanceElement> instanceElements = commandStateExecutionData.getInstanceStatusSummaries()
                                                 .stream()
                                                 .map(InstanceStatusSummary::getInstanceElement)
                                                 .collect(Collectors.toList());

    InstanceElementListParam instanceElementListParam =
        anInstanceElementListParam()
            .withInstanceElements(instanceElements == null ? new ArrayList<>() : instanceElements)
            .build();
    return anExecutionResponse()
        .withStateExecutionData(commandStateExecutionData)
        .withExecutionStatus(status)
        .addNotifyElement(instanceElementListParam)
        .build();
  }

  private List<InstanceStatusSummary> buildInstanceStatusSummaries(
      ExecutionContext context, Map<String, NotifyResponseData> response) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String serviceId = phaseElement.getServiceElement().getUuid();
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    Application app = workflowStandardParams.getApp();
    Environment env = workflowStandardParams.getEnv();
    Key<ServiceTemplate> serviceTemplateKey =
        serviceTemplateService.getTemplateRefKeysByService(app.getUuid(), serviceId, env.getUuid()).get(0);

    CommandExecutionData commandExecutionData =
        ((CommandExecutionResult) response.values().iterator().next()).getCommandExecutionData();
    List<InstanceStatusSummary> instanceStatusSummaries = new ArrayList<>();

    if (commandExecutionData instanceof ResizeCommandUnitExecutionData
        && ((ResizeCommandUnitExecutionData) commandExecutionData).getContainerInfos() != null) {
      ((ResizeCommandUnitExecutionData) commandExecutionData)
          .getContainerInfos()
          .forEach(containerInfo
              -> instanceStatusSummaries.add(
                  anInstanceStatusSummary()
                      .withStatus(ExecutionStatus.SUCCESS)
                      .withInstanceElement(
                          anInstanceElement()
                              .withUuid(containerInfo.getContainerId())
                              .withHostName(containerInfo.getHostName())
                              .withHostElement(aHostElement().withHostName(containerInfo.getHostName()).build())
                              .withServiceTemplateElement(aServiceTemplateElement()
                                                              .withUuid(serviceTemplateKey.getId().toString())
                                                              .withServiceElement(phaseElement.getServiceElement())
                                                              .build())
                              .withDisplayName(containerInfo.getContainerId())
                              .build())
                      .build()));
    }
    return instanceStatusSummaries;
  }

  private CommandExecutionContext buildCommandExecutionContext(Application app, String envId, String clusterName,
      String serviceName, int desiredCount, String activityId, SettingAttribute settingAttribute) {
    CommandExecutionContext commandExecutionContext =
        aCommandExecutionContext().withAccountId(app.getAccountId()).withAppId(app.getUuid()).withEnvId(envId).build();
    commandExecutionContext.setClusterName(clusterName);

    commandExecutionContext.setServiceName(serviceName);
    commandExecutionContext.setActivityId(activityId);
    commandExecutionContext.setCloudProviderSetting(settingAttribute);
    commandExecutionContext.setDesiredCount(desiredCount);

    return commandExecutionContext;
  }

  public int getInstanceCount() {
    return instanceCount;
  }

  public void setInstanceCount(int instanceCount) {
    this.instanceCount = instanceCount;
  }

  public abstract String getCommandName();
  protected abstract String getClusterName(ExecutionContext context);
  protected abstract String getServiceName(ExecutionContext context);
  protected abstract String getOldServiceName(ExecutionContext context);
  protected abstract int getServiceDesiredCount(ExecutionContext context, SettingAttribute settingAttribute);
  protected abstract int getOldServiceDesiredCount(ExecutionContext context, SettingAttribute settingAttribute);
}
