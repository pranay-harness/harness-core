package software.wings.sm.states;

import static java.util.stream.Collectors.toList;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.common.base.Joiner;
import com.google.common.io.BaseEncoding;
import com.google.inject.Inject;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupResult;
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationResult;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
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
import software.wings.beans.Application;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.AwsConfig;
import software.wings.beans.DeploymentExecutionContext;
import software.wings.beans.Environment;
import software.wings.beans.ResizeStrategy;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.container.UserDataSpecification;
import software.wings.common.Constants;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.EcsConvention;
import software.wings.utils.Misc;
import software.wings.utils.Validator;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

/**
 * Created by anubhaw on 12/19/17.
 */
public class AwsAmiServiceSetup extends State {
  private static final String HARNESS_AUTOSCALING_GROUP_TAG = "HARNESS_REVISION";
  private static final int MAX_OLD_ASG_VERSION_TO_KEEP = 2;
  private ResizeStrategy resizeStrategy;

  private String autoScalingGroupName;
  private Integer autoScalingSteadyStateTimeout;
  private Integer maxInstances;

  @Inject @Transient private transient AwsHelperService awsHelperService;
  @Inject @Transient protected transient SettingsService settingsService;
  @Inject @Transient protected transient ServiceResourceService serviceResourceService;
  @Inject @Transient protected transient InfrastructureMappingService infrastructureMappingService;
  @Inject @Transient protected transient ArtifactStreamService artifactStreamService;
  @Inject @Transient protected transient SecretManager secretManager;
  @Inject @Transient protected transient EncryptionService encryptionService;
  @Inject @Transient protected transient ActivityService activityService;
  @Inject @Transient protected transient DelegateService delegateService;
  @Inject @Transient protected transient ExecutorService executorService;

  @Transient private final transient Logger logger = LoggerFactory.getLogger(getClass());

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

    UserDataSpecification userDataSpecification =
        serviceResourceService.getUserDataSpecification(app.getUuid(), serviceId);

    AwsAmiInfrastructureMapping infrastructureMapping =
        (AwsAmiInfrastructureMapping) infrastructureMappingService.get(app.getUuid(), phaseElement.getInfraMappingId());
    // TODO:: ensure infraMappingType

    SettingAttribute cloudProviderSetting = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
    // TODO:: ensure cloudProviderType

    List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(
        (Encryptable) cloudProviderSetting.getValue(), context.getAppId(), context.getWorkflowExecutionId());

    String region = infrastructureMapping.getRegion();
    AwsConfig awsConfig = (AwsConfig) cloudProviderSetting.getValue();
    String baseAutoScalingGroupName = infrastructureMapping.getAutoScalingGroupName();
    AutoScalingGroup baseAutoScalingGroup = awsHelperService
                                                .describeAutoScalingGroups(awsConfig, encryptionDetails, region,
                                                    new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(
                                                        Arrays.asList(baseAutoScalingGroupName)))
                                                .getAutoScalingGroups()
                                                .get(0);
    // TODO:: ensure auto scaling group is present or else resort to last deployed auto scaling group

    Validator.notNullCheck("Auto Scaling Group", baseAutoScalingGroup);

    String baseAutoScalingGroupLaunchConfigurationName = baseAutoScalingGroup.getLaunchConfigurationName();
    List<LaunchConfiguration> launchConfigurations =
        awsHelperService.listLaunchConfiguration(awsConfig, encryptionDetails, region);
    // TODO:: filter on autoscaling group or launch config name

    LaunchConfiguration baseLaunchConfiguration =
        launchConfigurations.stream()
            .filter(launchConfiguration
                -> launchConfiguration.getLaunchConfigurationName().equals(baseAutoScalingGroupLaunchConfigurationName))
            .findFirst()
            .orElse(null);

    Validator.notNullCheck("Launch Configuration", baseLaunchConfiguration);

    List<AutoScalingGroup> autoScalingGroups =
        awsHelperService.listAutoScalingGroups(awsConfig, encryptionDetails, region);
    List<AutoScalingGroup> harnessAutoScalingGroups =
        autoScalingGroups.stream()
            .filter(autoScalingGroup
                -> autoScalingGroup.getTags().stream().anyMatch(
                    tagDescription -> tagDescription.getKey().equals(HARNESS_AUTOSCALING_GROUP_TAG)))
            .sorted(Comparator.comparing(AutoScalingGroup::getCreatedTime))
            .collect(toList());

    Integer harness_revision = 0;
    String oldAutoScalingGroupName = null;
    if (harnessAutoScalingGroups != null && harnessAutoScalingGroups.size() > 0) {
      AutoScalingGroup autoScalingGroup = harnessAutoScalingGroups.get(harnessAutoScalingGroups.size() - 1);
      Optional<TagDescription> optTag =
          autoScalingGroup.getTags()
              .stream()
              .filter(tagDescription -> tagDescription.getKey().equals(HARNESS_AUTOSCALING_GROUP_TAG))
              .findFirst();
      oldAutoScalingGroupName = autoScalingGroup.getAutoScalingGroupName();
      if (optTag.isPresent()) {
        harness_revision = Integer.parseInt(optTag.get().getValue());
      }
    }

    harness_revision += 1; // new version

    String newAutoScalingGroupName = EcsConvention.getServiceName(
        Misc.normalizeExpression(context.renderExpression(autoScalingGroupName)), harness_revision);

    LaunchConfiguration cloneBaseLaunchConfiguration = baseLaunchConfiguration.clone();
    cloneBaseLaunchConfiguration.setImageId(artifact.getRevision());
    cloneBaseLaunchConfiguration.setLaunchConfigurationName(newAutoScalingGroupName);

    CreateLaunchConfigurationRequest createLaunchConfigurationRequest =
        new CreateLaunchConfigurationRequest()
            .withLaunchConfigurationName(newAutoScalingGroupName)
            .withImageId(artifact.getRevision())
            .withAssociatePublicIpAddress(cloneBaseLaunchConfiguration.getAssociatePublicIpAddress())
            .withBlockDeviceMappings(cloneBaseLaunchConfiguration.getBlockDeviceMappings())
            .withClassicLinkVPCId(cloneBaseLaunchConfiguration.getClassicLinkVPCId())
            .withClassicLinkVPCSecurityGroups(cloneBaseLaunchConfiguration.getClassicLinkVPCSecurityGroups())
            .withEbsOptimized(cloneBaseLaunchConfiguration.getEbsOptimized())
            .withIamInstanceProfile(cloneBaseLaunchConfiguration.getIamInstanceProfile())
            .withInstanceMonitoring(cloneBaseLaunchConfiguration.getInstanceMonitoring())
            .withInstanceType(cloneBaseLaunchConfiguration.getInstanceType())
            //            .withKernelId(baseLaunchConfiguration.getKernelId())
            .withKeyName(cloneBaseLaunchConfiguration.getKeyName())
            .withPlacementTenancy(cloneBaseLaunchConfiguration.getPlacementTenancy())
            //            .withRamdiskId(baseLaunchConfiguration.getRamdiskId())
            .withSecurityGroups(cloneBaseLaunchConfiguration.getSecurityGroups())
            .withSpotPrice(cloneBaseLaunchConfiguration.getSpotPrice());

    // TODO:: check for fields to be non null
    if (userDataSpecification != null && userDataSpecification.getData() != null) {
      try {
        createLaunchConfigurationRequest.setUserData(
            BaseEncoding.base64().encode(userDataSpecification.getData().getBytes("UTF-8")));
      } catch (UnsupportedEncodingException e) {
        logger.error("Error in setting user data ", e);
      }
    }

    CreateLaunchConfigurationResult newLaunchConfiguration = awsHelperService.createLaunchConfiguration(
        awsConfig, encryptionDetails, region, createLaunchConfigurationRequest);

    AutoScalingGroup clonedBaseASG = baseAutoScalingGroup.clone();

    List<Tag> tags = clonedBaseASG.getTags()
                         .stream()
                         .filter(tagDescription -> !tagDescription.getKey().equals(HARNESS_AUTOSCALING_GROUP_TAG))
                         .map(tagDescription
                             -> new Tag()
                                    .withKey(tagDescription.getKey())
                                    .withValue(tagDescription.getValue())
                                    .withPropagateAtLaunch(tagDescription.getPropagateAtLaunch())
                                    .withResourceId(tagDescription.getResourceId())
                                    .withResourceType(tagDescription.getResourceType()))
                         .collect(toList());

    tags.add(new Tag()
                 .withKey(HARNESS_AUTOSCALING_GROUP_TAG)
                 .withValue(harness_revision.toString())
                 .withPropagateAtLaunch(true)
                 .withResourceType("auto-scaling-group"));

    // TODO:: check for fields to be non null
    CreateAutoScalingGroupRequest createAutoScalingGroupRequest =
        new CreateAutoScalingGroupRequest()
            .withAutoScalingGroupName(newAutoScalingGroupName)
            .withAvailabilityZones(clonedBaseASG.getAvailabilityZones())
            .withDefaultCooldown(clonedBaseASG.getDefaultCooldown())
            .withDesiredCapacity(0)
            .withMinSize(clonedBaseASG.getMinSize())
            .withMaxSize(clonedBaseASG.getMaxSize())
            .withAvailabilityZones(clonedBaseASG.getAvailabilityZones())
            .withHealthCheckGracePeriod(clonedBaseASG.getHealthCheckGracePeriod())
            .withHealthCheckType(clonedBaseASG.getHealthCheckType())
            .withLaunchConfigurationName(newAutoScalingGroupName)
            .withNewInstancesProtectedFromScaleIn(clonedBaseASG.getNewInstancesProtectedFromScaleIn())
            .withPlacementGroup(clonedBaseASG.getPlacementGroup())
            .withTags(tags)
            .withTerminationPolicies(clonedBaseASG.getTerminationPolicies())
            .withVPCZoneIdentifier(clonedBaseASG.getVPCZoneIdentifier());

    if (!Lists.isNullOrEmpty(infrastructureMapping.getClassicLoadBalancers())) {
      createAutoScalingGroupRequest.setLoadBalancerNames(infrastructureMapping.getClassicLoadBalancers());
    }

    if (!Lists.isNullOrEmpty(infrastructureMapping.getTargetGroupArns())) {
      createAutoScalingGroupRequest.setTargetGroupARNs(infrastructureMapping.getTargetGroupArns());
    }

    CreateAutoScalingGroupResult newAutoScalingGroup =
        awsHelperService.createAutoScalingGroup(awsConfig, encryptionDetails, region, createAutoScalingGroupRequest);

    AwsAmiSetupExecutionData awsAmiExecutionData = AwsAmiSetupExecutionData.builder()
                                                       .newAutoScalingGroupName(newAutoScalingGroupName)
                                                       .oldAutoScalingGroupName(oldAutoScalingGroupName)
                                                       .maxInstances(maxInstances)
                                                       .newVersion(harness_revision)
                                                       .resizeStrategy(resizeStrategy)
                                                       .build();
    AmiServiceSetupElement amiServiceElement = AmiServiceSetupElement.builder()
                                                   .newAutoScalingGroupName(newAutoScalingGroupName)
                                                   .oldAutoScalingGroupName(oldAutoScalingGroupName)
                                                   .maxInstances(maxInstances)
                                                   .resizeStrategy(resizeStrategy)
                                                   .build();
    deleteOldHarnessManagedAutoScalingGroups(encryptionDetails, region, awsConfig, harnessAutoScalingGroups);

    return anExecutionResponse()
        .withStateExecutionData(awsAmiExecutionData)
        .addContextElement(amiServiceElement)
        .addNotifyElement(amiServiceElement)
        .build();
  }

  private void deleteOldHarnessManagedAutoScalingGroups(List<EncryptedDataDetail> encryptionDetails, String region,
      AwsConfig awsConfig, List<AutoScalingGroup> harnessAutoScalingGroups) {
    try {
      List<AutoScalingGroup> emptyHarnessAsgToBeDeleted =
          harnessAutoScalingGroups.stream().filter(asg -> asg.getDesiredCapacity() == 0).collect(toList());
      if (emptyHarnessAsgToBeDeleted.size() > MAX_OLD_ASG_VERSION_TO_KEEP) {
        emptyHarnessAsgToBeDeleted =
            emptyHarnessAsgToBeDeleted.subList(0, emptyHarnessAsgToBeDeleted.size() - MAX_OLD_ASG_VERSION_TO_KEEP);
      }
      List<AutoScalingGroup> finalEmptyHarnessAsgToBeDeleted = emptyHarnessAsgToBeDeleted;
      executorService.submit(()
                                 -> awsHelperService.deleteAutoScalingGroups(
                                     awsConfig, encryptionDetails, region, finalEmptyHarnessAsgToBeDeleted));
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
}
