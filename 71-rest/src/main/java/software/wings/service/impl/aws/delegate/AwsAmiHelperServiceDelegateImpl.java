package software.wings.service.impl.aws.delegate;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.sm.ExecutionStatus.FAILED;
import static software.wings.sm.ExecutionStatus.SUCCESS;
import static software.wings.utils.AsgConvention.getRevisionFromTag;
import static software.wings.utils.Misc.getMessage;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.amazonaws.services.autoscaling.model.Tag;
import com.amazonaws.services.autoscaling.model.TagDescription;
import com.amazonaws.services.ec2.model.Instance;
import io.fabric8.utils.Lists;
import io.harness.exception.InvalidRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AwsConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.aws.model.AwsAmiPreDeploymentData;
import software.wings.service.impl.aws.model.AwsAmiResizeData;
import software.wings.service.impl.aws.model.AwsAmiServiceDeployRequest;
import software.wings.service.impl.aws.model.AwsAmiServiceDeployResponse;
import software.wings.service.impl.aws.model.AwsAmiServiceSetupRequest;
import software.wings.service.impl.aws.model.AwsAmiServiceSetupResponse;
import software.wings.service.impl.aws.model.AwsAmiServiceSetupResponse.AwsAmiServiceSetupResponseBuilder;
import software.wings.service.intfc.aws.delegate.AwsAmiHelperServiceDelegate;
import software.wings.service.intfc.aws.delegate.AwsAsgHelperServiceDelegate;
import software.wings.utils.AsgConvention;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

@Singleton
public class AwsAmiHelperServiceDelegateImpl
    extends AwsHelperServiceDelegateBase implements AwsAmiHelperServiceDelegate {
  private static final Logger logger = LoggerFactory.getLogger(AwsAmiHelperServiceDelegateImpl.class);
  private static final String AUTOSCALING_GROUP_RESOURCE_TYPE = "auto-scaling-group";
  @VisibleForTesting static final String HARNESS_AUTOSCALING_GROUP_TAG = "HARNESS_REVISION";
  @VisibleForTesting static final String NAME_TAG = "Name";
  private static final int MAX_OLD_ASG_VERSION_TO_KEEP = 3;
  @Inject private ExecutorService executorService;
  @Inject private AwsAsgHelperServiceDelegate awsAsgHelperServiceDelegate;

  @Override
  public AwsAmiServiceDeployResponse deployAmiService(
      AwsAmiServiceDeployRequest request, ExecutionLogCallback logCallback) {
    try {
      AwsConfig awsConfig = request.getAwsConfig();
      List<EncryptedDataDetail> encryptionDetails = request.getEncryptionDetails();
      encryptionService.decrypt(awsConfig, encryptionDetails);
      logCallback.saveExecutionLog("Starting AWS AMI Deploy", INFO);

      logCallback.saveExecutionLog("Getting existing instance Ids");
      Set<String> existingInstanceIds = Sets.newHashSet(awsAsgHelperServiceDelegate.listAutoScalingGroupInstanceIds(
          awsConfig, encryptionDetails, request.getRegion(), request.getNewAutoScalingGroupName()));

      logCallback.saveExecutionLog("Resizing Asgs", INFO);
      resizeAsgs(request.getRegion(), awsConfig, encryptionDetails, request.getNewAutoScalingGroupName(),
          request.getNewAsgFinalDesiredCount(), request.getAsgDesiredCounts(), logCallback, request.isResizeNewFirst(),
          request.getAutoScalingSteadyStateTimeout(), request.getMaxInstances(), request.getMinInstances(),
          request.getPreDeploymentData());

      List<Instance> allInstancesOfNewAsg = awsAsgHelperServiceDelegate.listAutoScalingGroupInstances(
          awsConfig, encryptionDetails, request.getRegion(), request.getNewAutoScalingGroupName());
      List<Instance> instancesAdded = allInstancesOfNewAsg.stream()
                                          .filter(instance -> !existingInstanceIds.contains(instance.getInstanceId()))
                                          .collect(toList());
      return AwsAmiServiceDeployResponse.builder().instancesAdded(instancesAdded).executionStatus(SUCCESS).build();
    } catch (Exception ex) {
      logCallback.saveExecutionLog(format("Exception: [%s].", getMessage(ex)), ERROR);
      logger.error(ex.getMessage(), ex);
      return AwsAmiServiceDeployResponse.builder().errorMessage(getMessage(ex)).executionStatus(FAILED).build();
    }
  }

  private void resizeAsgs(String region, AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String newAutoScalingGroupName, Integer newAsgFinalDesiredCount, List<AwsAmiResizeData> oldAsgsDesiredCounts,
      ExecutionLogCallback executionLogCallback, boolean resizeNewFirst, Integer autoScalingSteadyStateTimeout,
      int maxInstances, int minInstances, AwsAmiPreDeploymentData preDeploymentData) {
    if (isBlank(newAutoScalingGroupName) && isEmpty(oldAsgsDesiredCounts)) {
      throw new InvalidRequestException("At least one AutoScaling Group must be present");
    }
    if (resizeNewFirst) {
      if (isNotBlank(newAutoScalingGroupName)) {
        executionLogCallback.saveExecutionLog(format("Resizing AutoScaling Group [%s]", newAutoScalingGroupName));
        awsAsgHelperServiceDelegate.setAutoScalingGroupLimits(awsConfig, encryptionDetails, region,
            newAutoScalingGroupName, newAsgFinalDesiredCount, executionLogCallback);
        awsAsgHelperServiceDelegate.setAutoScalingGroupCapacityAndWaitForInstancesReadyState(awsConfig,
            encryptionDetails, region, newAutoScalingGroupName, newAsgFinalDesiredCount, executionLogCallback,
            autoScalingSteadyStateTimeout);
        if (newAsgFinalDesiredCount == maxInstances) {
          awsAsgHelperServiceDelegate.setMinInstancesForAsg(
              awsConfig, encryptionDetails, region, newAutoScalingGroupName, minInstances, executionLogCallback);
        }
      }
      if (isNotEmpty(oldAsgsDesiredCounts)) {
        oldAsgsDesiredCounts.forEach(count -> {
          executionLogCallback.saveExecutionLog(format("Resizing AutoScaling Group [%s]", count.getAsgName()));
          awsAsgHelperServiceDelegate.setAutoScalingGroupLimits(
              awsConfig, encryptionDetails, region, count.getAsgName(), count.getDesiredCount(), executionLogCallback);
          awsAsgHelperServiceDelegate.setAutoScalingGroupCapacityAndWaitForInstancesReadyState(awsConfig,
              encryptionDetails, region, count.getAsgName(), count.getDesiredCount(), executionLogCallback,
              autoScalingSteadyStateTimeout);
          if (preDeploymentData.hasAsgReachedPreDeploymentCount(count.getAsgName(), count.getDesiredCount())) {
            awsAsgHelperServiceDelegate.setMinInstancesForAsg(awsConfig, encryptionDetails, region, count.getAsgName(),
                preDeploymentData.getPreDeploymentMinCapacity(count.getAsgName()), executionLogCallback);
          }
        });
      }
    } else {
      if (isNotEmpty(oldAsgsDesiredCounts)) {
        oldAsgsDesiredCounts.forEach(count -> {
          executionLogCallback.saveExecutionLog(format("Resizing AutoScaling Group [%s]", count.getAsgName()));
          awsAsgHelperServiceDelegate.setAutoScalingGroupLimits(
              awsConfig, encryptionDetails, region, count.getAsgName(), count.getDesiredCount(), executionLogCallback);
          awsAsgHelperServiceDelegate.setAutoScalingGroupCapacityAndWaitForInstancesReadyState(awsConfig,
              encryptionDetails, region, count.getAsgName(), count.getDesiredCount(), executionLogCallback,
              autoScalingSteadyStateTimeout);
          if (preDeploymentData.hasAsgReachedPreDeploymentCount(count.getAsgName(), count.getDesiredCount())) {
            awsAsgHelperServiceDelegate.setMinInstancesForAsg(awsConfig, encryptionDetails, region, count.getAsgName(),
                preDeploymentData.getPreDeploymentMinCapacity(count.getAsgName()), executionLogCallback);
          }
        });
      }
      if (isNotBlank(newAutoScalingGroupName)) {
        executionLogCallback.saveExecutionLog(format("Resizing AutoScaling Group [%s]", newAutoScalingGroupName));
        awsAsgHelperServiceDelegate.setAutoScalingGroupLimits(awsConfig, encryptionDetails, region,
            newAutoScalingGroupName, newAsgFinalDesiredCount, executionLogCallback);
        awsAsgHelperServiceDelegate.setAutoScalingGroupCapacityAndWaitForInstancesReadyState(awsConfig,
            encryptionDetails, region, newAutoScalingGroupName, newAsgFinalDesiredCount, executionLogCallback,
            autoScalingSteadyStateTimeout);
        if (newAsgFinalDesiredCount == maxInstances) {
          awsAsgHelperServiceDelegate.setMinInstancesForAsg(
              awsConfig, encryptionDetails, region, newAutoScalingGroupName, minInstances, executionLogCallback);
        }
      }
    }
  }

  @Override
  public AwsAmiServiceSetupResponse setUpAmiService(
      AwsAmiServiceSetupRequest request, ExecutionLogCallback logCallback) {
    try {
      AwsConfig awsConfig = request.getAwsConfig();
      List<EncryptedDataDetail> encryptionDetails = request.getEncryptionDetails();
      encryptionService.decrypt(awsConfig, encryptionDetails);
      logCallback.saveExecutionLog("Starting AWS AMI Setup", INFO);

      logCallback.saveExecutionLog("Getting base auto scaling group");
      AutoScalingGroup baseAutoScalingGroup = ensureAndGetBaseAutoScalingGroup(
          awsConfig, encryptionDetails, request.getRegion(), request.getInfraMappingAsgName(), logCallback);

      logCallback.saveExecutionLog("Getting base launch configuration");
      LaunchConfiguration baseLaunchConfiguration = ensureAndGetBaseLaunchConfiguration(awsConfig, encryptionDetails,
          request.getRegion(), request.getInfraMappingAsgName(), baseAutoScalingGroup, logCallback);

      logCallback.saveExecutionLog("Getting all Harness managed autoscaling groups");
      List<AutoScalingGroup> harnessManagedAutoScalingGroups = listAllHarnessManagedAsgs(
          request.getAwsConfig(), request.getEncryptionDetails(), request.getRegion(), request.getInfraMappingId());

      logCallback.saveExecutionLog("Getting last deployed autoscaling group with non zero capacity");
      String lastDeployedAsgName = getLastDeployedAsgNameWithNonZeroCapacity(harnessManagedAutoScalingGroups);

      Integer harnessRevision = getNewHarnessVersion(harnessManagedAutoScalingGroups);
      String region = request.getRegion();
      String newAutoScalingGroupName = AsgConvention.getAsgName(request.getNewAsgNamePrefix(), harnessRevision);
      Integer maxInstances = request.getMaxInstances();

      LaunchConfiguration oldLaunchConfiguration = awsAsgHelperServiceDelegate.getLaunchConfiguration(
          awsConfig, encryptionDetails, region, newAutoScalingGroupName);
      if (oldLaunchConfiguration != null) {
        logCallback.saveExecutionLog(
            format("Deleting old launch configuration [%s]", oldLaunchConfiguration.getLaunchConfigurationName()));
        awsAsgHelperServiceDelegate.deleteLaunchConfig(awsConfig, encryptionDetails, region, newAutoScalingGroupName);
      }

      logCallback.saveExecutionLog(
          format("Creating new launch configuration [%s]", baseLaunchConfiguration.getLaunchConfigurationName()));
      awsAsgHelperServiceDelegate.createLaunchConfiguration(awsConfig, encryptionDetails, region,
          createNewLaunchConfigurationRequest(
              request.getArtifactRevision(), baseLaunchConfiguration, newAutoScalingGroupName, request.getUserData()));

      logCallback.saveExecutionLog(format("Creating new AutoScalingGroup [%s]", newAutoScalingGroupName));
      awsAsgHelperServiceDelegate.createAutoScalingGroup(awsConfig, encryptionDetails, region,
          createNewAutoScalingGroupRequest(request.getInfraMappingId(), request.getInfraMappingClassisLbs(),
              request.getInfraMappingTargetGroupArns(), newAutoScalingGroupName, baseAutoScalingGroup, harnessRevision,
              maxInstances),
          logCallback);

      AwsAmiServiceSetupResponseBuilder builder = AwsAmiServiceSetupResponse.builder()
                                                      .executionStatus(SUCCESS)
                                                      .lastDeployedAsgName(lastDeployedAsgName)
                                                      .newAsgName(newAutoScalingGroupName)
                                                      .harnessRevision(harnessRevision);
      populatePreDeploymentData(harnessManagedAutoScalingGroups, builder);

      logCallback.saveExecutionLog("Sending request to delete old auto scaling groups to executor");
      deleteOldHarnessManagedAutoScalingGroups(
          encryptionDetails, region, awsConfig, harnessManagedAutoScalingGroups, lastDeployedAsgName, logCallback);
      logCallback.saveExecutionLog(
          format("Completed AWS AMI Setup with new autoScalingGroupName [%s]", newAutoScalingGroupName));

      return builder.build();

    } catch (Exception exception) {
      logCallback.saveExecutionLog(format("Exception: [%s].", exception.getMessage()), ERROR);
      logger.error(exception.getMessage(), exception);
      return AwsAmiServiceSetupResponse.builder().errorMessage(getMessage(exception)).executionStatus(FAILED).build();
    }
  }

  private void populatePreDeploymentData(
      List<AutoScalingGroup> harnessManagedAutoScalingGroups, AwsAmiServiceSetupResponseBuilder builder) {
    Map<String, Integer> minCapacityMap = Maps.newHashMap();
    Map<String, Integer> desiredCapacityMap = Maps.newHashMap();
    if (isNotEmpty(harnessManagedAutoScalingGroups)) {
      harnessManagedAutoScalingGroups.forEach(group -> {
        if (group.getDesiredCapacity() > 0) {
          minCapacityMap.put(group.getAutoScalingGroupName(), group.getMinSize());
          desiredCapacityMap.put(group.getAutoScalingGroupName(), group.getDesiredCapacity());
        }
      });
    }
    builder.preDeploymentData(AwsAmiPreDeploymentData.builder()
                                  .asgNameToDesiredCapacity(desiredCapacityMap)
                                  .asgNameToMinCapacity(minCapacityMap)
                                  .build());
    builder.oldAsgNames(com.google.common.collect.Lists.reverse(harnessManagedAutoScalingGroups.stream()
                                                                    .filter(asg -> asg.getDesiredCapacity() > 0)
                                                                    .map(AutoScalingGroup::getAutoScalingGroupName)
                                                                    .collect(toList())));
  }

  private void deleteOldHarnessManagedAutoScalingGroups(List<EncryptedDataDetail> encryptionDetails, String region,
      AwsConfig awsConfig, List<AutoScalingGroup> harnessAutoScalingGroups, String oldAutoScalingGroupName,
      ExecutionLogCallback logCallback) {
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
                                   -> awsAsgHelperServiceDelegate.deleteAutoScalingGroups(awsConfig, encryptionDetails,
                                       region, finalEmptyHarnessAsgToBeDeleted, logCallback));
      }
    } catch (Exception ex) {
      logger.error(
          "Error in deleting old autoScaling groups [{}] [{}]", Joiner.on(",").join(harnessAutoScalingGroups), ex);
    }
  }

  private CreateAutoScalingGroupRequest createNewAutoScalingGroupRequest(String infraMappingId,
      List<String> infraMappingClassisLbs, List<String> infraMappingTargetGroupArns, String newAutoScalingGroupName,
      AutoScalingGroup baseAutoScalingGroup, Integer harnessRevision, Integer maxInstances) {
    List<Tag> tags =
        baseAutoScalingGroup.getTags()
            .stream()
            .filter(tagDescription
                -> !Arrays.asList(HARNESS_AUTOSCALING_GROUP_TAG, NAME_TAG).contains(tagDescription.getKey()))
            .map(tagDescription
                -> new Tag()
                       .withKey(tagDescription.getKey())
                       .withValue(tagDescription.getValue())
                       .withPropagateAtLaunch(tagDescription.getPropagateAtLaunch())
                       .withResourceType(tagDescription.getResourceType()))
            .collect(toList());
    tags.add(new Tag()
                 .withKey(HARNESS_AUTOSCALING_GROUP_TAG)
                 .withValue(AsgConvention.getRevisionTagValue(infraMappingId, harnessRevision))
                 .withPropagateAtLaunch(true)
                 .withResourceType(AUTOSCALING_GROUP_RESOURCE_TYPE));
    tags.add(new Tag().withKey(NAME_TAG).withValue(newAutoScalingGroupName).withPropagateAtLaunch(true));

    CreateAutoScalingGroupRequest createAutoScalingGroupRequest =
        new CreateAutoScalingGroupRequest()
            .withAutoScalingGroupName(newAutoScalingGroupName)
            .withLaunchConfigurationName(newAutoScalingGroupName)
            .withDesiredCapacity(0)
            .withMinSize(0)
            .withMaxSize(maxInstances)
            .withTags(tags)
            .withDefaultCooldown(baseAutoScalingGroup.getDefaultCooldown())
            .withAvailabilityZones(baseAutoScalingGroup.getAvailabilityZones())
            .withTerminationPolicies(baseAutoScalingGroup.getTerminationPolicies())
            .withNewInstancesProtectedFromScaleIn(baseAutoScalingGroup.getNewInstancesProtectedFromScaleIn());

    if (!Lists.isNullOrEmpty(infraMappingClassisLbs)) {
      createAutoScalingGroupRequest.setLoadBalancerNames(infraMappingClassisLbs);
    }

    if (!Lists.isNullOrEmpty(infraMappingTargetGroupArns)) {
      createAutoScalingGroupRequest.setTargetGroupARNs(infraMappingTargetGroupArns);
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

  private CreateLaunchConfigurationRequest createNewLaunchConfigurationRequest(String artifactRevision,
      LaunchConfiguration cloneBaseLaunchConfiguration, String newAutoScalingGroupName, String userData) {
    CreateLaunchConfigurationRequest createLaunchConfigurationRequest =
        new CreateLaunchConfigurationRequest()
            .withLaunchConfigurationName(newAutoScalingGroupName)
            .withImageId(artifactRevision)
            .withSecurityGroups(cloneBaseLaunchConfiguration.getSecurityGroups())
            .withClassicLinkVPCId(cloneBaseLaunchConfiguration.getClassicLinkVPCId())
            .withBlockDeviceMappings(cloneBaseLaunchConfiguration.getBlockDeviceMappings())
            .withEbsOptimized(cloneBaseLaunchConfiguration.getEbsOptimized())
            .withAssociatePublicIpAddress(cloneBaseLaunchConfiguration.getAssociatePublicIpAddress());

    if (isNotEmpty(userData)) {
      createLaunchConfigurationRequest.setUserData(userData);
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

  private List<AutoScalingGroup> listAllHarnessManagedAsgs(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String infraMappingId) {
    List<AutoScalingGroup> scalingGroups =
        awsAsgHelperServiceDelegate.listAllAsgs(awsConfig, encryptionDetails, region);
    return scalingGroups.stream()
        .filter(autoScalingGroup
            -> autoScalingGroup.getTags().stream().anyMatch(
                tagDescription -> isHarnessManagedTag(infraMappingId, tagDescription)))
        .sorted(Comparator.comparing(AutoScalingGroup::getCreatedTime).reversed())
        .collect(toList());
  }

  private boolean isHarnessManagedTag(String infraMappingId, TagDescription tagDescription) {
    return tagDescription.getKey().equals(HARNESS_AUTOSCALING_GROUP_TAG)
        && tagDescription.getValue().startsWith(infraMappingId);
  }

  private LaunchConfiguration ensureAndGetBaseLaunchConfiguration(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region, String autoScalingGroupName,
      AutoScalingGroup baseAutoScalingGroup, ExecutionLogCallback logCallback) {
    LaunchConfiguration baseAutoScalingGroupLaunchConfiguration = awsAsgHelperServiceDelegate.getLaunchConfiguration(
        awsConfig, encryptionDetails, region, baseAutoScalingGroup.getLaunchConfigurationName());

    if (baseAutoScalingGroupLaunchConfiguration == null) {
      String errorMessage = format(
          "LaunchConfiguration [%s] for referenced AutoScaling Group [%s] provided in Service Infrastructure couldn't be found in AWS region [%s]",
          baseAutoScalingGroup.getAutoScalingGroupName(), autoScalingGroupName, region);
      logCallback.saveExecutionLog(errorMessage, ERROR);
      throw new InvalidRequestException(errorMessage);
    }
    return baseAutoScalingGroupLaunchConfiguration;
  }

  private AutoScalingGroup ensureAndGetBaseAutoScalingGroup(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region, String baseAutoScalingGroupName,
      ExecutionLogCallback logCallback) {
    AutoScalingGroup baseAutoScalingGroup =
        awsAsgHelperServiceDelegate.getAutoScalingGroup(awsConfig, encryptionDetails, region, baseAutoScalingGroupName);
    if (baseAutoScalingGroup == null) {
      String errorMessage =
          format("Couldn't find reference AutoScalingGroup: [%s] in region: [%s]", baseAutoScalingGroupName, region);
      logCallback.saveExecutionLog(errorMessage, ERROR);
      logger.error(errorMessage);
      throw new InvalidRequestException(errorMessage);
    }
    return baseAutoScalingGroup;
  }
}