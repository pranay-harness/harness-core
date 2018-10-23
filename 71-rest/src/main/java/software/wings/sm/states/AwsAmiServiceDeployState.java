package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static software.wings.api.HostElement.Builder.aHostElement;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.api.ServiceTemplateElement.Builder.aServiceTemplateElement;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.InstanceUnitType.PERCENTAGE;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.TaskType.AWS_AMI_ASYNC_TASK;
import static software.wings.beans.infrastructure.Host.Builder.aHost;
import static software.wings.common.Constants.ASG_COMMAND_NAME;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.InstanceStatusSummary.InstanceStatusSummaryBuilder.anInstanceStatusSummary;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.amazonaws.services.ec2.model.Instance;
import com.github.reinert.jjschema.Attributes;
import io.harness.delegate.task.protocol.ResponseData;
import io.harness.exception.WingsException;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import software.wings.beans.DelegateTask;
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
import software.wings.beans.infrastructure.Host;
import software.wings.common.Constants;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.impl.AwsUtils;
import software.wings.service.impl.aws.model.AwsAmiPreDeploymentData;
import software.wings.service.impl.aws.model.AwsAmiResizeData;
import software.wings.service.impl.aws.model.AwsAmiServiceDeployRequest;
import software.wings.service.impl.aws.model.AwsAmiServiceDeployResponse;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.LogService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.aws.manager.AwsAsgHelperServiceManager;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManager;
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
import software.wings.utils.Misc;
import software.wings.utils.Validator;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

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
  @Inject @Transient private transient HostService hostService;
  @Inject @Transient private transient AwsUtils awsUtils;
  @Inject @Transient private transient AwsAsgHelperServiceManager awsAsgHelperServiceManager;
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
      throw new WingsException(format("Unable to find artifact for service %s", service.getName()));
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
    List<String> oldAsgNames = serviceSetupElement.getOldAsgNames();
    List<String> gpNames = Lists.newArrayList();
    if (isNotEmpty(oldAsgNames)) {
      gpNames.addAll(oldAsgNames);
    }
    String newAutoScalingGroupName = serviceSetupElement.getNewAutoScalingGroupName();
    if (isNotEmpty(newAutoScalingGroupName)) {
      gpNames.add(newAutoScalingGroupName);
    }

    Map<String, Integer> existingDesiredCapacities =
        awsAsgHelperServiceManager.getDesiredCapacitiesOfAsgs(awsConfig, encryptionDetails, region, gpNames);

    Integer newAutoScalingGroupDesiredCapacity =
        isNotEmpty(newAutoScalingGroupName) ? existingDesiredCapacities.get(newAutoScalingGroupName) : 0;
    Integer totalNewInstancesToBeAdded = Math.max(0, totalExpectedCount - newAutoScalingGroupDesiredCapacity);
    Integer newAsgFinalDesiredCount = newAutoScalingGroupDesiredCapacity + totalNewInstancesToBeAdded;
    List<ContainerServiceData> newInstanceData = singletonList(ContainerServiceData.builder()
                                                                   .name(newAutoScalingGroupName)
                                                                   .desiredCount(newAsgFinalDesiredCount)
                                                                   .previousCount(newAutoScalingGroupDesiredCapacity)
                                                                   .build());

    List<AwsAmiResizeData> newDesiredCapacities = getNewDesiredCounts(
        totalNewInstancesToBeAdded, serviceSetupElement.getOldAsgNames(), existingDesiredCapacities);

    List<ContainerServiceData> oldInstanceData = Lists.newArrayList();
    if (isNotEmpty(newDesiredCapacities)) {
      newDesiredCapacities.forEach(newDesiredCapacity -> {
        String asgName = newDesiredCapacity.getAsgName();
        int newCount = newDesiredCapacity.getDesiredCount();
        Integer oldCount = existingDesiredCapacities.get(asgName);
        if (oldCount == null) {
          oldCount = 0;
        }
        oldInstanceData.add(
            ContainerServiceData.builder().name(asgName).desiredCount(newCount).previousCount(oldCount).build());
      });
    }

    awsAmiDeployStateExecutionData = prepareStateExecutionData(activity.getUuid(), serviceSetupElement,
        getInstanceCount(), getInstanceUnitType(), newInstanceData, oldInstanceData);
    boolean resizeNewFirst = serviceSetupElement.getResizeStrategy().equals(ResizeStrategy.RESIZE_NEW_FIRST);

    createAndQueueResizeTask(awsConfig, encryptionDetails, region, infrastructureMapping.getAccountId(),
        infrastructureMapping.getAppId(), activity.getUuid(), getCommandName(), resizeNewFirst, newAutoScalingGroupName,
        newAsgFinalDesiredCount, newDesiredCapacities, serviceSetupElement.getAutoScalingSteadyStateTimeout(),
        infrastructureMapping.getEnvId(), serviceSetupElement.getMinInstances(), serviceSetupElement.getMaxInstances(),
        serviceSetupElement.getPreDeploymentData());
    return awsAmiDeployStateExecutionData;
  }

  private List<AwsAmiResizeData> getNewDesiredCounts(
      int instancesToBeAdded, List<String> oldAsgNames, Map<String, Integer> existingDesiredCapacities) {
    int n = instancesToBeAdded;
    List<AwsAmiResizeData> desiredCapacities = Lists.newArrayList();
    if (isNotEmpty(oldAsgNames)) {
      for (String oldAsgName : oldAsgNames) {
        Integer n1 = existingDesiredCapacities.get(oldAsgName);
        if (n1 == null) {
          n1 = 0;
        }
        if (n1 <= n) {
          n -= n1;
          n1 = 0;
        } else {
          n1 -= n;
          n = 0;
        }
        desiredCapacities.add(AwsAmiResizeData.builder().asgName(oldAsgName).desiredCount(n1).build());
      }
    }
    return desiredCapacities;
  }

  protected void createAndQueueResizeTask(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, String accountId, String appId, String activityId, String commandName, boolean resizeNewFirst,
      String newAutoScalingGroupName, Integer newAsgFinalDesiredCount, List<AwsAmiResizeData> resizeData,
      Integer autoScalingSteadyStateTimeout, String envId, int minInstaces, int maxInstances,
      AwsAmiPreDeploymentData preDeploymentData) {
    AwsAmiServiceDeployRequest request = AwsAmiServiceDeployRequest.builder()
                                             .awsConfig(awsConfig)
                                             .encryptionDetails(encryptionDetails)
                                             .region(region)
                                             .accountId(accountId)
                                             .appId(appId)
                                             .activityId(activityId)
                                             .commandName(commandName)
                                             .resizeNewFirst(resizeNewFirst)
                                             .newAutoScalingGroupName(newAutoScalingGroupName)
                                             .newAsgFinalDesiredCount(newAsgFinalDesiredCount)
                                             .autoScalingSteadyStateTimeout(autoScalingSteadyStateTimeout)
                                             .minInstances(minInstaces)
                                             .maxInstances(maxInstances)
                                             .preDeploymentData(preDeploymentData)
                                             .asgDesiredCounts(resizeData)
                                             .build();
    DelegateTask delegateTask = aDelegateTask()
                                    .withAccountId(accountId)
                                    .withAppId(appId)
                                    .withWaitId(activityId)
                                    .withParameters(new Object[] {request})
                                    .withTaskType(AWS_AMI_ASYNC_TASK)
                                    .withAsync(true)
                                    .withEnvId(envId)
                                    .build();
    delegateService.queueTask(delegateTask);
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
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    AwsAmiDeployStateExecutionData awsAmiDeployStateExecutionData =
        (AwsAmiDeployStateExecutionData) context.getStateExecutionData();

    String appId = context.getAppId();

    AwsAmiServiceDeployResponse amiServiceDeployResponse =
        (AwsAmiServiceDeployResponse) response.values().iterator().next();

    awsAmiDeployStateExecutionData.setDelegateMetaInfo(amiServiceDeployResponse.getDelegateMetaInfo());
    Activity activity = activityService.get(awsAmiDeployStateExecutionData.getActivityId(), appId);
    Validator.notNullCheck("Activity", activity);

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
    ExecutionStatus executionStatus = amiServiceDeployResponse.getExecutionStatus();
    String errorMessage = null;
    try {
      List<InstanceElement> instanceElements =
          handleAsyncInternal(amiServiceDeployResponse, context, serviceSetupElement, executionLogCallback);

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
      errorMessage = Misc.getMessage(ex);
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

  protected List<InstanceElement> handleAsyncInternal(AwsAmiServiceDeployResponse amiServiceDeployResponse,
      ExecutionContext context, AmiServiceSetupElement serviceSetupElement,
      ManagerExecutionLogCallback executionLogCallback) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);

    AwsAmiDeployStateExecutionData awsAmiDeployStateExecutionData =
        (AwsAmiDeployStateExecutionData) context.getStateExecutionData();

    Application app = workflowStandardParams.getApp();

    Activity activity = activityService.get(awsAmiDeployStateExecutionData.getActivityId(), app.getUuid());
    Validator.notNullCheck("Activity", activity);

    AwsAmiInfrastructureMapping infrastructureMapping = (AwsAmiInfrastructureMapping) infrastructureMappingService.get(
        activity.getAppId(), phaseElement.getInfraMappingId());

    String serviceId = phaseElement.getServiceElement().getUuid();
    Environment env = workflowStandardParams.getEnv();
    Service service = serviceResourceService.get(app.getUuid(), serviceId);
    Key<ServiceTemplate> serviceTemplateKey =
        serviceTemplateService.getTemplateRefKeysByService(app.getUuid(), serviceId, env.getUuid()).get(0);

    Artifact artifact = ((DeploymentExecutionContext) context).getArtifactForService(serviceId);
    if (artifact == null) {
      throw new WingsException(format("Unable to find artifact for service %s", service.getName()));
    }

    ContainerServiceData newContainerServiceData = awsAmiDeployStateExecutionData.getNewInstanceData().get(0);

    List<Instance> ec2InstancesAdded = amiServiceDeployResponse.getInstancesAdded();
    List<InstanceElement> instanceElements = emptyList();
    if (isNotEmpty(ec2InstancesAdded)) {
      instanceElements =
          amiServiceDeployResponse.getInstancesAdded()
              .stream()
              .map(instance -> {
                Host host = aHost()
                                .withHostName(awsUtils.getHostnameFromPrivateDnsName(instance.getPrivateDnsName()))
                                .withPublicDns(instance.getPublicDnsName())
                                .withEc2Instance(instance)
                                .withAppId(infrastructureMapping.getAppId())
                                .withEnvId(infrastructureMapping.getEnvId())
                                .withHostConnAttr(infrastructureMapping.getHostConnectionAttrs())
                                .withInfraMappingId(infrastructureMapping.getUuid())
                                .withServiceTemplateId(infrastructureMapping.getServiceTemplateId())
                                .build();
                Host savedHost = hostService.saveHost(host);
                HostElement hostElement = aHostElement()
                                              .withUuid(savedHost.getUuid())
                                              .withPublicDns(instance.getPublicDnsName())
                                              .withIp(instance.getPrivateIpAddress())
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
    }

    int instancesAdded = newContainerServiceData.getDesiredCount() - newContainerServiceData.getPreviousCount();
    if (instancesAdded > 0 && instancesAdded < instanceElements.size()) {
      instanceElements = instanceElements.subList(0, instancesAdded); // Ignore old instances recycled
    }
    return instanceElements;
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
