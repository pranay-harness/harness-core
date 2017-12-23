package software.wings.sm.states;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.github.reinert.jjschema.Attributes;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.Encryptable;
import software.wings.api.*;
import software.wings.beans.*;
import software.wings.beans.Activity.Type;
import software.wings.beans.Log.Builder;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.container.UserDataSpecification;
import software.wings.common.Constants;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.*;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.*;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.utils.Validator;
import software.wings.waitnotify.NotifyResponseData;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static software.wings.api.ContainerServiceData.ContainerServiceDataBuilder.aContainerServiceData;
import static software.wings.beans.InstanceUnitType.PERCENTAGE;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.waitnotify.StringNotifyResponseData.Builder.aStringNotifyResponseData;

/**
 * Created by anubhaw on 12/19/17.
 */
public class AwsAmiServiceDeployState extends State {
  private static final Logger logger = LoggerFactory.getLogger(AwsAmiServiceDeployState.class);

  @Attributes(title = "Desired Instances (cumulative)") private int instanceCount;

  @Attributes(title = "Instance Unit Type (Count/Percent)")
  @EnumData(enumDataProvider = InstanceUnitTypeDataProvider.class)
  @DefaultValue("COUNT")
  private InstanceUnitType instanceUnitType = InstanceUnitType.COUNT;

  @Attributes(title = "Command")
  @EnumData(enumDataProvider = CommandStateEnumDataProvider.class)
  @DefaultValue("Amazon AMI")
  private String commandName = "Amazon AMI";

  @Inject @Transient private transient AwsHelperService awsHelperService;
  @Inject @Transient protected transient SettingsService settingsService;
  @Inject @Transient protected transient ServiceResourceService serviceResourceService;
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
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String serviceId = phaseElement.getServiceElement().getUuid();

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    Application app = workflowStandardParams.getApp();
    Environment env = workflowStandardParams.getEnv();
    Service service = serviceResourceService.get(app.getUuid(), serviceId);

    Artifact artifact = ((DeploymentExecutionContext) context).getArtifactForService(serviceId);
    if (artifact == null) {
      throw new StateExecutionException(String.format("Unable to find artifact for service %s", service.getName()));
    }
    ArtifactStream artifactStream = artifactStreamService.get(artifact.getAppId(), artifact.getArtifactStreamId());

    Command command =
        serviceResourceService.getCommandByName(app.getUuid(), serviceId, env.getUuid(), getCommandName()).getCommand();
    List<CommandUnit> commandUnitList =
        serviceResourceService.getFlattenCommandUnitList(app.getUuid(), serviceId, env.getUuid(), commandName);
    Activity.ActivityBuilder activityBuilder = Activity.builder()
                                                   .applicationName(app.getName())
                                                   .environmentId(env.getUuid())
                                                   .environmentName(env.getName())
                                                   .environmentType(env.getEnvironmentType())
                                                   .serviceId(service.getUuid())
                                                   .serviceName(service.getName())
                                                   .commandName(commandName)
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
    Activity activity = activityService.save(build);

    Builder logBuilder = aLog()
                             .withAppId(activity.getAppId())
                             .withActivityId(activity.getUuid())
                             .withLogLevel(LogLevel.INFO)
                             .withCommandUnitName(commandUnitList.get(0).getName())
                             .withExecutionResult(CommandExecutionStatus.RUNNING);

    executorService.schedule(new SimpleNotifier(waitNotifyEngine, activity.getUuid(),
                                 aStringNotifyResponseData().withData(activity.getUuid()).build()),
        5, TimeUnit.SECONDS);
    return anExecutionResponse()
        .withAsync(true)
        .withStateExecutionData(
            AwsAmiDeployStateExecutionData.builder().activityId(activity.getUuid()).commandName(commandName).build())
        .withExecutionStatus(ExecutionStatus.SUCCESS)
        .addCorrelationIds(activity.getUuid())
        .build();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
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

    Artifact artifact = ((DeploymentExecutionContext) context).getArtifactForService(serviceId);
    if (artifact == null) {
      throw new StateExecutionException(String.format("Unable to find artifact for service %s", service.getName()));
    }
    ArtifactStream artifactStream = artifactStreamService.get(artifact.getAppId(), artifact.getArtifactStreamId());

    Command command =
        serviceResourceService.getCommandByName(app.getUuid(), serviceId, env.getUuid(), getCommandName()).getCommand();
    List<CommandUnit> commandUnitList =
        serviceResourceService.getFlattenCommandUnitList(app.getUuid(), serviceId, env.getUuid(), commandName);

    Builder logBuilder = aLog()
                             .withAppId(activity.getAppId())
                             .withActivityId(activity.getUuid())
                             .withLogLevel(LogLevel.INFO)
                             .withCommandUnitName(commandUnitList.get(0).getName())
                             .withExecutionResult(CommandExecutionStatus.RUNNING);

    UserDataSpecification userDataSpecification =
        serviceResourceService.getUserDataSpecification(app.getUuid(), serviceId);

    AwsAmiInfrastructureMapping infrastructureMapping =
        (AwsAmiInfrastructureMapping) infrastructureMappingService.get(app.getUuid(), phaseElement.getInfraMappingId());
    SettingAttribute cloudProviderSetting = settingsService.get(infrastructureMapping.getComputeProviderSettingId());

    String region = infrastructureMapping.getRegion();
    AwsConfig awsConfig = (AwsConfig) cloudProviderSetting.getValue();

    List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(
        (Encryptable) awsConfig, context.getAppId(), context.getWorkflowExecutionId());

    AmiServiceSetupElement serviceSetupElement = context.getContextElement(ContextElementType.AMI_SERVICE_SETUP);

    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback(logService, logBuilder, activity.getUuid());

    if (isRollback()) {
      AmiServiceDeployElement amiServiceDeployElement =
          context.getContextElement(ContextElementType.AMI_SERVICE_DEPLOY);
      // TODO: old and new both should be present with 1 element atleast
      ContainerServiceData oldContainerServiceData = amiServiceDeployElement.getOldInstanceData().get(0);
      ContainerServiceData newContainerServiceData = amiServiceDeployElement.getNewInstanceData().get(0);
      resizeAsgs(region, awsConfig, encryptionDetails, oldContainerServiceData.getName(),
          oldContainerServiceData.getPreviousCount(), newContainerServiceData.getName(),
          newContainerServiceData.getPreviousCount(), executionLogCallback,
          serviceSetupElement.getResizeStrategy() == ResizeStrategy.RESIZE_NEW_FIRST);

    } else {
      awsAmiDeployStateExecutionData.setInstanceCount(instanceCount);
      awsAmiDeployStateExecutionData.setInstanceUnitType(instanceUnitType);
      awsAmiDeployStateExecutionData.setActivityId(activity.getUuid());
      awsAmiDeployStateExecutionData.setAutoScalingSteadyStateTimeout(
          serviceSetupElement.getAutoScalingSteadyStateTimeout());
      awsAmiDeployStateExecutionData.setNewAutoScalingGroupName(serviceSetupElement.getNewAutoScalingGroupName());
      awsAmiDeployStateExecutionData.setOldAutoScalingGroupName(serviceSetupElement.getOldAutoScalingGroupName());
      awsAmiDeployStateExecutionData.setMaxInstances(serviceSetupElement.getMaxInstances());
      awsAmiDeployStateExecutionData.setResizeStrategy(serviceSetupElement.getResizeStrategy());
      String newAutoScalingGroupName = serviceSetupElement.getNewAutoScalingGroupName();
      String oldAutoScalingGroupName = serviceSetupElement.getOldAutoScalingGroupName();

      Integer totalExpectedCount;
      if (getInstanceUnitType() == PERCENTAGE) {
        int percent = Math.min(getInstanceCount(), 100);
        int instanceCount = (int) Math.round((percent * serviceSetupElement.getMaxInstances()) / 100.0);
        totalExpectedCount = Math.max(instanceCount, 1);
      } else {
        totalExpectedCount = getInstanceCount();
      }

      AutoScalingGroup newAutoScalingGroup = awsHelperService
                                                 .describeAutoScalingGroups(awsConfig, encryptionDetails, region,
                                                     new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(
                                                         Arrays.asList(newAutoScalingGroupName)))
                                                 .getAutoScalingGroups()
                                                 .get(0);

      AutoScalingGroup oldAutoScalingGroup = awsHelperService
                                                 .describeAutoScalingGroups(awsConfig, encryptionDetails, region,
                                                     new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(
                                                         Arrays.asList(oldAutoScalingGroupName)))
                                                 .getAutoScalingGroups()
                                                 .get(0);

      Integer newAutoScalingGroupDesiredCapacity = newAutoScalingGroup.getDesiredCapacity();
      Integer totalNewInstancesToBeAdded = Math.max(0, totalExpectedCount - newAutoScalingGroupDesiredCapacity);
      Integer newAsgFinalDesiredCount = newAutoScalingGroupDesiredCapacity + totalNewInstancesToBeAdded;
      awsAmiDeployStateExecutionData.setNewInstanceData(
          Arrays.asList(aContainerServiceData()
                            .withName(newAutoScalingGroupName)
                            .withDesiredCount(newAsgFinalDesiredCount)
                            .withPreviousCount(newAutoScalingGroupDesiredCapacity)
                            .build()));

      Integer oldAutoScalingGroupDesiredCapacity = oldAutoScalingGroup.getDesiredCapacity();
      Integer totalOldInstancesToBeRemoved =
          Math.max(0, oldAutoScalingGroupDesiredCapacity - totalNewInstancesToBeAdded);
      Integer oldAsgFinalDesiredCount = Math.max(0, oldAutoScalingGroupDesiredCapacity - totalOldInstancesToBeRemoved);

      awsAmiDeployStateExecutionData.setOldInstanceData(
          Arrays.asList(aContainerServiceData()
                            .withName(oldAutoScalingGroupName)
                            .withDesiredCount(oldAsgFinalDesiredCount)
                            .withPreviousCount(oldAutoScalingGroupDesiredCapacity)
                            .build()));

      boolean resizeNewFirst = serviceSetupElement.getResizeStrategy().equals(ResizeStrategy.RESIZE_NEW_FIRST);

      resizeAsgs(region, awsConfig, encryptionDetails, newAutoScalingGroupName, newAsgFinalDesiredCount,
          oldAutoScalingGroupName, oldAsgFinalDesiredCount, executionLogCallback, resizeNewFirst);
    }

    activityService.updateStatus(activity.getUuid(), activity.getAppId(), ExecutionStatus.SUCCESS);

    return anExecutionResponse().withStateExecutionData(awsAmiDeployStateExecutionData).build();
  }

  private void resizeAsgs(String region, AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String newAutoScalingGroupName, Integer newAsgFinalDesiredCount, String oldAutoScalingGroupName,
      Integer oldAsgFinalDesiredCount, ExecutionLogCallback executionLogCallback, boolean resizeNewFirst) {
    if (resizeNewFirst) {
      logger.info("resizeAsgs");
      awsHelperService.setAutoScalingGroupCapacityAndWaitForInstancesReadyState(
          awsConfig, encryptionDetails, region, newAutoScalingGroupName, newAsgFinalDesiredCount, executionLogCallback);
      awsHelperService.setAutoScalingGroupCapacityAndWaitForInstancesReadyState(
          awsConfig, encryptionDetails, region, oldAutoScalingGroupName, oldAsgFinalDesiredCount, executionLogCallback);
    } else {
      awsHelperService.setAutoScalingGroupCapacityAndWaitForInstancesReadyState(
          awsConfig, encryptionDetails, region, oldAutoScalingGroupName, oldAsgFinalDesiredCount, executionLogCallback);
      awsHelperService.setAutoScalingGroupCapacityAndWaitForInstancesReadyState(
          awsConfig, encryptionDetails, region, newAutoScalingGroupName, newAsgFinalDesiredCount, executionLogCallback);
    }
  }

  protected void compouteCounts() {}

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

  public static class ExecutionLogCallback {
    private transient LogService logService;
    private Builder logBuilder;
    private String activityId;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public ExecutionLogCallback() {}

    public ExecutionLogCallback(LogService logService, Builder logBuilder, String activityId) {
      this.logService = logService;
      this.logBuilder = logBuilder;
      this.activityId = activityId;
    }

    public void saveExecutionLog(String line) {
      if (logService != null) {
        Log log = logBuilder.but().withLogLine(line).build();
        logService.batchedSaveCommandUnitLogs(activityId, log.getCommandUnitName(), log);
      } else {
        logger.warn("No logService injected. Couldn't save log [{}]", line);
      }
    }

    public void setLogService(LogService logService) {
      this.logService = logService;
    }
  }
}
