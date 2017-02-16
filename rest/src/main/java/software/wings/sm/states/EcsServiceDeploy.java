package software.wings.sm.states;

import static software.wings.api.CommandStateExecutionData.Builder.aCommandStateExecutionData;
import static software.wings.beans.Activity.Builder.anActivity;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.command.CommandExecutionContext.Builder.aCommandExecutionContext;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.CommandStateExecutionData;
import software.wings.api.EcsServiceElement;
import software.wings.api.PhaseElement;
import software.wings.beans.Activity;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.ErrorCodes;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.cloudprovider.ClusterService;
import software.wings.common.Constants;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Created by rishi on 2/8/17.
 */
public class EcsServiceDeploy extends State {
  private static final Logger logger = LoggerFactory.getLogger(EcsServiceDeploy.class);

  @Attributes(title = "Command")
  @EnumData(enumDataProvider = CommandStateEnumDataProvider.class)
  @DefaultValue("Resize Service Cluster")
  private String commandName;

  @Attributes(title = "Number of instances") private int instanceCount;

  @Attributes(title = "Remove Old Containers") private boolean reduceOldVersion;

  @Inject @Transient private transient SettingsService settingsService;

  @Inject @Transient private transient DelegateService delegateService;

  @Inject @Transient private transient ServiceResourceService serviceResourceService;

  @Inject @Transient private transient ActivityService activityService;

  @Inject @Transient private transient InfrastructureMappingService infrastructureMappingService;

  @Inject @Transient private transient ClusterService clusterService;

  public EcsServiceDeploy(String name) {
    super(name, StateType.ECS_SERVICE_DEPLOY.name());
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

    EcsServiceElement ecsServiceElement = context.getContextElement(ContextElementType.ECS_SERVICE);

    CommandExecutionContext commandExecutionContext = aCommandExecutionContext()
                                                          .withAccountId(app.getAccountId())
                                                          .withAppId(app.getUuid())
                                                          .withEnvId(env.getUuid())
                                                          .build();
    commandExecutionContext.setClusterName(ecsServiceElement.getClusterName());

    String ecsServiceName;
    if (reduceOldVersion) {
      ecsServiceName = ecsServiceElement.getOldName();
      if (reduceOldVersion && ecsServiceName == null) {
        logger.warn("No old ECS Service found to be undeployed");
        return new ExecutionResponse();
      }
    } else {
      ecsServiceName = ecsServiceElement.getName();
    }

    commandExecutionContext.setServiceName(ecsServiceName);

    InfrastructureMapping infrastructureMapping =
        infrastructureMappingService.get(app.getUuid(), phaseElement.getInfraMappingId());

    SettingAttribute settingAttribute = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
    commandExecutionContext.setCloudProviderSetting(settingAttribute);

    List<com.amazonaws.services.ecs.model.Service> services =
        clusterService.getServices(settingAttribute, ecsServiceElement.getClusterName());
    Optional<com.amazonaws.services.ecs.model.Service> ecsService =
        services.stream().filter(svc -> svc.getServiceName().equals(ecsServiceName)).findFirst();
    if (!ecsService.isPresent()) {
      if (reduceOldVersion) {
        logger.info("Old ECS Service {} does not exist.. nothing to do", ecsServiceName);
        return new ExecutionResponse();
      } else {
        throw new WingsException(ErrorCodes.INVALID_REQUEST, "message", "ECS Service not setup");
      }
    }
    int desiredCount;
    if (reduceOldVersion) {
      desiredCount = ecsService.get().getDesiredCount() - instanceCount;
    } else {
      desiredCount = ecsService.get().getDesiredCount() + instanceCount;
    }
    if (desiredCount < 0) {
      desiredCount = 0;
    }

    logger.info("Desired count for service {} is {}", ecsServiceName, desiredCount);
    commandExecutionContext.setDesiredCount(desiredCount);

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
    commandExecutionContext.setActivityId(activity.getUuid());

    executionDataBuilder.withActivityId(activity.getUuid());

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

  public boolean isReduceOldVersion() {
    return reduceOldVersion;
  }

  public void setReduceOldVersion(boolean reduceOldVersion) {
    this.reduceOldVersion = reduceOldVersion;
  }
}
