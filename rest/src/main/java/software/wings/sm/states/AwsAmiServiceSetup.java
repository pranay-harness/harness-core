package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.ResizeStrategy.RESIZE_NEW_FIRST;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.utils.AsgConvention.getRevisionFromTag;
import static software.wings.utils.Misc.normalizeExpression;

import com.google.common.base.Joiner;
import com.google.common.io.BaseEncoding;
import com.google.inject.Inject;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.amazonaws.services.autoscaling.model.Tag;
import com.amazonaws.services.autoscaling.model.TagDescription;
import io.fabric8.utils.Lists;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.Encryptable;
import software.wings.api.AmiServiceSetupElement;
import software.wings.api.AwsAmiSetupExecutionData;
import software.wings.api.PhaseElement;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.AwsConfig;
import software.wings.beans.DeploymentExecutionContext;
import software.wings.beans.Environment;
import software.wings.beans.Log.Builder;
import software.wings.beans.ResizeStrategy;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.container.UserDataSpecification;
import software.wings.common.Constants;
import software.wings.exception.InvalidRequestException;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.LogService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.AsgConvention;
import software.wings.utils.Misc;

import java.io.UnsupportedEncodingException;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
/**
 * Created by anubhaw on 12/19/17.
 */
public class AwsAmiServiceSetup extends State {
  protected static final String HARNESS_AUTOSCALING_GROUP_TAG = "HARNESS_REVISION";
  protected static final String NAME_TAG = "Name";
  protected static final String AUTOSCALING_GROUP_RESOURCE_TYPE = "auto-scaling-group";
  private static final int MAX_OLD_ASG_VERSION_TO_KEEP = 3;

  private String autoScalingGroupName;
  private int autoScalingSteadyStateTimeout;
  private int maxInstances;
  private ResizeStrategy resizeStrategy;

  @Inject @Transient private transient AwsHelperService awsHelperService;
  @Inject @Transient protected transient SettingsService settingsService;
  @Inject @Transient protected transient ServiceResourceService serviceResourceService;
  @Inject @Transient protected transient InfrastructureMappingService infrastructureMappingService;
  @Inject @Transient protected transient SecretManager secretManager;
  @Inject @Transient protected transient ActivityService activityService;
  @Inject @Transient protected transient ExecutorService executorService;
  @Inject @Transient protected transient LogService logService;

  @Transient private static final Logger logger = LoggerFactory.getLogger(AwsAmiServiceSetup.class);

  private String commandName = Constants.AMI_SETUP_COMMAND_NAME;
  /**
   * Instantiates a new state.
   *
   * @param name the name
   */
  public AwsAmiServiceSetup(String name) {
    super(name, StateType.AWS_AMI_SERVICE_SETUP.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String serviceId = phaseElement.getServiceElement().getUuid();

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    Artifact artifact = ((DeploymentExecutionContext) context).getArtifactForService(serviceId);

    Application app = workflowStandardParams.getApp();
    Environment env = workflowStandardParams.getEnv();
    Service service = serviceResourceService.get(app.getUuid(), serviceId);

    AwsAmiInfrastructureMapping infrastructureMapping =
        (AwsAmiInfrastructureMapping) infrastructureMappingService.get(app.getUuid(), phaseElement.getInfraMappingId());

    SettingAttribute cloudProviderSetting = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
    List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(
        (Encryptable) cloudProviderSetting.getValue(), context.getAppId(), context.getWorkflowExecutionId());
    String region = infrastructureMapping.getRegion();
    AwsConfig awsConfig = (AwsConfig) cloudProviderSetting.getValue();

    AwsAmiSetupExecutionData awsAmiExecutionData = AwsAmiSetupExecutionData.builder().build();
    AmiServiceSetupElement amiServiceElement = AmiServiceSetupElement.builder().build();
    ExecutionStatus executionStatus = ExecutionStatus.SUCCESS;
    String errorMessage = null;

    String envId = env.getUuid();
    Command command =
        serviceResourceService.getCommandByName(app.getUuid(), serviceId, envId, getCommandName()).getCommand();

    List<CommandUnit> commandUnitList =
        serviceResourceService.getFlattenCommandUnitList(app.getUuid(), serviceId, envId, command.getName());

    ActivityBuilder activityBuilder = Activity.builder()
                                          .applicationName(app.getName())
                                          .environmentId(envId)
                                          .environmentName(env.getName())
                                          .environmentType(env.getEnvironmentType())
                                          .serviceId(service.getUuid())
                                          .serviceName(service.getName())
                                          .commandName(command.getName())
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
                                          .status(ExecutionStatus.RUNNING);

    Activity build = activityBuilder.build();
    build.setAppId(app.getUuid());
    Activity activity = activityService.save(build);

    String commandUnitName = commandUnitList.get(0).getName();

    Builder logBuilder =
        aLog().withAppId(activity.getAppId()).withActivityId(activity.getUuid()).withCommandUnitName(commandUnitName);

    ManagerExecutionLogCallback executionLogCallback =
        new ManagerExecutionLogCallback(logService, logBuilder, activity.getUuid());

    try {
      executionLogCallback.saveExecutionLog("Starting AWS AMI Setup");
      UserDataSpecification userDataSpecification =
          serviceResourceService.getUserDataSpecification(app.getUuid(), serviceId);

      AutoScalingGroup baseAutoScalingGroup = ensureAndGetBaseAutoScalingGroup(
          infrastructureMapping, encryptionDetails, region, awsConfig, infrastructureMapping.getAutoScalingGroupName());

      LaunchConfiguration baseLaunchConfiguration = ensureAndGetBaseLaunchConfiguration(
          infrastructureMapping, encryptionDetails, region, awsConfig, baseAutoScalingGroup);

      List<AutoScalingGroup> harnessManagedAutoScalingGroups =
          awsHelperService.listAutoScalingGroups(awsConfig, encryptionDetails, region)
              .stream()
              .filter(autoScalingGroup
                  -> autoScalingGroup.getTags().stream().anyMatch(
                      tagDescription -> isHarnessManagedTag(infrastructureMapping.getUuid(), tagDescription)))
              .sorted(Comparator.comparing(AutoScalingGroup::getCreatedTime).reversed())
              .collect(toList());

      String lastDeployedAsgName = getLastDeployedAsgNameWithNonZeroCapacity(harnessManagedAutoScalingGroups);
      Integer harnessRevision = getNewHarnessVersion(harnessManagedAutoScalingGroups);

      String asgNamePrefix = isNotEmpty(autoScalingGroupName)
          ? normalizeExpression(context.renderExpression(autoScalingGroupName))
          : AsgConvention.getAsgNamePrefix(app.getName(), service.getName(), env.getName());
      String newAutoScalingGroupName = AsgConvention.getAsgName(asgNamePrefix, harnessRevision);

      /**
       * Defaulting to 10 max instances in case we get maxInstances as 0
       */
      maxInstances = getMaxInstances() == 0 ? 10 : getMaxInstances();
      /**
       * Defaulting to 10 minutes timeout in case we get autoScalingSteadyStateTimeout as 0
       */
      autoScalingSteadyStateTimeout = getAutoScalingSteadyStateTimeout() == 0 ? (int) TimeUnit.MINUTES.toMinutes(10)
                                                                              : autoScalingSteadyStateTimeout;

      executionLogCallback.saveExecutionLog(format(
          "Setting up AWS AMI with old AutoScalingGroupName [%s] to new AutoScalingGroupName [%s], maxInstances [%s], autoScalingSteadyTimeout [%s] minutes in [%s] region",
          lastDeployedAsgName == null ? "N/A" : lastDeployedAsgName, newAutoScalingGroupName, maxInstances,
          autoScalingSteadyStateTimeout, region));

      awsAmiExecutionData = AwsAmiSetupExecutionData.builder()
                                .newAutoScalingGroupName(newAutoScalingGroupName)
                                .oldAutoScalingGroupName(lastDeployedAsgName)
                                .activityId(activity.getUuid())
                                .maxInstances(maxInstances)
                                .newVersion(harnessRevision)
                                .resizeStrategy(resizeStrategy)
                                .build();

      amiServiceElement = AmiServiceSetupElement.builder()
                              .newAutoScalingGroupName(newAutoScalingGroupName)
                              .oldAutoScalingGroupName(lastDeployedAsgName)
                              .maxInstances(maxInstances)
                              .resizeStrategy(getResizeStrategy() == null ? RESIZE_NEW_FIRST : getResizeStrategy())
                              .autoScalingSteadyStateTimeout(autoScalingSteadyStateTimeout)
                              .commandName(commandName)
                              .build();

      LaunchConfiguration oldLaunchConfiguration =
          awsHelperService.getLaunchConfiguration(awsConfig, encryptionDetails, region, newAutoScalingGroupName);
      if (oldLaunchConfiguration != null) {
        executionLogCallback.saveExecutionLog(
            format("Deleting old launch configuration [%s]", oldLaunchConfiguration.getLaunchConfigurationName()));
        awsHelperService.deleteLaunchConfig(awsConfig, encryptionDetails, region, newAutoScalingGroupName);
      }
      awsHelperService.createLaunchConfiguration(awsConfig, encryptionDetails, region,
          createNewLaunchConfigurationRequest(
              context, artifact, userDataSpecification, baseLaunchConfiguration, newAutoScalingGroupName));
      executionLogCallback.saveExecutionLog(
          format("Creating new launch configuration [%s]", baseLaunchConfiguration.getLaunchConfigurationName()));
      awsHelperService.createAutoScalingGroup(awsConfig, encryptionDetails, region,
          createNewAutoScalingGroupRequest(
              infrastructureMapping, newAutoScalingGroupName, baseAutoScalingGroup, harnessRevision),
          executionLogCallback);
      executionLogCallback.saveExecutionLog(format("Creating new AutoScalingGroup [%s]", newAutoScalingGroupName));
      deleteOldHarnessManagedAutoScalingGroups(encryptionDetails, region, awsConfig, harnessManagedAutoScalingGroups,
          amiServiceElement.getOldAutoScalingGroupName(), executionLogCallback);

      executionLogCallback.saveExecutionLog(
          format("Deleting old AutoScalingGroup [%s]", amiServiceElement.getOldAutoScalingGroupName()));

      executionLogCallback.saveExecutionLog(
          format("Completed AWS AMI Setup with new autoScalingGroupName [%s]", newAutoScalingGroupName));
    } catch (Exception exception) {
      logger.error("Ami setup step failed with error ", exception);
      executionStatus = ExecutionStatus.FAILED;
      errorMessage = Misc.getMessage(exception);
      awsAmiExecutionData.setStatus(executionStatus);
      awsAmiExecutionData.setErrorMsg(errorMessage);
      Misc.logAllMessages(exception, executionLogCallback, CommandExecutionStatus.FAILURE);
    }

    return anExecutionResponse()
        .withStateExecutionData(awsAmiExecutionData)
        .addContextElement(amiServiceElement)
        .addNotifyElement(amiServiceElement)
        .withExecutionStatus(executionStatus)
        .withErrorMessage(errorMessage)
        .build();
  }

  private boolean isHarnessManagedTag(String infraMappingId, TagDescription tagDescription) {
    return tagDescription.getKey().equals(HARNESS_AUTOSCALING_GROUP_TAG)
        && tagDescription.getValue().startsWith(infraMappingId);
  }

  private LaunchConfiguration ensureAndGetBaseLaunchConfiguration(AwsAmiInfrastructureMapping infrastructureMapping,
      List<EncryptedDataDetail> encryptionDetails, String region, AwsConfig awsConfig,
      AutoScalingGroup baseAutoScalingGroup) {
    LaunchConfiguration baseAutoScalingGroupLaunchConfiguration = awsHelperService.getLaunchConfiguration(
        awsConfig, encryptionDetails, region, baseAutoScalingGroup.getLaunchConfigurationName());

    if (baseAutoScalingGroupLaunchConfiguration == null) {
      throw new InvalidRequestException(format(
          "LaunchConfiguration [%s] for referenced AutoScaling Group [%s] provided in Service Infrastructure couldn't be found in AWS region [%s]",
          baseAutoScalingGroup.getAutoScalingGroupName(), infrastructureMapping.getAutoScalingGroupName(),
          infrastructureMapping.getRegion()));
    }
    return baseAutoScalingGroupLaunchConfiguration;
  }

  private AutoScalingGroup ensureAndGetBaseAutoScalingGroup(AwsAmiInfrastructureMapping infrastructureMapping,
      List<EncryptedDataDetail> encryptionDetails, String region, AwsConfig awsConfig,
      String baseAutoScalingGroupName) {
    AutoScalingGroup baseAutoScalingGroup =
        awsHelperService.getAutoScalingGroup(awsConfig, encryptionDetails, region, baseAutoScalingGroupName);
    if (baseAutoScalingGroup == null) {
      logger.error("Couldn't find reference AutoScalingGroup: {}", infrastructureMapping.getAutoScalingGroupName());
      throw new InvalidRequestException(format(
          "Reference AutoScaling Group [%s] provided in Service Infrastructure couldn't be found in AWS region [%s]",
          infrastructureMapping.getAutoScalingGroupName(), infrastructureMapping.getRegion()));
    }
    return baseAutoScalingGroup;
  }

  private Integer getNewHarnessVersion(List<AutoScalingGroup> harnessManagedAutoScalingGroups) {
    Integer harnessRevision = 1;
    if (isNotEmpty(harnessManagedAutoScalingGroups)) {
      harnessRevision = harnessManagedAutoScalingGroups.stream()
                            .flatMap(autoScalingGroup -> autoScalingGroup.getTags().stream())
                            .filter(tagDescription -> tagDescription.getKey().equals(HARNESS_AUTOSCALING_GROUP_TAG))
                            .mapToInt(tagDescription -> getRevisionFromTag(tagDescription.getValue()))
                            .max()
                            .orElse(0);
      harnessRevision += 1; // bump it by 1
    }
    return harnessRevision;
  }

  private String getLastDeployedAsgNameWithNonZeroCapacity(List<AutoScalingGroup> harnessManagedAutoScalingGroups) {
    String oldAutoScalingGroupName = null;
    if (isNotEmpty(harnessManagedAutoScalingGroups)) {
      oldAutoScalingGroupName = harnessManagedAutoScalingGroups.stream()
                                    .filter(hAsg -> hAsg.getDesiredCapacity() != 0)
                                    .findFirst()
                                    .orElse(harnessManagedAutoScalingGroups.get(
                                        harnessManagedAutoScalingGroups.size() - 1)) // take the last deployed anyway
                                    .getAutoScalingGroupName();
    }
    return oldAutoScalingGroupName;
  }

  private CreateLaunchConfigurationRequest createNewLaunchConfigurationRequest(ExecutionContext context,
      Artifact artifact, UserDataSpecification userDataSpecification, LaunchConfiguration cloneBaseLaunchConfiguration,
      String newAutoScalingGroupName) {
    CreateLaunchConfigurationRequest createLaunchConfigurationRequest =
        new CreateLaunchConfigurationRequest()
            .withLaunchConfigurationName(newAutoScalingGroupName)
            .withImageId(artifact.getRevision())
            .withSecurityGroups(cloneBaseLaunchConfiguration.getSecurityGroups())
            .withClassicLinkVPCId(cloneBaseLaunchConfiguration.getClassicLinkVPCId())
            .withBlockDeviceMappings(cloneBaseLaunchConfiguration.getBlockDeviceMappings())
            .withEbsOptimized(cloneBaseLaunchConfiguration.getEbsOptimized())
            .withAssociatePublicIpAddress(cloneBaseLaunchConfiguration.getAssociatePublicIpAddress());

    if (userDataSpecification != null && userDataSpecification.getData() != null) {
      try {
        String userData = userDataSpecification.getData();
        String userDataAfterEvaluation = context.renderExpression(userData);
        createLaunchConfigurationRequest.setUserData(
            BaseEncoding.base64().encode(userDataAfterEvaluation.getBytes("UTF-8")));
      } catch (UnsupportedEncodingException e) {
        logger.error("Error in setting user data ", e);
      }
    }

    if (isNotBlank(cloneBaseLaunchConfiguration.getInstanceType())) {
      createLaunchConfigurationRequest.setInstanceType(cloneBaseLaunchConfiguration.getInstanceType());
    }
    if (isNotBlank(cloneBaseLaunchConfiguration.getKernelId())) {
      createLaunchConfigurationRequest.setKernelId(cloneBaseLaunchConfiguration.getKernelId());
    }

    if (isNotBlank(cloneBaseLaunchConfiguration.getRamdiskId())) {
      createLaunchConfigurationRequest.setRamdiskId(cloneBaseLaunchConfiguration.getRamdiskId());
    }
    if (cloneBaseLaunchConfiguration.getInstanceMonitoring() != null) {
      createLaunchConfigurationRequest.setInstanceMonitoring(cloneBaseLaunchConfiguration.getInstanceMonitoring());
    }
    if (isNotBlank(cloneBaseLaunchConfiguration.getSpotPrice())) {
      createLaunchConfigurationRequest.setSpotPrice(cloneBaseLaunchConfiguration.getSpotPrice());
    }
    if (isNotBlank(cloneBaseLaunchConfiguration.getIamInstanceProfile())) {
      createLaunchConfigurationRequest.setIamInstanceProfile(cloneBaseLaunchConfiguration.getIamInstanceProfile());
    }
    if (isNotBlank(cloneBaseLaunchConfiguration.getPlacementTenancy())) {
      createLaunchConfigurationRequest.setPlacementTenancy(cloneBaseLaunchConfiguration.getPlacementTenancy());
    }
    if (isNotBlank(cloneBaseLaunchConfiguration.getKeyName())) {
      createLaunchConfigurationRequest.setKeyName(cloneBaseLaunchConfiguration.getKeyName());
    }

    return createLaunchConfigurationRequest;
  }

  protected CreateAutoScalingGroupRequest createNewAutoScalingGroupRequest(
      AwsAmiInfrastructureMapping infrastructureMapping, String newAutoScalingGroupName,
      AutoScalingGroup baseAutoScalingGroup, Integer harnessRevision) {
    List<Tag> tags =
        baseAutoScalingGroup.getTags()
            .stream()
            .filter(
                tagDescription -> !asList(HARNESS_AUTOSCALING_GROUP_TAG, NAME_TAG).contains(tagDescription.getKey()))
            .map(tagDescription
                -> new Tag()
                       .withKey(tagDescription.getKey())
                       .withValue(tagDescription.getValue())
                       .withPropagateAtLaunch(tagDescription.getPropagateAtLaunch())
                       .withResourceType(tagDescription.getResourceType()))
            .collect(toList());

    tags.add(new Tag()
                 .withKey(HARNESS_AUTOSCALING_GROUP_TAG)
                 .withValue(AsgConvention.getRevisionTagValue(infrastructureMapping.getUuid(), harnessRevision))
                 .withPropagateAtLaunch(true)
                 .withResourceType(AUTOSCALING_GROUP_RESOURCE_TYPE));
    tags.add(new Tag().withKey(NAME_TAG).withValue(newAutoScalingGroupName).withPropagateAtLaunch(true));

    CreateAutoScalingGroupRequest createAutoScalingGroupRequest =
        new CreateAutoScalingGroupRequest()
            .withAutoScalingGroupName(newAutoScalingGroupName)
            .withLaunchConfigurationName(newAutoScalingGroupName)
            .withDesiredCapacity(0)
            .withMinSize(0)
            .withMaxSize(baseAutoScalingGroup.getMaxSize())
            .withTags(tags)
            .withDefaultCooldown(baseAutoScalingGroup.getDefaultCooldown())
            .withAvailabilityZones(baseAutoScalingGroup.getAvailabilityZones())
            .withTerminationPolicies(baseAutoScalingGroup.getTerminationPolicies())
            .withNewInstancesProtectedFromScaleIn(baseAutoScalingGroup.getNewInstancesProtectedFromScaleIn());

    if (!Lists.isNullOrEmpty(infrastructureMapping.getClassicLoadBalancers())) {
      createAutoScalingGroupRequest.setLoadBalancerNames(infrastructureMapping.getClassicLoadBalancers());
    }

    if (!Lists.isNullOrEmpty(infrastructureMapping.getTargetGroupArns())) {
      createAutoScalingGroupRequest.setTargetGroupARNs(infrastructureMapping.getTargetGroupArns());
    }

    if (baseAutoScalingGroup.getDefaultCooldown() != null) {
      createAutoScalingGroupRequest.setDefaultCooldown(baseAutoScalingGroup.getDefaultCooldown());
    }

    if (isNotBlank(baseAutoScalingGroup.getHealthCheckType())) {
      createAutoScalingGroupRequest.setHealthCheckType(baseAutoScalingGroup.getHealthCheckType());
    }
    if (baseAutoScalingGroup.getHealthCheckGracePeriod() != null) {
      createAutoScalingGroupRequest.setHealthCheckGracePeriod(baseAutoScalingGroup.getHealthCheckGracePeriod());
    }
    if (isNotBlank(baseAutoScalingGroup.getPlacementGroup())) {
      createAutoScalingGroupRequest.setPlacementGroup(baseAutoScalingGroup.getPlacementGroup());
    }

    if (isNotBlank(baseAutoScalingGroup.getVPCZoneIdentifier())) {
      createAutoScalingGroupRequest.setVPCZoneIdentifier(baseAutoScalingGroup.getVPCZoneIdentifier());
    }
    return createAutoScalingGroupRequest;
  }

  private void deleteOldHarnessManagedAutoScalingGroups(List<EncryptedDataDetail> encryptionDetails, String region,
      AwsConfig awsConfig, List<AutoScalingGroup> harnessAutoScalingGroups, String oldAutoScalingGroupName,
      ManagerExecutionLogCallback logCallback) {
    try {
      List<AutoScalingGroup> emptyHarnessAsgToBeDeleted =
          harnessAutoScalingGroups.stream()
              .filter(asg
                  -> asg.getDesiredCapacity() == 0 && !asg.getAutoScalingGroupName().equals(oldAutoScalingGroupName))
              .collect(toList());
      if (emptyHarnessAsgToBeDeleted.size() >= MAX_OLD_ASG_VERSION_TO_KEEP) {
        int startIdx = MAX_OLD_ASG_VERSION_TO_KEEP;
        if (isNotBlank(oldAutoScalingGroupName)) {
          startIdx--; // one is already counted as oldAutoScalingGroup
        }
        emptyHarnessAsgToBeDeleted = emptyHarnessAsgToBeDeleted.subList(startIdx, emptyHarnessAsgToBeDeleted.size());
        logger.info("ASG Cleanup. Deleting following ASG [{}]", Joiner.on(",").join(emptyHarnessAsgToBeDeleted));
        List<AutoScalingGroup> finalEmptyHarnessAsgToBeDeleted = emptyHarnessAsgToBeDeleted;
        executorService.submit(()
                                   -> awsHelperService.deleteAutoScalingGroups(awsConfig, encryptionDetails, region,
                                       finalEmptyHarnessAsgToBeDeleted, logCallback));
      }
    } catch (Exception ex) {
      logger.error(
          "Error in deleting old autoScaling groups [{}] [{}]", Joiner.on(",").join(harnessAutoScalingGroups), ex);
    }
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  /**
   * Gets resize strategy.
   *
   * @return the resize strategy
   */
  public ResizeStrategy getResizeStrategy() {
    return resizeStrategy;
  }

  /**
   * Sets resize strategy.
   *
   * @param resizeStrategy the resize strategy
   */
  public void setResizeStrategy(ResizeStrategy resizeStrategy) {
    this.resizeStrategy = resizeStrategy;
  }

  /**
   * Gets ecs service name.
   *
   * @return the ecs service name
   */
  public String getAutoScalingGroupName() {
    return autoScalingGroupName;
  }

  /**
   * Sets ecs service name.
   *
   * @param autoScalingGroupName the ecs service name
   */
  public void setAutoScalingGroupName(String autoScalingGroupName) {
    this.autoScalingGroupName = autoScalingGroupName;
  }

  /**
   * Gets auto scaling steady state timeout.
   *
   * @return the auto scaling steady state timeout
   */
  public int getAutoScalingSteadyStateTimeout() {
    return autoScalingSteadyStateTimeout;
  }

  /**
   * Sets auto scaling steady state timeout.
   *
   * @param autoScalingSteadyStateTimeout the auto scaling steady state timeout
   */
  public void setAutoScalingSteadyStateTimeout(int autoScalingSteadyStateTimeout) {
    this.autoScalingSteadyStateTimeout = autoScalingSteadyStateTimeout;
  }

  public int getMaxInstances() {
    return maxInstances;
  }

  public void setMaxInstances(int maxInstances) {
    this.maxInstances = maxInstances;
  }

  public String getCommandName() {
    return commandName;
  }

  public void setCommandName(String commandName) {
    this.commandName = commandName;
  }
}
