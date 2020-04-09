package software.wings.sm.states;

import static io.harness.beans.OrchestrationWorkflowType.BASIC;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static software.wings.api.CommandStateExecutionData.Builder.aCommandStateExecutionData;
import static software.wings.beans.command.CommandExecutionContext.Builder.aCommandExecutionContext;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.mongodb.morphia.annotations.Transient;
import software.wings.annotation.EncryptableSetting;
import software.wings.api.CommandStateExecutionData;
import software.wings.api.CommandStateExecutionData.Builder;
import software.wings.api.ContainerServiceData;
import software.wings.api.ContainerServiceElement;
import software.wings.api.DeploymentType;
import software.wings.api.InstanceElementListParam;
import software.wings.api.PhaseElement;
import software.wings.api.ecs.EcsBGSetupData;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.DeploymentExecutionContext;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.ResizeStrategy;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.beans.command.ContainerSetupCommandUnitExecutionData;
import software.wings.beans.command.ContainerSetupParams;
import software.wings.beans.command.EcsSetupParams;
import software.wings.beans.command.KubernetesSetupParams;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.ImageDetails;
import software.wings.delegatetasks.aws.AwsCommandHelper;
import software.wings.service.impl.artifact.ArtifactCollectionUtils;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.State;
import software.wings.sm.StateExecutionData;
import software.wings.sm.WorkflowStandardParams;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by brett on 9/29/17
 */
@FieldNameConstants(innerTypeName = "ContainerServiceSetupKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public abstract class ContainerServiceSetup extends State {
  static final int DEFAULT_MAX = 2;

  private String desiredInstanceCount;
  private String fixedInstances;
  private String maxInstances; // Number for first time when using "Same as already running" in the UI
  private ResizeStrategy resizeStrategy;
  private int serviceSteadyStateTimeout; // Minutes
  @Inject @Transient protected SettingsService settingsService;
  @Inject @Transient protected ServiceResourceService serviceResourceService;
  @Inject @Transient protected InfrastructureMappingService infrastructureMappingService;
  @Inject @Transient protected ArtifactStreamService artifactStreamService;
  @Inject @Transient protected FeatureFlagService featureFlagService;
  @Inject @Transient protected SecretManager secretManager;
  @Inject @Transient protected EncryptionService encryptionService;
  @Inject @Transient protected ActivityService activityService;
  @Inject @Transient protected DelegateService delegateService;
  @Inject @Transient private AwsCommandHelper awsCommandHelper;
  @Inject @Transient private ArtifactCollectionUtils artifactCollectionUtils;

  ContainerServiceSetup(String name, String type) {
    super(name, type);
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
      PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM);
      String serviceId = phaseElement.getServiceElement().getUuid();

      WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
      Artifact artifact = ((DeploymentExecutionContext) context).getDefaultArtifactForService(serviceId);
      if (artifact == null) {
        throw new IllegalArgumentException(
            "Artifact is required. Please ensure that artifact provided as part of execution either Manual or through Trigger");
      }

      ImageDetails imageDetails =
          artifactCollectionUtils.fetchContainerImageDetails(artifact, context.getWorkflowExecutionId());

      Application app = workflowStandardParams.fetchRequiredApp();
      Environment env = workflowStandardParams.getEnv();
      Service service = serviceResourceService.getWithDetails(app.getUuid(), serviceId);

      logger.info("Setting up container service for service {}", service.getName());
      ContainerTask containerTask =
          serviceResourceService.getContainerTaskByDeploymentType(app.getUuid(), serviceId, getDeploymentType());

      InfrastructureMapping infrastructureMapping =
          infrastructureMappingService.get(app.getUuid(), context.fetchInfraMappingId());
      if (!(infrastructureMapping instanceof ContainerInfrastructureMapping)
          || !isValidInfraMapping(infrastructureMapping)) {
        throw new InvalidRequestException("Invalid infrastructure type");
      }

      ContainerInfrastructureMapping containerInfrastructureMapping =
          (ContainerInfrastructureMapping) infrastructureMapping;

      String clusterName = containerInfrastructureMapping.getClusterName();
      logger.info("Got cluster {} from container infra-mapping {} for cloud provider {}", clusterName,
          infrastructureMapping.getUuid(), infrastructureMapping.getComputeProviderSettingId());

      Command command =
          serviceResourceService.getCommandByName(app.getUuid(), serviceId, env.getUuid(), getCommandName())
              .getCommand();

      Activity activity = buildActivity(context, app, env, service, command);

      SettingAttribute settingAttribute = settingsService.get(infrastructureMapping.getComputeProviderSettingId());

      if (settingAttribute == null) {
        throw new InvalidArgumentsException(Pair.of("Cloud Provider", "Missing, check service infrastructure"));
      }

      List<EncryptedDataDetail> encryptedDataDetails = secretManager.getEncryptionDetails(
          (EncryptableSetting) settingAttribute.getValue(), context.getAppId(), context.getWorkflowExecutionId());

      ContainerSetupParams containerSetupParams = buildContainerSetupParams(context, service.getName(), imageDetails,
          app, env, service, containerInfrastructureMapping, containerTask, clusterName);

      StateExecutionData executionData =
          buildStateExecutionData(app, service, clusterName, activity, containerSetupParams);

      Map<String, String> serviceVariables = context.getServiceVariables().entrySet().stream().collect(
          Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));
      Map<String, String> safeDisplayServiceVariables = context.getSafeDisplayServiceVariables();

      if (serviceVariables != null) {
        serviceVariables.replaceAll((name, value) -> context.renderExpression(value));
      }

      if (safeDisplayServiceVariables != null) {
        safeDisplayServiceVariables.replaceAll((name, value) -> context.renderExpression(value));
      }

      DeploymentType deploymentType = serviceResourceService.getDeploymentType(infrastructureMapping, service, null);

      // Check is added to prevent deployment of a K8 V2 service with a V1 workflow.
      if (deploymentType == DeploymentType.KUBERNETES) {
        if (context.getOrchestrationWorkflowType() != null && context.getOrchestrationWorkflowType() == BASIC
            && service.isK8sV2()) {
          throw new InvalidRequestException(
              "Kubernetes V2 service is not allowed to be deployed by 'Basic' workflow type. "
              + "Please recreate appropriate workflow type for the selected service");
        }
      }

      CommandExecutionContext commandExecutionContext = aCommandExecutionContext()
                                                            .accountId(app.getAccountId())
                                                            .appId(app.getUuid())
                                                            .envId(env.getUuid())
                                                            .deploymentType(deploymentType.name())
                                                            .containerSetupParams(containerSetupParams)
                                                            .activityId(activity.getUuid())
                                                            .cloudProviderSetting(settingAttribute)
                                                            .cloudProviderCredentials(encryptedDataDetails)
                                                            .serviceVariables(serviceVariables)
                                                            .safeDisplayServiceVariables(safeDisplayServiceVariables)
                                                            .build();

      String delegateTaskId =
          delegateService.queueTask(DelegateTask.builder()
                                        .async(true)
                                        .accountId(app.getAccountId())
                                        .appId(app.getUuid())
                                        .waitId(activity.getUuid())
                                        .data(TaskData.builder()
                                                  .taskType(TaskType.COMMAND.name())
                                                  .parameters(new Object[] {command, commandExecutionContext})
                                                  .timeout(TimeUnit.HOURS.toMillis(1))
                                                  .build())
                                        .envId(env.getUuid())
                                        .tags(awsCommandHelper.getAwsConfigTagsFromContext(commandExecutionContext))
                                        .infrastructureMappingId(infrastructureMapping.getUuid())
                                        .build());

      return ExecutionResponse.builder()
          .async(true)
          .correlationIds(singletonList(activity.getUuid()))
          .stateExecutionData(executionData)
          .delegateTaskId(delegateTaskId)
          .build();

    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
  }

  private StateExecutionData buildStateExecutionData(Application app, Service service, String clusterName,
      Activity activity, ContainerSetupParams containerSetupParams) {
    Builder builder = aCommandStateExecutionData()
                          .withServiceId(service.getUuid())
                          .withServiceName(service.getName())
                          .withAppId(app.getUuid())
                          .withCommandName(getCommandName())
                          .withContainerSetupParams(containerSetupParams)
                          .withClusterName(clusterName)
                          .withActivityId(activity.getUuid());
    if (containerSetupParams instanceof KubernetesSetupParams) {
      builder.withNamespace(((KubernetesSetupParams) containerSetupParams).getNamespace());
    }
    return builder.build();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    try {
      logger.info("Received async response");
      CommandExecutionResult commandExecutionResult = (CommandExecutionResult) response.values().iterator().next();

      if (commandExecutionResult == null || commandExecutionResult.getStatus() != SUCCESS) {
        return buildEndStateExecution(context, commandExecutionResult, ExecutionStatus.FAILED);
      }

      return buildEndStateExecution(context, commandExecutionResult, ExecutionStatus.SUCCESS);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
  }

  private ExecutionResponse buildEndStateExecution(
      ExecutionContext context, CommandExecutionResult executionResult, ExecutionStatus status) {
    CommandStateExecutionData executionData = (CommandStateExecutionData) context.getStateExecutionData();
    activityService.updateStatus(executionData.getActivityId(), executionData.getAppId(), status);

    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM);
    String serviceId = phaseElement.getServiceElement().getUuid();
    Artifact artifact = ((DeploymentExecutionContext) context).getDefaultArtifactForService(serviceId);
    if (artifact == null) {
      throw new IllegalArgumentException("Artifact is null");
    }

    ImageDetails imageDetails =
        artifactCollectionUtils.fetchContainerImageDetails(artifact, context.getWorkflowExecutionId());

    ContainerServiceElement containerServiceElement =
        buildContainerServiceElement(context, executionResult, status, imageDetails);

    InstanceElementListParam instanceElementListParam =
        InstanceElementListParam.builder()
            .instanceElements(Optional
                                  .ofNullable(executionData.getNewInstanceStatusSummaries()
                                                  .stream()
                                                  .map(InstanceStatusSummary::getInstanceElement)
                                                  .collect(toList()))
                                  .orElse(emptyList()))
            .build();

    if (executionResult != null) {
      ContainerSetupCommandUnitExecutionData setupExecutionData =
          (ContainerSetupCommandUnitExecutionData) executionResult.getCommandExecutionData();
      if (setupExecutionData != null) {
        // This will be non-null, when its ECS Daemon workflow
        executionData.setPreviousEcsServiceSnapshotJson(setupExecutionData.getPreviousEcsServiceSnapshotJson());
        executionData.setEcsServiceArn(setupExecutionData.getEcsServiceArn());
        executionData.setNewInstanceData(singletonList(ContainerServiceData.builder()
                                                           .name(setupExecutionData.getContainerServiceName())
                                                           .uniqueIdentifier(setupExecutionData.getEcsTaskDefintion())
                                                           .build()));
        executionData.setServiceName(setupExecutionData.getContainerServiceName());
        executionData.setLoadBalancer(containerServiceElement.getLoadBalancer());
        executionData.setPreviousAwsAutoScalarConfigs(setupExecutionData.getPreviousAwsAutoScalarConfigs());

        containerServiceElement.setPreviousAwsAutoScalarConfigs(setupExecutionData.getPreviousAwsAutoScalarConfigs());
        containerServiceElement.setNewEcsServiceName(setupExecutionData.getContainerServiceName());
        containerServiceElement.setEcsRegion(setupExecutionData.getEcsRegion());
        containerServiceElement.setTargetGroupForNewService(setupExecutionData.getTargetGroupForNewService());
        containerServiceElement.setTargetGroupForExistingService(setupExecutionData.getTargetGroupForExistingService());

        if (containesEcsBGSetupParams(executionData)) {
          EcsSetupParams ecsSetupParams = (EcsSetupParams) executionData.getContainerSetupParams();
          containerServiceElement.setEcsBGSetupData(
              EcsBGSetupData.builder()
                  .prodEcsListener(ecsSetupParams.getProdListenerArn())
                  .stageEcsListener(setupExecutionData.getStageEcsListener())
                  .ecsBGTargetGroup1(ecsSetupParams.getTargetGroupArn())
                  .ecsBGTargetGroup2(ecsSetupParams.getTargetGroupArn2())
                  .ecsBlueGreen(true)
                  .downsizedServiceName(setupExecutionData.getEcsServiceToBeDownsized())
                  .downsizedServiceCount(setupExecutionData.getCountToBeDownsizedForOldService())
                  .parentRecordName(setupExecutionData.getParentRecordName())
                  .parentRecordHostedZoneId(setupExecutionData.getParentRecordHostedZoneId())
                  .useRoute53Swap(setupExecutionData.isUseRoute53Swap())
                  .oldServiceDiscoveryArn(setupExecutionData.getOldServiceDiscoveryArn())
                  .newServiceDiscoveryArn(setupExecutionData.getNewServiceDiscoveryArn())
                  .build());
        }
      }
      executionData.setDelegateMetaInfo(executionResult.getDelegateMetaInfo());
    }

    return ExecutionResponse.builder()
        .stateExecutionData(executionData)
        .executionStatus(status)
        .contextElement(containerServiceElement)
        .contextElement(instanceElementListParam)
        .notifyElement(containerServiceElement)
        .notifyElement(instanceElementListParam)
        .build();
  }

  private boolean containesEcsBGSetupParams(CommandStateExecutionData executionData) {
    if (!(executionData.getContainerSetupParams() instanceof EcsSetupParams)) {
      return false;
    }

    EcsSetupParams ecsSetupParams = (EcsSetupParams) executionData.getContainerSetupParams();
    return ecsSetupParams.isBlueGreen();
  }

  public String getDesiredInstanceCount() {
    return desiredInstanceCount;
  }

  public void setDesiredInstanceCount(String desiredInstanceCount) {
    this.desiredInstanceCount = desiredInstanceCount;
  }

  public String getFixedInstances() {
    return fixedInstances;
  }

  public void setFixedInstances(String fixedInstances) {
    this.fixedInstances = fixedInstances;
  }

  public String getMaxInstances() {
    return maxInstances;
  }

  public void setMaxInstances(String maxInstances) {
    this.maxInstances = maxInstances;
  }

  public ResizeStrategy getResizeStrategy() {
    return resizeStrategy;
  }

  public void setResizeStrategy(ResizeStrategy resizeStrategy) {
    this.resizeStrategy = resizeStrategy;
  }

  public int getServiceSteadyStateTimeout() {
    return serviceSteadyStateTimeout;
  }

  public void setServiceSteadyStateTimeout(int serviceSteadyStateTimeout) {
    this.serviceSteadyStateTimeout = serviceSteadyStateTimeout;
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  private Activity buildActivity(
      ExecutionContext context, Application app, Environment env, Service service, Command command) {
    Activity activity = Activity.builder()
                            .applicationName(app.getName())
                            .environmentId(env.getUuid())
                            .environmentName(env.getName())
                            .environmentType(env.getEnvironmentType())
                            .serviceId(service.getUuid())
                            .serviceName(service.getName())
                            .commandName(command.getName())
                            .type(Activity.Type.Command)
                            .workflowExecutionId(context.getWorkflowExecutionId())
                            .workflowType(context.getWorkflowType())
                            .workflowId(context.getWorkflowId())
                            .workflowExecutionName(context.getWorkflowExecutionName())
                            .stateExecutionInstanceId(context.getStateExecutionInstanceId())
                            .stateExecutionInstanceName(context.getStateExecutionInstanceName())
                            .commandUnits(serviceResourceService.getFlattenCommandUnitList(
                                app.getUuid(), service.getUuid(), env.getUuid(), command.getName()))
                            .commandType(command.getCommandUnitType().name())
                            .status(ExecutionStatus.RUNNING)
                            .build();

    activity.setAppId(app.getUuid());
    return activityService.save(activity);
  }

  protected abstract String getDeploymentType();

  public abstract String getCommandName();

  protected abstract ContainerSetupParams buildContainerSetupParams(ExecutionContext context, String serviceName,
      ImageDetails imageDetails, Application app, Environment env, Service service,
      ContainerInfrastructureMapping infrastructureMapping, ContainerTask containerTask, String clusterName);

  protected abstract boolean isValidInfraMapping(InfrastructureMapping infrastructureMapping);

  protected abstract ContainerServiceElement buildContainerServiceElement(ExecutionContext context,
      CommandExecutionResult executionResult, ExecutionStatus status, ImageDetails imageDetails);
}
