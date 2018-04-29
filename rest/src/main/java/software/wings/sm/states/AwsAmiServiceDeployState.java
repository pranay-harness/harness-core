package software.wings.sm.states;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.api.HostElement.Builder.aHostElement;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.api.ServiceTemplateElement.Builder.aServiceTemplateElement;
import static software.wings.beans.InstanceUnitType.PERCENTAGE;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.common.Constants.ASG_COMMAND_NAME;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.InstanceStatusSummary.InstanceStatusSummaryBuilder.anInstanceStatusSummary;
import static software.wings.waitnotify.StringNotifyResponseData.Builder.aStringNotifyResponseData;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.github.reinert.jjschema.Attributes;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.Encryptable;
import software.wings.api.AmiServiceSetupElement;
import software.wings.api.AwsAmiDeployStateExecutionData;
import software.wings.api.ContainerServiceData;
import software.wings.api.HostElement;
import software.wings.api.InstanceElement;
import software.wings.api.InstanceElementListParam;
import software.wings.api.InstanceElementListParam.InstanceElementListParamBuilder;
import software.wings.api.PhaseElement;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.AwsConfig;
import software.wings.beans.DeploymentExecutionContext;
import software.wings.beans.Environment;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.Log.Builder;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.ResizeStrategy;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.CommandUnit;
import software.wings.common.Constants;
import software.wings.exception.InvalidRequestException;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.LogService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.State;
import software.wings.sm.StateExecutionException;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.utils.Validator;
import software.wings.waitnotify.NotifyResponseData;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by anubhaw on 12/19/17.
 */
public class AwsAmiServiceDeployState extends State {
  @Transient private static final transient Logger logger = LoggerFactory.getLogger(AwsAmiServiceDeployState.class);

  @Attributes(title = "Desired Instances (cumulative)") private int instanceCount;

  @Attributes(title = "Instance Unit Type (Count/Percent)")
  @EnumData(enumDataProvider = InstanceUnitTypeDataProvider.class)
  @DefaultValue("COUNT")
  private InstanceUnitType instanceUnitType = InstanceUnitType.COUNT;

  @Attributes(title = "Command") @DefaultValue(ASG_COMMAND_NAME) private String commandName = ASG_COMMAND_NAME;

  @Inject @Transient protected transient AwsHelperService awsHelperService;
  @Inject @Transient protected transient SettingsService settingsService;
  @Inject @Transient protected transient ServiceResourceService serviceResourceService;
  @Inject @Transient protected transient ServiceTemplateService serviceTemplateService;
  @Inject @Transient protected transient InfrastructureMappingService infrastructureMappingService;
  @Inject @Transient protected transient ArtifactStreamService artifactStreamService;
  @Inject @Transient protected transient SecretManager secretManager;
  @Inject @Transient protected transient EncryptionService encryptionService;
  @Inject @Transient protected transient ActivityService activityService;
  @Inject @Transient protected transient DelegateService delegateService;
  @Inject @Transient protected transient LogService logService;
  @Inject @Named("waitStateResumer") @Transient private ScheduledExecutorService executorService;
  @Transient @Inject private transient WaitNotifyEngine waitNotifyEngine;

  /**
   * Instantiates a new state.
   *
   * @param name the name
   */
  public AwsAmiServiceDeployState(String name) {
    this(name, StateType.AWS_AMI_SERVICE_DEPLOY.name());
  }

  public AwsAmiServiceDeployState(String name, String type) {
    super(name, type);
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    Activity activity = crateActivity(context);
    AwsAmiDeployStateExecutionData awsAmiDeployStateExecutionData = prepareStateExecutionData(context, activity);
    executorService.schedule(new SimpleNotifier(waitNotifyEngine, activity.getUuid(),
                                 aStringNotifyResponseData().withData(activity.getUuid()).build()),
        5, TimeUnit.SECONDS);
    return anExecutionResponse()
        .withAsync(true)
        .withStateExecutionData(awsAmiDeployStateExecutionData)
        .withExecutionStatus(ExecutionStatus.SUCCESS)
        .addCorrelationIds(activity.getUuid())
        .build();
  }

  protected Activity crateActivity(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String serviceId = phaseElement.getServiceElement().getUuid();

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    Application app = workflowStandardParams.getApp();
    Environment env = workflowStandardParams.getEnv();
    Service service = serviceResourceService.get(app.getUuid(), serviceId);

    Artifact artifact = ((DeploymentExecutionContext) context).getArtifactForService(serviceId);
    if (artifact == null) {
      throw new StateExecutionException(format("Unable to find artifact for service %s", service.getName()));
    }
    ArtifactStream artifactStream = artifactStreamService.get(artifact.getAppId(), artifact.getArtifactStreamId());

    Command command =
        serviceResourceService.getCommandByName(app.getUuid(), serviceId, env.getUuid(), getCommandName()).getCommand();
    List<CommandUnit> commandUnitList =
        serviceResourceService.getFlattenCommandUnitList(app.getUuid(), serviceId, env.getUuid(), getCommandName());
    ActivityBuilder activityBuilder = Activity.builder()
                                          .applicationName(app.getName())
                                          .environmentId(env.getUuid())
                                          .environmentName(env.getName())
                                          .environmentType(env.getEnvironmentType())
                                          .serviceId(service.getUuid())
                                          .serviceName(service.getName())
                                          .commandName(getCommandName())
                                          .type(Type.Command)
                                          .workflowExecutionId(context.getWorkflowExecutionId())
                                          .workflowId(context.getWorkflowId())
                                          .workflowType(context.getWorkflowType())
                                          .workflowExecutionName(context.getWorkflowExecutionName())
                                          .stateExecutionInstanceId(context.getStateExecutionInstanceId())
                                          .stateExecutionInstanceName(context.getStateExecutionInstanceName())
                                          .commandUnits(commandUnitList)
                                          .commandType(command.getCommandUnitType().name())
                                          .serviceVariables(context.getServiceVariables())
                                          .status(ExecutionStatus.RUNNING)
                                          .artifactStreamId(artifactStream.getUuid())
                                          .artifactStreamName(artifactStream.getSourceName())
                                          .artifactName(artifact.getDisplayName())
                                          .artifactId(artifact.getUuid())
                                          .artifactId(artifact.getUuid())
                                          .artifactName(artifact.getDisplayName());

    Activity build = activityBuilder.build();
    build.setAppId(app.getUuid());
    return activityService.save(build);
  }

  protected AwsAmiDeployStateExecutionData prepareStateExecutionData(ExecutionContext context, Activity activity) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    AmiServiceSetupElement serviceSetupElement = context.getContextElement(ContextElementType.AMI_SERVICE_SETUP);

    AwsAmiDeployStateExecutionData awsAmiDeployStateExecutionData;
    AwsAmiInfrastructureMapping infrastructureMapping = (AwsAmiInfrastructureMapping) infrastructureMappingService.get(
        activity.getAppId(), phaseElement.getInfraMappingId());
    SettingAttribute cloudProviderSetting = settingsService.get(infrastructureMapping.getComputeProviderSettingId());

    String region = infrastructureMapping.getRegion();
    AwsConfig awsConfig = (AwsConfig) cloudProviderSetting.getValue();

    List<EncryptedDataDetail> encryptionDetails =
        secretManager.getEncryptionDetails(awsConfig, context.getAppId(), context.getWorkflowExecutionId());

    Integer totalExpectedCount;
    if (getInstanceUnitType() == PERCENTAGE) {
      int percent = Math.min(getInstanceCount(), 100);
      int instanceCount1 = (int) Math.round((percent * serviceSetupElement.getMaxInstances()) / 100.0);
      totalExpectedCount = Math.max(instanceCount1, 1);
    } else {
      totalExpectedCount = getInstanceCount();
    }

    String newAutoScalingGroupName = serviceSetupElement.getNewAutoScalingGroupName();
    AutoScalingGroup newAutoScalingGroup =
        awsHelperService.getAutoScalingGroup(awsConfig, encryptionDetails, region, newAutoScalingGroupName);
    Integer newAutoScalingGroupDesiredCapacity = newAutoScalingGroup.getDesiredCapacity();
    Integer totalNewInstancesToBeAdded = Math.max(0, totalExpectedCount - newAutoScalingGroupDesiredCapacity);
    Integer newAsgFinalDesiredCount = newAutoScalingGroupDesiredCapacity + totalNewInstancesToBeAdded;
    List<ContainerServiceData> newInstanceData = asList(ContainerServiceData.builder()
                                                            .name(newAutoScalingGroupName)
                                                            .desiredCount(newAsgFinalDesiredCount)
                                                            .previousCount(newAutoScalingGroupDesiredCapacity)
                                                            .build());

    String oldAutoScalingGroupName = serviceSetupElement.getOldAutoScalingGroupName();
    AutoScalingGroup oldAutoScalingGroup =
        awsHelperService.getAutoScalingGroup(awsConfig, encryptionDetails, region, oldAutoScalingGroupName);

    Integer oldAutoScalingGroupDesiredCapacity = oldAutoScalingGroup.getDesiredCapacity();
    Integer oldAsgFinalDesiredCount = Math.max(0, oldAutoScalingGroupDesiredCapacity - totalNewInstancesToBeAdded);
    List<ContainerServiceData> oldInstanceData = asList(ContainerServiceData.builder()
                                                            .name(oldAutoScalingGroupName)
                                                            .desiredCount(oldAsgFinalDesiredCount)
                                                            .previousCount(oldAutoScalingGroupDesiredCapacity)
                                                            .build());

    awsAmiDeployStateExecutionData = prepareStateExecutionData(activity.getUuid(), serviceSetupElement,
        getInstanceCount(), getInstanceUnitType(), newInstanceData, oldInstanceData);
    return awsAmiDeployStateExecutionData;
  }

  protected AwsAmiDeployStateExecutionData prepareStateExecutionData(String activityId,
      AmiServiceSetupElement serviceSetupElement, int instanceCount, InstanceUnitType instanceUnitType,
      List<ContainerServiceData> newInstanceData, List<ContainerServiceData> oldInstanceData) {
    AwsAmiDeployStateExecutionData awsAmiDeployStateExecutionData =
        AwsAmiDeployStateExecutionData.builder().activityId(activityId).commandName(getCommandName()).build();
    awsAmiDeployStateExecutionData.setAutoScalingSteadyStateTimeout(
        serviceSetupElement.getAutoScalingSteadyStateTimeout());
    awsAmiDeployStateExecutionData.setNewAutoScalingGroupName(serviceSetupElement.getNewAutoScalingGroupName());
    awsAmiDeployStateExecutionData.setOldAutoScalingGroupName(serviceSetupElement.getOldAutoScalingGroupName());
    awsAmiDeployStateExecutionData.setMaxInstances(serviceSetupElement.getMaxInstances());
    awsAmiDeployStateExecutionData.setResizeStrategy(serviceSetupElement.getResizeStrategy());

    awsAmiDeployStateExecutionData.setInstanceCount(instanceCount);
    awsAmiDeployStateExecutionData.setInstanceUnitType(instanceUnitType);

    awsAmiDeployStateExecutionData.setNewInstanceData(newInstanceData);
    awsAmiDeployStateExecutionData.setOldInstanceData(oldInstanceData);

    return awsAmiDeployStateExecutionData;
  }

  protected AwsAmiDeployStateExecutionData prepareStateExecutionDataRollback(
      String activityId, AmiServiceSetupElement serviceSetupElement) {
    AwsAmiDeployStateExecutionData awsAmiDeployStateExecutionData =
        AwsAmiDeployStateExecutionData.builder().activityId(activityId).commandName(getCommandName()).build();
    awsAmiDeployStateExecutionData.setAutoScalingSteadyStateTimeout(
        serviceSetupElement.getAutoScalingSteadyStateTimeout());
    awsAmiDeployStateExecutionData.setNewAutoScalingGroupName(serviceSetupElement.getNewAutoScalingGroupName());
    awsAmiDeployStateExecutionData.setOldAutoScalingGroupName(serviceSetupElement.getOldAutoScalingGroupName());
    awsAmiDeployStateExecutionData.setMaxInstances(serviceSetupElement.getMaxInstances());
    awsAmiDeployStateExecutionData.setResizeStrategy(serviceSetupElement.getResizeStrategy());
    return awsAmiDeployStateExecutionData;
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);

    AwsAmiDeployStateExecutionData awsAmiDeployStateExecutionData =
        (AwsAmiDeployStateExecutionData) context.getStateExecutionData();

    String appId = context.getAppId();

    Activity activity = activityService.get(awsAmiDeployStateExecutionData.getActivityId(), appId);
    Validator.notNullCheck("Activity", activity);

    AwsAmiInfrastructureMapping infrastructureMapping =
        (AwsAmiInfrastructureMapping) infrastructureMappingService.get(appId, phaseElement.getInfraMappingId());
    SettingAttribute cloudProviderSetting = settingsService.get(infrastructureMapping.getComputeProviderSettingId());

    String region = infrastructureMapping.getRegion();
    AwsConfig awsConfig = (AwsConfig) cloudProviderSetting.getValue();

    List<EncryptedDataDetail> encryptionDetails =
        secretManager.getEncryptionDetails((Encryptable) awsConfig, appId, context.getWorkflowExecutionId());

    AmiServiceSetupElement serviceSetupElement = context.getContextElement(ContextElementType.AMI_SERVICE_SETUP);

    Builder logBuilder = aLog()
                             .withAppId(activity.getAppId())
                             .withActivityId(activity.getUuid())
                             .withLogLevel(LogLevel.INFO)
                             .withCommandUnitName(getCommandName())
                             .withExecutionResult(CommandExecutionStatus.RUNNING);

    ManagerExecutionLogCallback executionLogCallback =
        new ManagerExecutionLogCallback(logService, logBuilder, activity.getUuid());

    InstanceElementListParam instanceElementListParam = InstanceElementListParamBuilder.anInstanceElementListParam()
                                                            .withInstanceElements(Collections.emptyList())
                                                            .build();
    ExecutionStatus executionStatus = ExecutionStatus.SUCCESS;
    String errorMessage = null;
    try {
      List<InstanceElement> instanceElements =
          handleAsyncInternal(context, region, awsConfig, encryptionDetails, serviceSetupElement, executionLogCallback);

      List<InstanceStatusSummary> instanceStatusSummaries =
          instanceElements.stream()
              .map(instanceElement
                  -> anInstanceStatusSummary()
                         .withInstanceElement((InstanceElement) instanceElement.cloneMin())
                         .withStatus(ExecutionStatus.SUCCESS)
                         .build())
              .collect(toList());

      awsAmiDeployStateExecutionData.setNewInstanceStatusSummaries(instanceStatusSummaries);
      instanceElementListParam.setInstanceElements(instanceElements);
    } catch (Exception ex) {
      logger.error("Ami deploy step failed with error ", ex);
      executionStatus = ExecutionStatus.FAILED;
      errorMessage = ex.getMessage();
      executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR);
    }

    activityService.updateStatus(activity.getUuid(), activity.getAppId(), executionStatus);

    executionLogCallback.saveExecutionLog(
        format("AutoScaling Group resize operation completed with status:[%s]", executionStatus),
        ExecutionStatus.SUCCESS.equals(executionStatus) ? LogLevel.INFO : LogLevel.ERROR,
        ExecutionStatus.SUCCESS.equals(executionStatus) ? CommandExecutionStatus.SUCCESS
                                                        : CommandExecutionStatus.FAILURE);

    return anExecutionResponse()
        .withStateExecutionData(awsAmiDeployStateExecutionData)
        .withExecutionStatus(executionStatus)
        .withErrorMessage(errorMessage)
        .addContextElement(instanceElementListParam)
        .addNotifyElement(instanceElementListParam)
        .build();
  }

  protected List<InstanceElement> handleAsyncInternal(ExecutionContext context, String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, AmiServiceSetupElement serviceSetupElement,
      ManagerExecutionLogCallback executionLogCallback) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);

    AwsAmiDeployStateExecutionData awsAmiDeployStateExecutionData =
        (AwsAmiDeployStateExecutionData) context.getStateExecutionData();

    Application app = workflowStandardParams.getApp();

    Activity activity = activityService.get(awsAmiDeployStateExecutionData.getActivityId(), app.getUuid());
    Validator.notNullCheck("Activity", activity);

    String serviceId = phaseElement.getServiceElement().getUuid();
    Environment env = workflowStandardParams.getEnv();
    Service service = serviceResourceService.get(app.getUuid(), serviceId);
    Key<ServiceTemplate> serviceTemplateKey =
        serviceTemplateService.getTemplateRefKeysByService(app.getUuid(), serviceId, env.getUuid()).get(0);

    AwsAmiInfrastructureMapping infrastructureMapping = (AwsAmiInfrastructureMapping) infrastructureMappingService.get(
        activity.getAppId(), phaseElement.getInfraMappingId());

    Artifact artifact = ((DeploymentExecutionContext) context).getArtifactForService(serviceId);
    if (artifact == null) {
      throw new StateExecutionException(format("Unable to find artifact for service %s", service.getName()));
    }

    boolean resizeNewFirst = serviceSetupElement.getResizeStrategy().equals(ResizeStrategy.RESIZE_NEW_FIRST);
    ContainerServiceData oldContainerServiceData = awsAmiDeployStateExecutionData.getOldInstanceData().get(0);
    ContainerServiceData newContainerServiceData = awsAmiDeployStateExecutionData.getNewInstanceData().get(0);

    Set<String> existingInstanceIds = new HashSet<>(awsHelperService.listInstanceIdsFromAutoScalingGroup(
        awsConfig, encryptionDetails, region, newContainerServiceData.getName()));

    resizeAsgs(region, awsConfig, encryptionDetails, newContainerServiceData.getName(),
        newContainerServiceData.getDesiredCount(), oldContainerServiceData.getName(),
        oldContainerServiceData.getDesiredCount(), executionLogCallback, resizeNewFirst,
        serviceSetupElement.getAutoScalingSteadyStateTimeout());

    DescribeInstancesResult describeInstancesResult = awsHelperService.describeAutoScalingGroupInstances(
        awsConfig, encryptionDetails, region, newContainerServiceData.getName());
    List<InstanceElement> instanceElements =
        describeInstancesResult.getReservations()
            .stream()
            .flatMap(reservation -> reservation.getInstances().stream())
            .filter(instance -> !existingInstanceIds.contains(instance.getInstanceId()))
            .map(instance -> {
              HostElement hostElement = aHostElement()
                                            .withPublicDns(instance.getPublicDnsName())
                                            .withEc2Instance(instance)
                                            .withInstanceId(instance.getInstanceId())
                                            .build();

              final Map<String, Object> contextMap = context.asMap();
              contextMap.put("host", hostElement);
              String hostName = awsHelperService.getHostnameFromConvention(contextMap, "");
              hostElement.setHostName(hostName);
              return anInstanceElement()
                  .withUuid(instance.getInstanceId())
                  .withHostName(hostName)
                  .withDisplayName(instance.getPublicDnsName())
                  .withHost(hostElement)
                  .withServiceTemplateElement(aServiceTemplateElement()
                                                  .withUuid(serviceTemplateKey.getId().toString())
                                                  .withServiceElement(phaseElement.getServiceElement())
                                                  .build())
                  .build();
            })
            .collect(toList());

    int instancesAdded = newContainerServiceData.getDesiredCount() - newContainerServiceData.getPreviousCount();
    if (instancesAdded > 0 && instancesAdded < instanceElements.size()) {
      instanceElements = instanceElements.subList(0, instancesAdded); // Ignore old instances recycled
    }
    return instanceElements;
  }

  protected void resizeAsgs(String region, AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String newAutoScalingGroupName, Integer newAsgFinalDesiredCount, String oldAutoScalingGroupName,
      Integer oldAsgFinalDesiredCount, ManagerExecutionLogCallback executionLogCallback, boolean resizeNewFirst,
      Integer autoScalingSteadyStateTimeout) {
    if (isBlank(newAutoScalingGroupName) && isBlank(oldAutoScalingGroupName)) {
      throw new InvalidRequestException("At least one AutoScaling Group must be present");
    }
    if (resizeNewFirst) {
      if (isNotBlank(newAutoScalingGroupName)) {
        executionLogCallback.saveExecutionLog(format("Upscale AutoScaling Group [%s]", newAutoScalingGroupName));
        awsHelperService.setAutoScalingGroupCapacityAndWaitForInstancesReadyState(awsConfig, encryptionDetails, region,
            newAutoScalingGroupName, newAsgFinalDesiredCount, executionLogCallback, autoScalingSteadyStateTimeout);
      }
      if (isNotBlank(oldAutoScalingGroupName)) {
        executionLogCallback.saveExecutionLog(format("Downscale AutoScaling Group [%s]", oldAutoScalingGroupName));
        awsHelperService.setAutoScalingGroupCapacityAndWaitForInstancesReadyState(awsConfig, encryptionDetails, region,
            oldAutoScalingGroupName, oldAsgFinalDesiredCount, executionLogCallback);
      }
    } else {
      if (isNotBlank(oldAutoScalingGroupName)) {
        executionLogCallback.saveExecutionLog(format("Downscale AutoScaling Group [%s]", oldAutoScalingGroupName));
        awsHelperService.setAutoScalingGroupCapacityAndWaitForInstancesReadyState(awsConfig, encryptionDetails, region,
            oldAutoScalingGroupName, oldAsgFinalDesiredCount, executionLogCallback);
      }

      if (isNotBlank(newAutoScalingGroupName)) {
        executionLogCallback.saveExecutionLog(format("Upscale AutoScaling Group [%s]", newAutoScalingGroupName));
        awsHelperService.setAutoScalingGroupCapacityAndWaitForInstancesReadyState(awsConfig, encryptionDetails, region,
            newAutoScalingGroupName, newAsgFinalDesiredCount, executionLogCallback);
      }
    }
  }

  @Override
  public Map<String, String> validateFields() {
    Map<String, String> invalidFields = new HashMap<>();
    if (!isRollback() && getInstanceCount() == 0) {
      invalidFields.put("instanceCount", "Instance count must be greater than 0");
    }
    if (getCommandName() == null) {
      invalidFields.put("commandName", "Command name must not be null");
    }
    return invalidFields;
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  /**
   * Gets instance count.
   *
   * @return the instance count
   */
  public int getInstanceCount() {
    return instanceCount;
  }

  /**
   * Sets instance count.
   *
   * @param instanceCount the instance count
   */
  public void setInstanceCount(int instanceCount) {
    this.instanceCount = instanceCount;
  }

  /**
   * Gets instance unit type.
   *
   * @return the instance unit type
   */
  public InstanceUnitType getInstanceUnitType() {
    return instanceUnitType;
  }

  /**
   * Sets instance unit type.
   *
   * @param instanceUnitType the instance unit type
   */
  public void setInstanceUnitType(InstanceUnitType instanceUnitType) {
    this.instanceUnitType = instanceUnitType;
  }

  /**
   * Gets command name.
   *
   * @return the command name
   */
  public String getCommandName() {
    return commandName;
  }

  /**
   * Sets command name.
   *
   * @param commandName the command name
   */
  public void setCommandName(String commandName) {
    this.commandName = commandName;
  }
}
