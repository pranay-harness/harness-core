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
import io.fabric8.kubernetes.api.model.ReplicationController;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.CommandStateExecutionData;
import software.wings.api.InstanceElement;
import software.wings.api.InstanceElementListParam;
import software.wings.api.KubernetesReplicationControllerElement;
import software.wings.api.PhaseElement;
import software.wings.beans.Activity;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.ErrorCode;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.KubernetesConfig;
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
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.common.Constants;
import software.wings.exception.WingsException;
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
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.waitnotify.NotifyResponseData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by brett on 3/1/17
 */
public class KubernetesReplicationControllerDeploy extends State {
  private static final Logger logger = LoggerFactory.getLogger(KubernetesReplicationControllerDeploy.class);

  @Attributes(title = "Command")
  @EnumData(enumDataProvider = CommandStateEnumDataProvider.class)
  @DefaultValue("Resize Replication Controller")
  private String commandName;

  @Attributes(title = "Number of instances") private int instanceCount;

  @Inject @Transient private transient SettingsService settingsService;

  @Inject @Transient private transient DelegateService delegateService;

  @Inject @Transient private transient ServiceResourceService serviceResourceService;

  @Inject @Transient private transient ActivityService activityService;

  @Inject @Transient private transient InfrastructureMappingService infrastructureMappingService;

  @Inject @Transient private transient GkeClusterService gkeClusterService;

  @Inject @Transient private transient KubernetesContainerService kubernetesContainerService;

  @Inject @Transient private transient ServiceTemplateService serviceTemplateService;

  public KubernetesReplicationControllerDeploy(String name) {
    super(name, StateType.KUBERNETES_REPLICATION_CONTROLLER_DEPLOY.name());
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
        serviceResourceService.getCommandByName(app.getUuid(), serviceId, env.getUuid(), commandName).getCommand();

    KubernetesReplicationControllerElement kubernetesReplicationControllerElement =
        context.getContextElement(ContextElementType.KUBERNETES_REPLICATION_CONTROLLER);

    String replicationControllerName = kubernetesReplicationControllerElement.getName();
    String clusterName = kubernetesReplicationControllerElement.getClusterName();

    InfrastructureMapping infrastructureMapping =
        infrastructureMappingService.get(app.getUuid(), phaseElement.getInfraMappingId());
    SettingAttribute settingAttribute = settingsService.get(infrastructureMapping.getComputeProviderSettingId());

    KubernetesConfig kubernetesConfig = gkeClusterService.getCluster(settingAttribute, clusterName);
    ReplicationController replicationController =
        kubernetesContainerService.getController(kubernetesConfig, replicationControllerName);

    if (replicationController == null) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message",
          "Kubernetes replication controller setup not done, controllerName: " + replicationControllerName);
    }

    int desiredCount = replicationController.getSpec().getReplicas() + instanceCount;
    logger.info("Desired count for service {} is {}", replicationControllerName, desiredCount);

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
        buildCommandExecutionContext(app, env.getUuid(), kubernetesReplicationControllerElement.getClusterName(),
            replicationControllerName, desiredCount, activity.getUuid(), settingAttribute);
    executionDataBuilder.withActivityId(activity.getUuid()).withNewContainerServiceName(replicationControllerName);

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
    KubernetesReplicationControllerElement replicationControllerElement =
        context.getContextElement(ContextElementType.KUBERNETES_REPLICATION_CONTROLLER);
    CommandStateExecutionData commandStateExecutionData = (CommandStateExecutionData) context.getStateExecutionData();

    CommandExecutionResult commandExecutionResult = ((CommandExecutionResult) response.values().iterator().next());
    if (commandExecutionResult == null || commandExecutionResult.getStatus() != CommandExecutionStatus.SUCCESS) {
      return buildEndStateExecution(commandStateExecutionData, ExecutionStatus.FAILED);
    }

    if (commandStateExecutionData.getOldContainerServiceName() == null) {
      commandStateExecutionData.setInstanceStatusSummaries(buildInstanceStatusSummaries(context, response));
      String replicationControllerName = replicationControllerElement.getOldName();

      WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
      PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
      InfrastructureMapping infrastructureMapping =
          infrastructureMappingService.get(workflowStandardParams.getAppId(), phaseElement.getInfraMappingId());
      SettingAttribute settingAttribute = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
      KubernetesConfig kubernetesConfig =
          gkeClusterService.getCluster(settingAttribute, replicationControllerElement.getClusterName());
      ReplicationController replicationController =
          kubernetesContainerService.getController(kubernetesConfig, replicationControllerName);
      if (replicationController == null) {
        logger.info(
            "Old kubernetes replication controller {} does not exist.. nothing to do", replicationControllerName);
        return buildEndStateExecution(commandStateExecutionData, ExecutionStatus.SUCCESS);
      }

      commandStateExecutionData.setOldContainerServiceName(replicationControllerName);

      Application app = workflowStandardParams.getApp();
      Environment env = workflowStandardParams.getEnv();

      Command command = serviceResourceService
                            .getCommandByName(workflowStandardParams.getAppId(),
                                phaseElement.getServiceElement().getUuid(), env.getUuid(), commandName)
                            .getCommand();

      int desiredCount = replicationController.getSpec().getReplicas() - instanceCount;
      logger.info("Desired count for service {} is {}", replicationControllerName, desiredCount);

      if (desiredCount < 0) {
        desiredCount = 0;
      }
      CommandExecutionContext commandExecutionContext =
          buildCommandExecutionContext(app, env.getUuid(), replicationControllerElement.getClusterName(),
              replicationControllerName, desiredCount, commandStateExecutionData.getActivityId(), settingAttribute);

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
        && ((ResizeCommandUnitExecutionData) commandExecutionData).getHostNames() != null) {
      ((ResizeCommandUnitExecutionData) commandExecutionData)
          .getHostNames()
          .forEach(podName
              -> instanceStatusSummaries.add(
                  anInstanceStatusSummary()
                      .withStatus(ExecutionStatus.SUCCESS)
                      .withInstanceElement(
                          anInstanceElement()
                              .withUuid(podName)
                              .withHostElement(aHostElement().withHostName(podName).build())
                              .withServiceTemplateElement(aServiceTemplateElement()
                                                              .withUuid(serviceTemplateKey.getId().toString())
                                                              .withServiceElement(phaseElement.getServiceElement())
                                                              .build())
                              .withDisplayName(podName)
                              .build())
                      .build()));
    }

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

  public static final class KubernetesReplicationControllerDeployBuilder {
    private static Logger logger = LoggerFactory.getLogger(KubernetesReplicationControllerDeploy.class);
    private String id;
    private String name;
    private ContextElementType requiredContextElementType;
    private String stateType;
    private boolean rollback;
    private String commandName;
    private int instanceCount;
    private transient SettingsService settingsService;
    private transient DelegateService delegateService;
    private transient ServiceResourceService serviceResourceService;
    private transient ActivityService activityService;
    private transient InfrastructureMappingService infrastructureMappingService;
    private transient GkeClusterService gkeClusterService;
    private transient KubernetesContainerService kubernetesContainerService;

    private KubernetesReplicationControllerDeployBuilder(String name) {
      this.name = name;
    }

    public static KubernetesReplicationControllerDeployBuilder aKubernetesReplicationControllerDeploy(String name) {
      return new KubernetesReplicationControllerDeployBuilder(name);
    }

    public KubernetesReplicationControllerDeployBuilder withId(String id) {
      this.id = id;
      return this;
    }

    public KubernetesReplicationControllerDeployBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public KubernetesReplicationControllerDeployBuilder withRequiredContextElementType(
        ContextElementType requiredContextElementType) {
      this.requiredContextElementType = requiredContextElementType;
      return this;
    }

    public KubernetesReplicationControllerDeployBuilder withStateType(String stateType) {
      this.stateType = stateType;
      return this;
    }

    public KubernetesReplicationControllerDeployBuilder withRollback(boolean rollback) {
      this.rollback = rollback;
      return this;
    }

    public KubernetesReplicationControllerDeployBuilder withLogger(Logger logger) {
      this.logger = logger;
      return this;
    }

    public KubernetesReplicationControllerDeployBuilder withCommandName(String commandName) {
      this.commandName = commandName;
      return this;
    }

    public KubernetesReplicationControllerDeployBuilder withInstanceCount(int instanceCount) {
      this.instanceCount = instanceCount;
      return this;
    }

    public KubernetesReplicationControllerDeployBuilder withSettingsService(SettingsService settingsService) {
      this.settingsService = settingsService;
      return this;
    }

    public KubernetesReplicationControllerDeployBuilder withDelegateService(DelegateService delegateService) {
      this.delegateService = delegateService;
      return this;
    }

    public KubernetesReplicationControllerDeployBuilder withServiceResourceService(
        ServiceResourceService serviceResourceService) {
      this.serviceResourceService = serviceResourceService;
      return this;
    }

    public KubernetesReplicationControllerDeployBuilder withActivityService(ActivityService activityService) {
      this.activityService = activityService;
      return this;
    }

    public KubernetesReplicationControllerDeployBuilder withInfrastructureMappingService(
        InfrastructureMappingService infrastructureMappingService) {
      this.infrastructureMappingService = infrastructureMappingService;
      return this;
    }

    public KubernetesReplicationControllerDeployBuilder withGkeClusterService(GkeClusterService gkeClusterService) {
      this.gkeClusterService = gkeClusterService;
      return this;
    }

    public KubernetesReplicationControllerDeployBuilder withKubernetesContainerService(
        KubernetesContainerService kubernetesContainerService) {
      this.kubernetesContainerService = kubernetesContainerService;
      return this;
    }

    public KubernetesReplicationControllerDeployBuilder but() {
      return aKubernetesReplicationControllerDeploy(name)
          .withId(id)
          .withRequiredContextElementType(requiredContextElementType)
          .withStateType(stateType)
          .withRollback(rollback)
          .withLogger(logger)
          .withCommandName(commandName)
          .withInstanceCount(instanceCount)
          .withSettingsService(settingsService)
          .withDelegateService(delegateService)
          .withServiceResourceService(serviceResourceService)
          .withActivityService(activityService)
          .withInfrastructureMappingService(infrastructureMappingService)
          .withGkeClusterService(gkeClusterService)
          .withKubernetesContainerService(kubernetesContainerService);
    }

    public KubernetesReplicationControllerDeploy build() {
      KubernetesReplicationControllerDeploy kubernetesReplicationControllerDeploy =
          new KubernetesReplicationControllerDeploy(name);
      kubernetesReplicationControllerDeploy.setId(id);
      kubernetesReplicationControllerDeploy.setRequiredContextElementType(requiredContextElementType);
      kubernetesReplicationControllerDeploy.setStateType(stateType);
      kubernetesReplicationControllerDeploy.setRollback(rollback);
      kubernetesReplicationControllerDeploy.setCommandName(commandName);
      kubernetesReplicationControllerDeploy.setInstanceCount(instanceCount);
      kubernetesReplicationControllerDeploy.infrastructureMappingService = this.infrastructureMappingService;
      kubernetesReplicationControllerDeploy.gkeClusterService = this.gkeClusterService;
      kubernetesReplicationControllerDeploy.activityService = this.activityService;
      kubernetesReplicationControllerDeploy.kubernetesContainerService = this.kubernetesContainerService;
      kubernetesReplicationControllerDeploy.settingsService = this.settingsService;
      kubernetesReplicationControllerDeploy.delegateService = this.delegateService;
      kubernetesReplicationControllerDeploy.serviceResourceService = this.serviceResourceService;
      return kubernetesReplicationControllerDeploy;
    }
  }
}
