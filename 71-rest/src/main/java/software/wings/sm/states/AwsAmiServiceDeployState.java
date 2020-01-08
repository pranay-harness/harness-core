package software.wings.sm.states;

import static com.google.common.collect.Lists.newArrayList;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.api.ServiceTemplateElement.Builder.aServiceTemplateElement;
import static software.wings.beans.InstanceUnitType.PERCENTAGE;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.TaskType.AWS_AMI_ASYNC_TASK;
import static software.wings.beans.infrastructure.Host.Builder.aHost;
import static software.wings.common.Constants.ASG_COMMAND_NAME;
import static software.wings.sm.InstanceStatusSummary.InstanceStatusSummaryBuilder.anInstanceStatusSummary;

import com.google.inject.Inject;

import com.amazonaws.services.ec2.model.Instance;
import com.github.reinert.jjschema.Attributes;
import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.TriggeredBy;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.Key;
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
import software.wings.beans.command.CommandUnit;
import software.wings.beans.infrastructure.Host;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.impl.AwsUtils;
import software.wings.service.impl.aws.model.AwsAmiPreDeploymentData;
import software.wings.service.impl.aws.model.AwsAmiResizeData;
import software.wings.service.impl.aws.model.AwsAmiServiceDeployRequest;
import software.wings.service.impl.aws.model.AwsAmiServiceDeployResponse;
import software.wings.service.impl.servicetemplates.ServiceTemplateHelper;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.LogService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.SweepingOutputService;
import software.wings.service.intfc.aws.manager.AwsAsgHelperServiceManager;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class AwsAmiServiceDeployState extends State {
  @Attributes(title = "Desired Instances (cumulative)") private String instanceCount;

  @Attributes(title = "Instance Unit Type (Count/Percent)")
  @EnumData(enumDataProvider = InstanceUnitTypeDataProvider.class)
  @DefaultValue("COUNT")
  private InstanceUnitType instanceUnitType = InstanceUnitType.COUNT;

  @Attributes(title = "Command") @DefaultValue(ASG_COMMAND_NAME) private String commandName = ASG_COMMAND_NAME;

  @Inject protected transient AwsHelperService awsHelperService;
  @Inject protected transient SettingsService settingsService;
  @Inject protected transient ServiceResourceService serviceResourceService;
  @Inject protected transient ServiceTemplateService serviceTemplateService;
  @Inject protected transient InfrastructureMappingService infrastructureMappingService;
  @Inject protected transient ArtifactStreamService artifactStreamService;
  @Inject protected transient SecretManager secretManager;
  @Inject protected transient EncryptionService encryptionService;
  @Inject protected transient ActivityService activityService;
  @Inject protected transient DelegateService delegateService;
  @Inject protected transient LogService logService;
  @Inject protected transient SweepingOutputService sweepingOutputService;
  @Inject private transient HostService hostService;
  @Inject private transient AwsUtils awsUtils;
  @Inject private transient AwsAsgHelperServiceManager awsAsgHelperServiceManager;
  @Inject private transient ServiceTemplateHelper serviceTemplateHelper;

  public AwsAmiServiceDeployState(String name) {
    this(name, StateType.AWS_AMI_SERVICE_DEPLOY.name());
  }

  public AwsAmiServiceDeployState(String name, String type) {
    super(name, type);
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
      return executeInternal(context);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
  }

  protected Activity crateActivity(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM);
    String serviceId = phaseElement.getServiceElement().getUuid();

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    notNullCheck("workflowStandardParams", workflowStandardParams, USER);
    notNullCheck("currentUser", workflowStandardParams.getCurrentUser(), USER);

    Application app = workflowStandardParams.getApp();
    notNullCheck("Application", app);
    Environment env = workflowStandardParams.getEnv();
    notNullCheck("Environment", env);
    Service service = serviceResourceService.getWithDetails(app.getUuid(), serviceId);

    Artifact artifact = ((DeploymentExecutionContext) context).getDefaultArtifactForService(serviceId);
    if (artifact == null) {
      throw new WingsException(format("Unable to find artifact for service %s", service.getName()));
    }
    ArtifactStream artifactStream = artifactStreamService.get(artifact.getArtifactStreamId());

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
                                          .artifactName(artifact.getDisplayName())
                                          .triggeredBy(TriggeredBy.builder()
                                                           .email(workflowStandardParams.getCurrentUser().getEmail())
                                                           .name(workflowStandardParams.getCurrentUser().getName())
                                                           .build());

    Activity build = activityBuilder.build();
    build.setAppId(app.getUuid());
    return activityService.save(build);
  }

  protected ExecutionResponse executeInternal(ExecutionContext context) {
    Activity activity = crateActivity(context);
    AmiServiceSetupElement serviceSetupElement = context.getContextElement(ContextElementType.AMI_SERVICE_SETUP);
    boolean blueGreen = serviceSetupElement.isBlueGreen();

    AwsAmiDeployStateExecutionData awsAmiDeployStateExecutionData;
    AwsAmiInfrastructureMapping infrastructureMapping = (AwsAmiInfrastructureMapping) infrastructureMappingService.get(
        activity.getAppId(), context.fetchInfraMappingId());
    SettingAttribute cloudProviderSetting = settingsService.get(infrastructureMapping.getComputeProviderSettingId());

    String region = infrastructureMapping.getRegion();
    AwsConfig awsConfig = (AwsConfig) cloudProviderSetting.getValue();

    List<EncryptedDataDetail> encryptionDetails =
        secretManager.getEncryptionDetails(awsConfig, context.getAppId(), context.getWorkflowExecutionId());

    Integer totalExpectedCount;
    int instanceCountLocal = Integer.parseInt(context.renderExpression(getInstanceCount()));
    if (getInstanceUnitType() == PERCENTAGE) {
      int percent = Math.min(instanceCountLocal, 100);
      int instanceCount1 = (int) Math.round((percent * serviceSetupElement.getDesiredInstances()) / 100.0);
      totalExpectedCount = Math.max(instanceCount1, 1);
    } else {
      totalExpectedCount = instanceCountLocal;
    }

    List<String> oldAsgNames = serviceSetupElement.getOldAsgNames();
    List<String> gpNames = newArrayList();
    if (isNotEmpty(oldAsgNames) && !blueGreen) {
      gpNames.addAll(oldAsgNames);
    }
    String newAutoScalingGroupName = serviceSetupElement.getNewAutoScalingGroupName();
    if (isNotEmpty(newAutoScalingGroupName)) {
      gpNames.add(newAutoScalingGroupName);
    }

    Map<String, Integer> existingDesiredCapacities = awsAsgHelperServiceManager.getDesiredCapacitiesOfAsgs(
        awsConfig, encryptionDetails, region, gpNames, infrastructureMapping.getAppId());

    Integer newAutoScalingGroupDesiredCapacity =
        isNotEmpty(newAutoScalingGroupName) ? existingDesiredCapacities.get(newAutoScalingGroupName) : 0;
    Integer totalNewInstancesToBeAdded = Math.max(0, totalExpectedCount - newAutoScalingGroupDesiredCapacity);
    Integer newAsgFinalDesiredCount = newAutoScalingGroupDesiredCapacity + totalNewInstancesToBeAdded;
    List<ContainerServiceData> newInstanceData = singletonList(ContainerServiceData.builder()
                                                                   .name(newAutoScalingGroupName)
                                                                   .desiredCount(newAsgFinalDesiredCount)
                                                                   .previousCount(newAutoScalingGroupDesiredCapacity)
                                                                   .build());
    // If the deployment is of B/G type, we will not downscale the old ASGs.
    // For canary Wfs we will downscale old ASGs
    List<AwsAmiResizeData> newDesiredCapacities = emptyList();
    List<ContainerServiceData> oldInstanceData = newArrayList();
    List<String> classicLbs = newArrayList();
    List<String> targetGroupArns = newArrayList();
    if (blueGreen) {
      classicLbs = infrastructureMapping.getStageClassicLoadBalancers();
      targetGroupArns = infrastructureMapping.getStageTargetGroupArns();
    } else {
      newDesiredCapacities = getNewDesiredCounts(
          totalNewInstancesToBeAdded, serviceSetupElement.getOldAsgNames(), existingDesiredCapacities);
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
      classicLbs = infrastructureMapping.getClassicLoadBalancers();
      targetGroupArns = infrastructureMapping.getTargetGroupArns();
    }

    awsAmiDeployStateExecutionData = prepareStateExecutionData(activity.getUuid(), serviceSetupElement,
        instanceCountLocal, getInstanceUnitType(), newInstanceData, oldInstanceData);
    boolean resizeNewFirst = serviceSetupElement.getResizeStrategy() == ResizeStrategy.RESIZE_NEW_FIRST;

    createAndQueueResizeTask(awsConfig, encryptionDetails, region, infrastructureMapping.getAccountId(),
        infrastructureMapping.getAppId(), activity.getUuid(), getCommandName(), resizeNewFirst, newAutoScalingGroupName,
        newAsgFinalDesiredCount, newDesiredCapacities, serviceSetupElement.getAutoScalingSteadyStateTimeout(),
        infrastructureMapping.getEnvId(), serviceSetupElement.getMinInstances(), serviceSetupElement.getMaxInstances(),
        serviceSetupElement.getPreDeploymentData(), classicLbs, targetGroupArns, false,
        serviceSetupElement.getBaseScalingPolicyJSONs(), serviceSetupElement.getDesiredInstances());

    return ExecutionResponse.builder()
        .async(true)
        .stateExecutionData(awsAmiDeployStateExecutionData)
        .executionStatus(ExecutionStatus.SUCCESS)
        .correlationId(activity.getUuid())
        .build();
  }

  private List<AwsAmiResizeData> getNewDesiredCounts(
      int instancesToBeAdded, List<String> oldAsgNames, Map<String, Integer> existingDesiredCapacities) {
    int n = instancesToBeAdded;
    List<AwsAmiResizeData> desiredCapacities = newArrayList();
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
      AwsAmiPreDeploymentData preDeploymentData, List<String> classicLBs, List<String> targetGroupArns,
      boolean rollback, List<String> baseScalingPolicyJSONs, int desiredInstances) {
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
                                             .desiredInstances(desiredInstances)
                                             .preDeploymentData(preDeploymentData)
                                             .rollback(rollback)
                                             .baseScalingPolicyJSONs(baseScalingPolicyJSONs)
                                             .asgDesiredCounts(resizeData)
                                             .infraMappingClassisLbs(classicLBs)
                                             .infraMappingTargetGroupArns(targetGroupArns)
                                             .build();
    DelegateTask delegateTask =
        DelegateTask.builder()
            .accountId(accountId)
            .appId(appId)
            .waitId(activityId)
            .data(TaskData.builder()
                      .taskType(AWS_AMI_ASYNC_TASK.name())
                      .parameters(new Object[] {request})
                      .timeout(TimeUnit.MINUTES.toMillis(autoScalingSteadyStateTimeout))
                      .build())
            .tags(isNotEmpty(request.getAwsConfig().getTag()) ? singletonList(request.getAwsConfig().getTag()) : null)
            .async(true)
            .envId(envId)
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
    notNullCheck("Activity", activity);

    AmiServiceSetupElement serviceSetupElement = context.getContextElement(ContextElementType.AMI_SERVICE_SETUP);
    Builder logBuilder = aLog()
                             .withAppId(activity.getAppId())
                             .withActivityId(activity.getUuid())
                             .withLogLevel(LogLevel.INFO)
                             .withCommandUnitName(getCommandName())
                             .withExecutionResult(CommandExecutionStatus.RUNNING);

    ManagerExecutionLogCallback executionLogCallback =
        new ManagerExecutionLogCallback(logService, logBuilder, activity.getUuid());

    InstanceElementListParamBuilder instanceElementListParamBuilder =
        InstanceElementListParam.builder().instanceElements(Collections.emptyList());
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
      instanceElementListParamBuilder.instanceElements(instanceElements);
    } catch (Exception ex) {
      logger.error("Ami deploy step failed with error ", ex);
      executionStatus = ExecutionStatus.FAILED;
      errorMessage = ExceptionUtils.getMessage(ex);
      executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR);
    }

    activityService.updateStatus(activity.getUuid(), activity.getAppId(), executionStatus);

    executionLogCallback.saveExecutionLog(
        format("AutoScaling Group resize operation completed with status:[%s]", executionStatus),
        ExecutionStatus.SUCCESS == executionStatus ? LogLevel.INFO : LogLevel.ERROR,
        ExecutionStatus.SUCCESS == executionStatus ? CommandExecutionStatus.SUCCESS : CommandExecutionStatus.FAILURE);

    final InstanceElementListParam instanceElementListParam = instanceElementListParamBuilder.build();

    return ExecutionResponse.builder()
        .stateExecutionData(awsAmiDeployStateExecutionData)
        .executionStatus(executionStatus)
        .errorMessage(errorMessage)
        .contextElement(instanceElementListParam)
        .notifyElement(instanceElementListParam)
        .build();
  }

  protected List<InstanceElement> handleAsyncInternal(AwsAmiServiceDeployResponse amiServiceDeployResponse,
      ExecutionContext context, AmiServiceSetupElement serviceSetupElement,
      ManagerExecutionLogCallback executionLogCallback) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM);
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);

    AwsAmiDeployStateExecutionData awsAmiDeployStateExecutionData =
        (AwsAmiDeployStateExecutionData) context.getStateExecutionData();

    Application app = workflowStandardParams.getApp();
    notNullCheck("Application", app);

    Activity activity = activityService.get(awsAmiDeployStateExecutionData.getActivityId(), app.getUuid());
    notNullCheck("Activity", activity);

    AwsAmiInfrastructureMapping infrastructureMapping = (AwsAmiInfrastructureMapping) infrastructureMappingService.get(
        activity.getAppId(), context.fetchInfraMappingId());

    String serviceId = phaseElement.getServiceElement().getUuid();
    Environment env = workflowStandardParams.getEnv();
    notNullCheck("Environment", env);
    Service service = serviceResourceService.getWithDetails(app.getUuid(), serviceId);
    Key<ServiceTemplate> serviceTemplateKey =
        serviceTemplateService.getTemplateRefKeysByService(app.getUuid(), serviceId, env.getUuid()).get(0);

    Artifact artifact = ((DeploymentExecutionContext) context).getDefaultArtifactForService(serviceId);
    if (artifact == null) {
      throw new WingsException(format("Unable to find artifact for service %s", service.getName()));
    }

    List<Instance> ec2InstancesAdded = amiServiceDeployResponse.getInstancesAdded();
    List<InstanceElement> instanceElements = new ArrayList<>();
    if (isNotEmpty(ec2InstancesAdded)) {
      String serviceTemplateId = serviceTemplateHelper.fetchServiceTemplateId(infrastructureMapping);

      instanceElements.addAll(generateInstanceElements(amiServiceDeployResponse.getInstancesAdded(), context,
          phaseElement, infrastructureMapping, serviceTemplateKey, serviceTemplateId, true));
    }

    return instanceElements;
  }

  private List<InstanceElement> generateInstanceElements(List<Instance> instances, ExecutionContext context,
      PhaseElement phaseElement, AwsAmiInfrastructureMapping infrastructureMapping,
      Key<ServiceTemplate> serviceTemplateKey, String serviceTemplateId, boolean isUpsize) {
    if (isEmpty(instances)) {
      return emptyList();
    }

    return instances.stream()
        .map(instance -> {
          Host host = aHost()
                          .withHostName(awsUtils.getHostnameFromPrivateDnsName(instance.getPrivateDnsName()))
                          .withPublicDns(instance.getPublicDnsName())
                          .withEc2Instance(instance)
                          .withAppId(infrastructureMapping.getAppId())
                          .withEnvId(infrastructureMapping.getEnvId())
                          .withHostConnAttr(infrastructureMapping.getHostConnectionAttrs())
                          .withInfraMappingId(infrastructureMapping.getUuid())
                          .withInfraDefinitionId(infrastructureMapping.getInfrastructureDefinitionId())
                          .withServiceTemplateId(serviceTemplateId)
                          .build();
          Host savedHost = hostService.saveHost(host);
          HostElement hostElement = HostElement.builder()
                                        .uuid(savedHost.getUuid())
                                        .publicDns(instance.getPublicDnsName())
                                        .ip(instance.getPrivateIpAddress())
                                        .ec2Instance(instance)
                                        .instanceId(instance.getInstanceId())
                                        .build();

          final Map<String, Object> contextMap = context.asMap();
          contextMap.put("host", hostElement);
          String hostName = awsHelperService.getHostnameFromConvention(contextMap, "");
          hostElement.setHostName(hostName);
          return anInstanceElement()
              .uuid(instance.getInstanceId())
              .hostName(hostName)
              .displayName(instance.getPublicDnsName())
              .host(hostElement)
              .serviceTemplateElement(aServiceTemplateElement()
                                          .withUuid(serviceTemplateKey.getId().toString())
                                          .withServiceElement(phaseElement.getServiceElement())
                                          .build())
              .newInstance(isUpsize)
              .build();
        })
        .collect(toList());
  }

  @Override
  public Map<String, String> validateFields() {
    Map<String, String> invalidFields = new HashMap<>();
    if (!isRollback() && isEmpty(getInstanceCount())) {
      invalidFields.put("instanceCount", "Instance count must be greater than 0");
    }
    if (getCommandName() == null) {
      invalidFields.put("commandName", "Command name must not be null");
    }
    return invalidFields;
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  public String getInstanceCount() {
    return instanceCount;
  }

  public void setInstanceCount(String instanceCount) {
    this.instanceCount = instanceCount;
  }

  public InstanceUnitType getInstanceUnitType() {
    return instanceUnitType;
  }

  public void setInstanceUnitType(InstanceUnitType instanceUnitType) {
    this.instanceUnitType = instanceUnitType;
  }

  public String getCommandName() {
    return commandName;
  }

  public void setCommandName(String commandName) {
    this.commandName = commandName;
  }
}
