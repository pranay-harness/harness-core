package software.wings.sm.states;

import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.common.io.BaseEncoding;
import com.google.inject.Inject;

import com.amazonaws.services.autoscaling.model.AttachLoadBalancerTargetGroupsRequest;
import com.amazonaws.services.autoscaling.model.AttachLoadBalancersRequest;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupResult;
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationResult;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.amazonaws.services.autoscaling.model.Tag;
import com.amazonaws.services.autoscaling.model.TagDescription;
import org.mongodb.morphia.annotations.Transient;
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
import java.util.stream.Collectors;

/**
 * Created by anubhaw on 12/19/17.
 */
public class AwsAmiServiceSetup extends State {
  public static final String HARNESS_AUTOSCALING_GROUP_TAG = "HARNESS_REVISION";
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

    String launchConfigurationName = baseAutoScalingGroup.getLaunchConfigurationName();
    List<LaunchConfiguration> launchConfigurations =
        awsHelperService.listLaunchConfiguration(awsConfig, encryptionDetails, region);
    // TODO:: filter on autoscaling group or launch config name

    LaunchConfiguration baseLaunchConfiguration =
        launchConfigurations.stream()
            .filter(
                launchConfiguration -> launchConfiguration.getLaunchConfigurationName().equals(launchConfigurationName))
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
            .collect(Collectors.toList());

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

    artifact = Artifact.Builder.anArtifact().withRevision("ami-730c7064").build();
    LaunchConfiguration cloneBaseLaunchConfiguration = baseLaunchConfiguration.clone();
    cloneBaseLaunchConfiguration.setUserData(userDataSpecification.getData());
    cloneBaseLaunchConfiguration.setImageId(artifact.getRevision());
    cloneBaseLaunchConfiguration.setLaunchConfigurationName(newAutoScalingGroupName);

    String encodededUserData = null;
    try {
      encodededUserData = BaseEncoding.base64().encode(userDataSpecification.getData().getBytes("UTF-8"));
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
      // TODO:: handle error
    }

    // TODO:: check for fields to be non null
    CreateLaunchConfigurationRequest createLaunchConfigurationRequest =
        new CreateLaunchConfigurationRequest()
            .withLaunchConfigurationName(newAutoScalingGroupName)
            .withImageId(artifact.getRevision())
            .withUserData(encodededUserData)
            .withAssociatePublicIpAddress(baseLaunchConfiguration.getAssociatePublicIpAddress())
            .withBlockDeviceMappings(baseLaunchConfiguration.getBlockDeviceMappings())
            .withClassicLinkVPCId(baseLaunchConfiguration.getClassicLinkVPCId())
            .withClassicLinkVPCSecurityGroups(baseLaunchConfiguration.getClassicLinkVPCSecurityGroups())
            .withEbsOptimized(baseLaunchConfiguration.getEbsOptimized())
            .withIamInstanceProfile(baseLaunchConfiguration.getIamInstanceProfile())
            .withInstanceMonitoring(baseLaunchConfiguration.getInstanceMonitoring())
            .withInstanceType(baseLaunchConfiguration.getInstanceType())
            //            .withKernelId(baseLaunchConfiguration.getKernelId())
            .withKeyName(baseLaunchConfiguration.getKeyName())
            .withPlacementTenancy(baseLaunchConfiguration.getPlacementTenancy())
            //            .withRamdiskId(baseLaunchConfiguration.getRamdiskId())
            .withSecurityGroups(baseLaunchConfiguration.getSecurityGroups())
            .withSpotPrice(baseLaunchConfiguration.getSpotPrice());

    CreateLaunchConfigurationResult newLaunchConfiguration = awsHelperService.createLaunchConfiguration(
        awsConfig, encryptionDetails, region, createLaunchConfigurationRequest);

    List<Tag> tags = baseAutoScalingGroup.getTags()
                         .stream()
                         .map(tagDescription
                             -> new Tag()
                                    .withKey(tagDescription.getKey())
                                    .withValue(tagDescription.getValue())
                                    .withPropagateAtLaunch(tagDescription.getPropagateAtLaunch())
                                    .withResourceId(tagDescription.getResourceId())
                                    .withResourceType(tagDescription.getResourceType()))
                         .collect(Collectors.toList());

    tags.add(new Tag()
                 .withKey(HARNESS_AUTOSCALING_GROUP_TAG)
                 .withValue(harness_revision.toString())
                 .withPropagateAtLaunch(true)
                 .withResourceType("auto-scaling-group"));

    // TODO:: check for fields to be non null
    CreateAutoScalingGroupRequest createAutoScalingGroupRequest =
        new CreateAutoScalingGroupRequest()
            .withAutoScalingGroupName(newAutoScalingGroupName)
            .withAvailabilityZones(baseAutoScalingGroup.getAvailabilityZones())
            .withDefaultCooldown(baseAutoScalingGroup.getDefaultCooldown())
            .withDesiredCapacity(0)
            .withMinSize(baseAutoScalingGroup.getMinSize())
            .withMaxSize(baseAutoScalingGroup.getMaxSize())
            .withAvailabilityZones(baseAutoScalingGroup.getAvailabilityZones())
            .withHealthCheckGracePeriod(baseAutoScalingGroup.getHealthCheckGracePeriod())
            .withHealthCheckType(baseAutoScalingGroup.getHealthCheckType())
            .withLaunchConfigurationName(baseAutoScalingGroup.getLaunchConfigurationName())
            .withLoadBalancerNames(baseAutoScalingGroup.getLoadBalancerNames())
            .withNewInstancesProtectedFromScaleIn(baseAutoScalingGroup.getNewInstancesProtectedFromScaleIn())
            .withPlacementGroup(baseAutoScalingGroup.getPlacementGroup())
            .withTags(tags)
            .withTargetGroupARNs(baseAutoScalingGroup.getTargetGroupARNs())
            .withTerminationPolicies(baseAutoScalingGroup.getTerminationPolicies())
            .withVPCZoneIdentifier(baseAutoScalingGroup.getVPCZoneIdentifier());

    CreateAutoScalingGroupResult newAutoScalingGroup =
        awsHelperService.createAutoScalingGroup(awsConfig, encryptionDetails, region, createAutoScalingGroupRequest);

    awsHelperService.attachLoadBalancerToAutoScalingGroup(awsConfig, encryptionDetails, region,
        new AttachLoadBalancersRequest()
            .withLoadBalancerNames(infrastructureMapping.getClassicLoadBalancers())
            .withAutoScalingGroupName(newAutoScalingGroupName));
    awsHelperService.attachTargetGroupsToAutoScalingGroup(awsConfig, encryptionDetails, region,
        new AttachLoadBalancerTargetGroupsRequest()
            .withAutoScalingGroupName(newAutoScalingGroupName)
            .withTargetGroupARNs(infrastructureMapping.getTargetGroupArns()));

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
    return anExecutionResponse()
        .withStateExecutionData(awsAmiExecutionData)
        .addContextElement(amiServiceElement)
        .addNotifyElement(amiServiceElement)
        .build();
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
