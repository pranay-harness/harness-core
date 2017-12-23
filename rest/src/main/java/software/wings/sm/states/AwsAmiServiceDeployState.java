package software.wings.sm.states;

import static software.wings.beans.InstanceUnitType.PERCENTAGE;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.waitnotify.StringNotifyResponseData.Builder.aStringNotifyResponseData;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.github.reinert.jjschema.Attributes;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.Encryptable;
import software.wings.api.AmiServiceDeployElement;
import software.wings.api.AmiServiceSetupElement;
import software.wings.api.AwsAmiDeployStateExecutionData;
import software.wings.api.PhaseElement;
import software.wings.beans.Activity;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.AwsConfig;
import software.wings.beans.DeploymentExecutionContext;
import software.wings.beans.Environment;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.Log;
import software.wings.beans.Log.Builder;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.container.UserDataSpecification;
import software.wings.common.Constants;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.LogService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateExecutionException;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
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

/**
 * Created by anubhaw on 12/19/17.
 */
public class AwsAmiServiceDeployState extends State {
  @Attributes(title = "Desired Instances (cumulative)") private int instanceCount;

  @Attributes(title = "Instance Unit Type (Count/Percent)")
  @EnumData(enumDataProvider = InstanceUnitTypeDataProvider.class)
  @DefaultValue("COUNT")
  private InstanceUnitType instanceUnitType = InstanceUnitType.COUNT;

  @Attributes(title = "Command")
  @EnumData(enumDataProvider = CommandStateEnumDataProvider.class)
  @DefaultValue("Amazon AMI")
  private String commandName;

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

    String newAutoScalingGroupName = serviceSetupElement.getNewAutoScalingGroupName();
    String oldAutoScalingGroupName = serviceSetupElement.getOldAutoScalingGroupName();

    Integer totalExpectedCount;
    if (getInstanceUnitType() == PERCENTAGE) {
      int percent = Math.min(getInstanceCount(), 100);
      int instanceCount = Long.valueOf(Math.round(percent * getInstanceCount() / 100.0)).intValue();
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

    Integer oldAutoScalingGroupDesiredCapacity = oldAutoScalingGroup.getDesiredCapacity();
    Integer totalOldInstancesToBeRemoved = Math.max(0, oldAutoScalingGroupDesiredCapacity - totalNewInstancesToBeAdded);
    Integer oldAsgFinalDesiredCount = Math.max(0, oldAutoScalingGroupDesiredCapacity - totalOldInstancesToBeRemoved);

    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback(logService, logBuilder, activity.getUuid());
    //
    //    if (serviceSetupElement.getResizeStrategy().equals(ResizeStrategy.RESIZE_NEW_FIRST)) {
    //      awsHelperService.setAutoScalingGroupCapacityAndWaitForInstancesReadyState(awsConfig, encryptionDetails,
    //      region,
    //          newAutoScalingGroupName, totalNewInstancesToBeAdded, executionLogCallback);
    //      awsHelperService.setAutoScalingGroupCapacityAndWaitForInstancesReadyState(awsConfig, encryptionDetails,
    //      region,
    //          oldAutoScalingGroupName, totalOldInstancesToBeRemoved, executionLogCallback);
    //    } else {
    //      awsHelperService.setAutoScalingGroupCapacityAndWaitForInstancesReadyState(awsConfig, encryptionDetails,
    //      region,
    //          oldAutoScalingGroupName, totalOldInstancesToBeRemoved, executionLogCallback);
    //      awsHelperService.setAutoScalingGroupCapacityAndWaitForInstancesReadyState(awsConfig, encryptionDetails,
    //      region,
    //          newAutoScalingGroupName, totalNewInstancesToBeAdded, executionLogCallback);
    //    }
    //
    //    activityService.updateStatus(activity.getUuid(), activity.getAppId(), ExecutionStatus.SUCCESS);
    AmiServiceDeployElement amiServiceDeployElement =
        AmiServiceDeployElement.builder().activityId(activity.getUuid()).commandName(commandName).build();
    return anExecutionResponse()
        .withAsync(false)
        .withStateExecutionData(awsAmiDeployStateExecutionData)
        .withExecutionStatus(ExecutionStatus.FAILED)
        .addContextElement(amiServiceDeployElement)
        .addNotifyElement(amiServiceDeployElement)
        .build();
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
