package software.wings.sm.states;

import com.github.reinert.jjschema.Attributes;
import com.google.inject.Inject;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.CommandStateExecutionData;
import software.wings.api.KubernetesServiceElement;
import software.wings.api.PhaseElement;
import software.wings.beans.Activity;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.common.Constants;
import software.wings.common.UUIDGenerator;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.waitnotify.NotifyResponseData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static software.wings.api.CommandStateExecutionData.Builder.aCommandStateExecutionData;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.beans.Activity.Builder.anActivity;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.command.CommandExecutionContext.Builder.aCommandExecutionContext;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.InstanceStatusSummary.InstanceStatusSummaryBuilder.anInstanceStatusSummary;

/**
 * Created by brett on 3/1/17
 * TODO(brett): Implement
 */
public class KubernetesServiceDeploy extends State {
  private static final Logger logger = LoggerFactory.getLogger(KubernetesServiceDeploy.class);

  @Attributes(title = "Command")
  @EnumData(enumDataProvider = CommandStateEnumDataProvider.class)
  @DefaultValue("Resize Cluster")
  private String commandName;

  @Attributes(title = "Number of instances") private int instanceCount;

  @Inject @Transient private transient SettingsService settingsService;

  @Inject @Transient private transient DelegateService delegateService;

  @Inject @Transient private transient ServiceResourceService serviceResourceService;

  @Inject @Transient private transient ActivityService activityService;

  @Inject @Transient private transient InfrastructureMappingService infrastructureMappingService;

  @Inject @Transient private transient GkeClusterService gkeClusterService;

  public KubernetesServiceDeploy(String name) {
    super(name, StateType.KUBERNETES_SERVICE_DEPLOY.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    CommandStateExecutionData.Builder executionDataBuilder = aCommandStateExecutionData();

    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String serviceId = phaseElement.getServiceElement().getUuid();

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    Application app = workflowStandardParams.getApp();
    Environment env = workflowStandardParams.getEnv();

    Service service = serviceResourceService.get(app.getAppId(), serviceId);
    Command command =
        serviceResourceService.getCommandByName(app.getUuid(), serviceId, env.getUuid(), commandName).getCommand();

    KubernetesServiceElement kubernetesServiceElement =
        context.getContextElement(ContextElementType.KUBERNETES_SERVICE);

    String kubernetesServiceName = kubernetesServiceElement.getName();

    InfrastructureMapping infrastructureMapping =
        infrastructureMappingService.get(app.getUuid(), phaseElement.getInfraMappingId());
    SettingAttribute settingAttribute = settingsService.get(infrastructureMapping.getComputeProviderSettingId());

    // TODO(brett): Implement
    int desiredCount = 0;

    executionDataBuilder.withServiceId(service.getUuid())
        .withServiceName(service.getName())
        .withAppId(app.getUuid())
        .withCommandName(commandName);

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
        buildCommandExecutionContext(app, env.getUuid(), kubernetesServiceElement.getClusterName(),
            kubernetesServiceName, desiredCount, activity.getUuid(), settingAttribute);
    executionDataBuilder.withActivityId(activity.getUuid()).withNewContainerServiceName(kubernetesServiceName);

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
    KubernetesServiceElement kubernetesServiceElement =
        context.getContextElement(ContextElementType.KUBERNETES_SERVICE);
    CommandStateExecutionData commandStateExecutionData = (CommandStateExecutionData) context.getStateExecutionData();

    commandStateExecutionData.setInstanceStatusSummaries(buildInstanceStatusSummaries(context, response));

    if (commandStateExecutionData.getOldContainerServiceName() == null) {
      String ecsServiceName = kubernetesServiceElement.getOldName();

      PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
      InfrastructureMapping infrastructureMapping =
          infrastructureMappingService.get(commandStateExecutionData.getAppId(), phaseElement.getInfraMappingId());
      SettingAttribute settingAttribute = settingsService.get(infrastructureMapping.getComputeProviderSettingId());

      // TODO(brett): Implement

      commandStateExecutionData.setOldContainerServiceName(ecsServiceName);

      WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
      Application app = workflowStandardParams.getApp();
      Environment env = workflowStandardParams.getEnv();

      String serviceId = phaseElement.getServiceElement().getUuid();

      Command command = serviceResourceService
                            .getCommandByName(commandStateExecutionData.getAppId(),
                                commandStateExecutionData.getServiceId(), env.getUuid(), commandName)
                            .getCommand();

      // TODO(brett): Implement
      int desiredCount = 0;

      logger.info("Desired count for service {} is {}", ecsServiceName, desiredCount);

      if (desiredCount < 0) {
        desiredCount = 0;
      }
      CommandExecutionContext commandExecutionContext =
          buildCommandExecutionContext(app, env.getUuid(), kubernetesServiceElement.getClusterName(), ecsServiceName,
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
      return anExecutionResponse()
          .withStateExecutionData(commandStateExecutionData)
          .withExecutionStatus(ExecutionStatus.SUCCESS)
          .build();
    }
  }

  private List<InstanceStatusSummary> buildInstanceStatusSummaries(
      ExecutionContext context, Map<String, NotifyResponseData> response) {
    // TODO: set actual containers

    List<InstanceStatusSummary> instanceStatusSummaries = new ArrayList<>();
    IntStream.range(0, instanceCount).parallel().forEach(value -> {
      String uuid = UUIDGenerator.getUuid();
      instanceStatusSummaries.add(
          anInstanceStatusSummary()
              .withStatus(ExecutionStatus.SUCCESS)
              .withInstanceElement(anInstanceElement().withUuid(uuid).withDisplayName(uuid).build())
              .build());
    });
    return instanceStatusSummaries;
  }

  private CommandExecutionContext buildCommandExecutionContext(Application app, String envId, String clusterName,
      String ecsServiceName, int desiredCount, String activityId, SettingAttribute settingAttribute) {
    CommandExecutionContext commandExecutionContext =
        aCommandExecutionContext().withAccountId(app.getAccountId()).withAppId(app.getUuid()).withEnvId(envId).build();
    commandExecutionContext.setClusterName(clusterName);

    commandExecutionContext.setServiceName(ecsServiceName);
    commandExecutionContext.setActivityId(activityId);
    commandExecutionContext.setCloudProviderSetting(settingAttribute);
    commandExecutionContext.setDesiredCount(desiredCount);

    return commandExecutionContext;
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  public String getCommandName() {
    return commandName;
  }

  public void setCommandName(String commandName) {
    this.commandName = commandName;
  }

  public int getInstanceCount() {
    return instanceCount;
  }

  public void setInstanceCount(int instanceCount) {
    this.instanceCount = instanceCount;
  }
}
