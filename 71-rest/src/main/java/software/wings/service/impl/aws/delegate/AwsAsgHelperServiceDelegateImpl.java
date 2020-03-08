package software.wings.service.impl.aws.delegate;

import static com.google.common.collect.Lists.newArrayList;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.INIT_TIMEOUT;
import static io.harness.threading.Morpheus.sleep;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.service.impl.aws.model.AwsConstants.DEFAULT_AMI_ASG_DESIRED_INSTANCES;
import static software.wings.service.impl.aws.model.AwsConstants.DEFAULT_AMI_ASG_MAX_INSTANCES;
import static software.wings.service.impl.aws.model.AwsConstants.DEFAULT_AMI_ASG_MIN_INSTANCES;
import static software.wings.service.impl.aws.model.AwsConstants.DEFAULT_AMI_ASG_NAME;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder;
import com.amazonaws.services.autoscaling.model.Activity;
import com.amazonaws.services.autoscaling.model.AttachLoadBalancerTargetGroupsRequest;
import com.amazonaws.services.autoscaling.model.AttachLoadBalancersRequest;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupResult;
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationResult;
import com.amazonaws.services.autoscaling.model.DeleteAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.DeleteLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.DeletePolicyRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.DescribeLaunchConfigurationsRequest;
import com.amazonaws.services.autoscaling.model.DescribePoliciesRequest;
import com.amazonaws.services.autoscaling.model.DescribePoliciesResult;
import com.amazonaws.services.autoscaling.model.DescribeScalingActivitiesRequest;
import com.amazonaws.services.autoscaling.model.DescribeScalingActivitiesResult;
import com.amazonaws.services.autoscaling.model.DetachLoadBalancerTargetGroupsRequest;
import com.amazonaws.services.autoscaling.model.DetachLoadBalancersRequest;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.amazonaws.services.autoscaling.model.PutScalingPolicyRequest;
import com.amazonaws.services.autoscaling.model.PutScalingPolicyResult;
import com.amazonaws.services.autoscaling.model.ScalingPolicy;
import com.amazonaws.services.autoscaling.model.SetDesiredCapacityRequest;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AwsConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.command.LogCallback;
import software.wings.service.impl.aws.model.AwsAsgGetRunningCountData;
import software.wings.service.intfc.aws.delegate.AwsAsgHelperServiceDelegate;
import software.wings.service.intfc.aws.delegate.AwsEc2HelperServiceDelegate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Singleton
@Slf4j
public class AwsAsgHelperServiceDelegateImpl
    extends AwsHelperServiceDelegateBase implements AwsAsgHelperServiceDelegate {
  private static final long AUTOSCALING_REQUEST_STATUS_CHECK_INTERVAL = TimeUnit.SECONDS.toSeconds(15);
  @Inject private AwsEc2HelperServiceDelegate awsEc2HelperServiceDelegate;
  @Inject private TimeLimiter timeLimiter;

  @VisibleForTesting
  AmazonAutoScalingClient getAmazonAutoScalingClient(Regions region, AwsConfig awsConfig) {
    AmazonAutoScalingClientBuilder builder = AmazonAutoScalingClientBuilder.standard().withRegion(region);
    attachCredentials(builder, awsConfig);
    return (AmazonAutoScalingClient) builder.build();
  }

  @Override
  public List<String> listAutoScalingGroupNames(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    try {
      List<AutoScalingGroup> result = listAllAsgs(awsConfig, encryptionDetails, region);
      return result.stream().map(AutoScalingGroup::getAutoScalingGroupName).collect(toList());
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return emptyList();
  }

  @Override
  public List<String> listAutoScalingGroupInstanceIds(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String autoScalingGroupName) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      DescribeAutoScalingGroupsRequest describeAutoScalingGroupsRequest =
          new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(autoScalingGroupName);
      AmazonAutoScalingClient amazonAutoScalingClient = getAmazonAutoScalingClient(Regions.fromName(region), awsConfig);
      tracker.trackASGCall("Describe ASGs");
      DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult =
          amazonAutoScalingClient.describeAutoScalingGroups(describeAutoScalingGroupsRequest);
      if (describeAutoScalingGroupsResult.getAutoScalingGroups().isEmpty()) {
        return emptyList();
      }
      AutoScalingGroup autoScalingGroup = describeAutoScalingGroupsResult.getAutoScalingGroups().get(0);
      return autoScalingGroup.getInstances()
          .stream()
          .map(com.amazonaws.services.autoscaling.model.Instance::getInstanceId)
          .collect(toList());
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return emptyList();
  }

  @Override
  public List<Instance> listAutoScalingGroupInstances(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String autoScalingGroupName) {
    List<String> instanceIds =
        listAutoScalingGroupInstanceIds(awsConfig, encryptionDetails, region, autoScalingGroupName);
    return awsEc2HelperServiceDelegate.listEc2Instances(awsConfig, encryptionDetails, instanceIds, region);
  }

  @Override
  public AutoScalingGroup getAutoScalingGroup(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String autoScalingGroupName) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      DescribeAutoScalingGroupsRequest describeAutoScalingGroupsRequest =
          new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(autoScalingGroupName);
      AmazonAutoScalingClient amazonAutoScalingClient = getAmazonAutoScalingClient(Regions.fromName(region), awsConfig);
      tracker.trackASGCall("Describe Autoscaling Group");
      DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult =
          amazonAutoScalingClient.describeAutoScalingGroups(describeAutoScalingGroupsRequest);
      if (!describeAutoScalingGroupsResult.getAutoScalingGroups().isEmpty()) {
        return describeAutoScalingGroupsResult.getAutoScalingGroups().get(0);
      }
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return null;
  }

  @Override
  public LaunchConfiguration getLaunchConfiguration(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String launchConfigName) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonAutoScalingClient amazonAutoScalingClient = getAmazonAutoScalingClient(Regions.fromName(region), awsConfig);
      tracker.trackASGCall("Describe Launch Configuration");
      return amazonAutoScalingClient
          .describeLaunchConfigurations(
              new DescribeLaunchConfigurationsRequest().withLaunchConfigurationNames(launchConfigName))
          .getLaunchConfigurations()
          .stream()
          .findFirst()
          .orElse(null);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return null;
  }

  @Override
  public List<AutoScalingGroup> listAllAsgs(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonAutoScalingClient amazonAutoScalingClient = getAmazonAutoScalingClient(Regions.fromName(region), awsConfig);

      DescribeAutoScalingGroupsRequest describeAutoScalingGroupsRequest;
      DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult;
      String nextToken = null;
      List<AutoScalingGroup> result = new ArrayList<>();
      do {
        describeAutoScalingGroupsRequest =
            new DescribeAutoScalingGroupsRequest().withMaxRecords(100).withNextToken(nextToken);
        tracker.trackASGCall("Describe Autoscaling Group");
        describeAutoScalingGroupsResult =
            amazonAutoScalingClient.describeAutoScalingGroups(describeAutoScalingGroupsRequest);
        result.addAll(describeAutoScalingGroupsResult.getAutoScalingGroups());
        nextToken = describeAutoScalingGroupsResult.getNextToken();
      } while (nextToken != null);
      return result;
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return emptyList();
  }

  @Override
  public void deleteLaunchConfig(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String autoScalingGroupName) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonAutoScalingClient amazonAutoScalingClient = getAmazonAutoScalingClient(Regions.fromName(region), awsConfig);
      tracker.trackASGCall("Delete Launch Config");
      amazonAutoScalingClient.deleteLaunchConfiguration(
          new DeleteLaunchConfigurationRequest().withLaunchConfigurationName(autoScalingGroupName));
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
  }

  @Override
  public CreateLaunchConfigurationResult createLaunchConfiguration(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region,
      CreateLaunchConfigurationRequest createLaunchConfigurationRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      tracker.trackASGCall("Create Launch Config");
      AmazonAutoScalingClient amazonAutoScalingClient = getAmazonAutoScalingClient(Regions.fromName(region), awsConfig);
      return amazonAutoScalingClient.createLaunchConfiguration(createLaunchConfigurationRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return new CreateLaunchConfigurationResult();
  }

  @Override
  public CreateAutoScalingGroupResult createAutoScalingGroup(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region,
      CreateAutoScalingGroupRequest createAutoScalingGroupRequest, LogCallback logCallback) {
    AmazonAutoScalingClient amazonAutoScalingClient = null;
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      amazonAutoScalingClient = getAmazonAutoScalingClient(Regions.fromName(region), awsConfig);
      tracker.trackASGCall("Create Autoscaling Group");
      return amazonAutoScalingClient.createAutoScalingGroup(createAutoScalingGroupRequest);
    } catch (AmazonServiceException amazonServiceException) {
      if (amazonAutoScalingClient != null && logCallback != null) {
        describeAutoScalingGroupActivities(amazonAutoScalingClient,
            createAutoScalingGroupRequest.getAutoScalingGroupName(), new HashSet<>(), logCallback, true);
      }
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return new CreateAutoScalingGroupResult();
  }

  @Override
  public void deleteAutoScalingGroups(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      List<AutoScalingGroup> autoScalingGroups, LogCallback callback) {
    AmazonAutoScalingClient amazonAutoScalingClient = getAmazonAutoScalingClient(Regions.fromName(region), awsConfig);
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      autoScalingGroups.forEach(autoScalingGroup -> {
        try {
          tracker.trackASGCall("Delete Autoscaling Group");
          amazonAutoScalingClient.deleteAutoScalingGroup(
              new DeleteAutoScalingGroupRequest().withAutoScalingGroupName(autoScalingGroup.getAutoScalingGroupName()));
          waitForAutoScalingGroupToBeDeleted(amazonAutoScalingClient, autoScalingGroup, callback);
        } catch (Exception ignored) {
          describeAutoScalingGroupActivities(
              amazonAutoScalingClient, autoScalingGroup.getAutoScalingGroupName(), new HashSet<>(), callback, true);
          logger.warn("Failed to delete ASG: [{}] [{}]", autoScalingGroup.getAutoScalingGroupName(), ignored);
        }
        if (isNotEmpty(autoScalingGroup.getLaunchConfigurationName())) {
          try {
            tracker.trackASGCall("Delete Launch Config");
            amazonAutoScalingClient.deleteLaunchConfiguration(
                new DeleteLaunchConfigurationRequest().withLaunchConfigurationName(
                    autoScalingGroup.getLaunchConfigurationName()));
          } catch (Exception ignored) {
            describeAutoScalingGroupActivities(
                amazonAutoScalingClient, autoScalingGroup.getAutoScalingGroupName(), new HashSet<>(), callback, true);
            logger.warn("Failed to delete ASG: [{}] [{}]", autoScalingGroup.getAutoScalingGroupName(), ignored);
          }
        }
      });
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
  }

  @Override
  public Map<String, Integer> getDesiredCapacitiesOfAsgs(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, List<String> asgs) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      Map<String, Integer> capacities = new HashMap<>();
      AmazonAutoScalingClient amazonAutoScalingClient = getAmazonAutoScalingClient(Regions.fromName(region), awsConfig);
      String nextToken = null;
      DescribeAutoScalingGroupsRequest request = null;
      DescribeAutoScalingGroupsResult result = null;
      do {
        request = new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(asgs).withNextToken(nextToken);
        tracker.trackASGCall("Describe ASGs");
        result = amazonAutoScalingClient.describeAutoScalingGroups(request);
        List<AutoScalingGroup> groups = result.getAutoScalingGroups();
        groups.forEach(group -> { capacities.put(group.getAutoScalingGroupName(), group.getDesiredCapacity()); });
        nextToken = result.getNextToken();
      } while (nextToken != null);
      return capacities;
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return emptyMap();
  }

  @Override
  public void setAutoScalingGroupCapacityAndWaitForInstancesReadyState(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region, String autoScalingGroupName, Integer desiredCapacity,
      ExecutionLogCallback logCallback, Integer autoScalingSteadyStateTimeout) {
    encryptionService.decrypt(awsConfig, encryptionDetails);
    AmazonAutoScalingClient amazonAutoScalingClient = getAmazonAutoScalingClient(Regions.fromName(region), awsConfig);
    try {
      logCallback.saveExecutionLog(
          format("Set AutoScaling Group: [%s] desired capacity to [%s]", autoScalingGroupName, desiredCapacity));
      tracker.trackASGCall("Set ASG Desired Capacity");
      amazonAutoScalingClient.setDesiredCapacity(new SetDesiredCapacityRequest()
                                                     .withAutoScalingGroupName(autoScalingGroupName)
                                                     .withDesiredCapacity(desiredCapacity));
      logCallback.saveExecutionLog("Successfully set desired capacity");
      waitForAllInstancesToBeReady(awsConfig, encryptionDetails, region, autoScalingGroupName, desiredCapacity,
          logCallback, autoScalingSteadyStateTimeout);
    } catch (AmazonServiceException amazonServiceException) {
      describeAutoScalingGroupActivities(
          amazonAutoScalingClient, autoScalingGroupName, new HashSet<>(), logCallback, true);
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
  }

  @Override
  public void setMinInstancesForAsg(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      String autoScalingGroupName, int minCapacity, ExecutionLogCallback logCallback) {
    try {
      if (isEmpty(autoScalingGroupName)) {
        return;
      }
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonAutoScalingClient amazonAutoScalingClient = getAmazonAutoScalingClient(Regions.fromName(region), awsConfig);
      tracker.trackASGCall("Describe Autoscaling Group");
      DescribeAutoScalingGroupsRequest request =
          new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(autoScalingGroupName);
      DescribeAutoScalingGroupsResult result = amazonAutoScalingClient.describeAutoScalingGroups(request);
      List<AutoScalingGroup> autoScalingGroups = result.getAutoScalingGroups();
      if (isEmpty(autoScalingGroups)) {
        return;
      }
      logCallback.saveExecutionLog(
          format("Setting min capacity of Asg[%s] to [%d]", autoScalingGroupName, minCapacity));
      UpdateAutoScalingGroupRequest updateAutoScalingGroupRequest =
          new UpdateAutoScalingGroupRequest().withAutoScalingGroupName(autoScalingGroupName).withMinSize(minCapacity);
      tracker.trackASGCall("Update Autoscaling Group");
      amazonAutoScalingClient.updateAutoScalingGroup(updateAutoScalingGroupRequest);
    } catch (AmazonServiceException amazonServiceException) {
      logCallback.saveExecutionLog(
          format("Exception: [%s] while setting Asg limits", ExceptionUtils.getMessage(amazonServiceException)));
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
  }

  @Override
  public void setAutoScalingGroupLimits(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      String autoScalingGroupName, Integer desiredCapacity, ExecutionLogCallback logCallback) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonAutoScalingClient amazonAutoScalingClient = getAmazonAutoScalingClient(Regions.fromName(region), awsConfig);
      DescribeAutoScalingGroupsRequest request =
          new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(autoScalingGroupName);
      tracker.trackASGCall("Describe Autoscaling Groups");
      DescribeAutoScalingGroupsResult result = amazonAutoScalingClient.describeAutoScalingGroups(request);
      List<AutoScalingGroup> autoScalingGroups = result.getAutoScalingGroups();
      if (isEmpty(autoScalingGroups)) {
        return;
      }
      AutoScalingGroup autoScalingGroup = autoScalingGroups.get(0);
      if (desiredCapacity < autoScalingGroup.getMinSize()) {
        logCallback.saveExecutionLog(
            format("Autoscaling group: [%s] has min Size: [%d] > desired capacity: [%d]. Updating",
                autoScalingGroupName, autoScalingGroup.getMinSize(), desiredCapacity));
        UpdateAutoScalingGroupRequest updateAutoScalingGroupRequest =
            new UpdateAutoScalingGroupRequest()
                .withAutoScalingGroupName(autoScalingGroupName)
                .withMinSize(desiredCapacity);
        tracker.trackASGCall("Update Autoscaling Group");
        amazonAutoScalingClient.updateAutoScalingGroup(updateAutoScalingGroupRequest);
      } else if (desiredCapacity > autoScalingGroup.getMaxSize()) {
        logCallback.saveExecutionLog(
            format("Autoscaling group: [%s] has max Size: [%d] < desired capacity: [%d]. Updating",
                autoScalingGroupName, autoScalingGroup.getMaxSize(), desiredCapacity));
        UpdateAutoScalingGroupRequest updateAutoScalingGroupRequest =
            new UpdateAutoScalingGroupRequest()
                .withAutoScalingGroupName(autoScalingGroupName)
                .withMaxSize(desiredCapacity);
        tracker.trackASGCall("Update Autoscaling Group");
        amazonAutoScalingClient.updateAutoScalingGroup(updateAutoScalingGroupRequest);
      }
    } catch (AmazonServiceException amazonServiceException) {
      logCallback.saveExecutionLog(
          format("Exception: [%s] while setting Asg limits", ExceptionUtils.getMessage(amazonServiceException)));
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
  }

  private void waitForAutoScalingGroupToBeDeleted(
      AmazonAutoScalingClient amazonAutoScalingClient, AutoScalingGroup autoScalingGroup, LogCallback callback) {
    try {
      timeLimiter.callWithTimeout(() -> {
        Set<String> completedActivities = new HashSet<>();
        while (true) {
          tracker.trackASGCall("Describe Autoscaling Group");
          DescribeAutoScalingGroupsResult result = amazonAutoScalingClient.describeAutoScalingGroups(
              new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(
                  autoScalingGroup.getAutoScalingGroupName()));
          if (result.getAutoScalingGroups().isEmpty()) {
            return true;
          }
          describeAutoScalingGroupActivities(amazonAutoScalingClient, autoScalingGroup.getAutoScalingGroupName(),
              completedActivities, callback, false);
          sleep(ofSeconds(AUTOSCALING_REQUEST_STATUS_CHECK_INTERVAL));
        }
      }, 1L, TimeUnit.MINUTES, true);
    } catch (UncheckedTimeoutException e) {
      throw new WingsException(INIT_TIMEOUT)
          .addParam("message", "Timed out waiting for autoscaling group to be deleted");
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException("Error while waiting for autoscaling group to be deleted", e);
    }
  }

  private void describeAutoScalingGroupActivities(AmazonAutoScalingClient amazonAutoScalingClient,
      String autoScalingGroupName, Set<String> completedActivities, LogCallback callback, boolean withCause) {
    if (callback == null) {
      logger.info("Not describing autoScalingGroupActivities for {} since logCallback is null", completedActivities);
      return;
    }
    try {
      tracker.trackASGCall("Describe ASG activities");
      DescribeScalingActivitiesResult activitiesResult = amazonAutoScalingClient.describeScalingActivities(
          new DescribeScalingActivitiesRequest().withAutoScalingGroupName(autoScalingGroupName));
      List<Activity> activities = activitiesResult.getActivities();
      if (activities != null && activities.size() > 0) {
        activities.stream()
            .filter(activity -> !completedActivities.contains(activity.getActivityId()))
            .forEach(activity -> {
              String activityId = activity.getActivityId();
              String details = activity.getDetails();
              Integer progress = activity.getProgress();
              String activityDescription = activity.getDescription();
              String statuscode = activity.getStatusCode();
              String logStatement =
                  format("AutoScalingGroup [%s] activity [%s] progress [%d percent] , statuscode [%s]  details [%s]",
                      autoScalingGroupName, activityDescription, progress, statuscode, details);
              if (withCause) {
                String cause = activity.getCause();
                logStatement = format(logStatement + " cause [%s]", cause);
              }

              callback.saveExecutionLog(logStatement);
              if (progress == 100) {
                completedActivities.add(activityId);
              }
            });
      }
    } catch (Exception e) {
      logger.warn("Failed to describe autoScalingGroup for [%s]", autoScalingGroupName, e);
    }
  }

  private void waitForAllInstancesToBeReady(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, String autoScalingGroupName, Integer desiredCount, ExecutionLogCallback logCallback,
      Integer autoScalingSteadyStateTimeout) {
    try {
      timeLimiter.callWithTimeout(() -> {
        AmazonAutoScalingClient amazonAutoScalingClient =
            getAmazonAutoScalingClient(Regions.fromName(region), awsConfig);
        Set<String> completedActivities = new HashSet<>();
        while (true) {
          List<String> instanceIds =
              listAutoScalingGroupInstanceIds(awsConfig, encryptionDetails, region, autoScalingGroupName);
          describeAutoScalingGroupActivities(
              amazonAutoScalingClient, autoScalingGroupName, completedActivities, logCallback, false);
          if (instanceIds.size() == desiredCount
              && allInstanceInReadyState(awsConfig, encryptionDetails, region, instanceIds, logCallback)) {
            return true;
          }
          sleep(ofSeconds(AUTOSCALING_REQUEST_STATUS_CHECK_INTERVAL));
        }
      }, autoScalingSteadyStateTimeout, TimeUnit.MINUTES, true);
    } catch (UncheckedTimeoutException e) {
      logCallback.saveExecutionLog("Request timeout. AutoScaling group couldn't reach steady state", ERROR);
      throw new WingsException(INIT_TIMEOUT)
          .addParam("message", "Timed out waiting for all instances to be in running state");
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException("Error while waiting for all instances to be in running state", e);
    }
    logCallback.saveExecutionLog("AutoScaling group reached steady state");
  }

  @VisibleForTesting
  boolean allInstanceInReadyState(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      List<String> instanceIds, ExecutionLogCallback logCallback) {
    List<Instance> instances =
        awsEc2HelperServiceDelegate.listEc2Instances(awsConfig, encryptionDetails, instanceIds, region);
    boolean allRunning = instanceIds.isEmpty()
        || instances.stream().allMatch(instance -> instance.getState().getName().equals("running"));
    if (!allRunning) {
      Map<String, Long> instanceStateCountMap =
          instances.stream().collect(groupingBy(instance -> instance.getState().getName(), counting()));
      logCallback.saveExecutionLog("Waiting for instances to be in running state. "
          + Joiner.on(",").withKeyValueSeparator("=").join(instanceStateCountMap));
    }
    return allRunning;
  }

  @Override
  public void registerAsgWithTargetGroups(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, String asgName, List<String> targetGroupARNs, ExecutionLogCallback logCallback) {
    try {
      if (isEmpty(targetGroupARNs)) {
        logCallback.saveExecutionLog(format("No Target Groups to attach to: [%s]", asgName));
        return;
      }
      AmazonAutoScalingClient amazonAutoScalingClient = getAmazonAutoScalingClient(Regions.fromName(region), awsConfig);
      tracker.trackASGCall("Attach LB Target Groups");
      AttachLoadBalancerTargetGroupsRequest request =
          new AttachLoadBalancerTargetGroupsRequest().withAutoScalingGroupName(asgName).withTargetGroupARNs(
              targetGroupARNs);
      amazonAutoScalingClient.attachLoadBalancerTargetGroups(request);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
  }

  @Override
  public void registerAsgWithClassicLBs(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      String asgName, List<String> classicLBs, ExecutionLogCallback logCallback) {
    try {
      if (isEmpty(classicLBs)) {
        logCallback.saveExecutionLog(format("No classic load balancers to attach to: [%s]", asgName));
        return;
      }
      AmazonAutoScalingClient amazonAutoScalingClient = getAmazonAutoScalingClient(Regions.fromName(region), awsConfig);
      AttachLoadBalancersRequest request =
          new AttachLoadBalancersRequest().withAutoScalingGroupName(asgName).withLoadBalancerNames(classicLBs);
      tracker.trackASGCall("Attach Load Balancers");
      amazonAutoScalingClient.attachLoadBalancers(request);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
  }

  @Override
  public void deRegisterAsgWithTargetGroups(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, String asgName, List<String> targetGroupARNs, ExecutionLogCallback logCallback) {
    try {
      if (isEmpty(targetGroupARNs)) {
        logCallback.saveExecutionLog(format("No Target Groups to attach to: [%s]", asgName));
        return;
      }
      AmazonAutoScalingClient amazonAutoScalingClient = getAmazonAutoScalingClient(Regions.fromName(region), awsConfig);
      DetachLoadBalancerTargetGroupsRequest request =
          new DetachLoadBalancerTargetGroupsRequest().withAutoScalingGroupName(asgName).withTargetGroupARNs(
              targetGroupARNs);
      tracker.trackASGCall("Detach Load Balancer Target Groups");
      amazonAutoScalingClient.detachLoadBalancerTargetGroups(request);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
  }

  @Override
  public void deRegisterAsgWithClassicLBs(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, String asgName, List<String> classicLBs, ExecutionLogCallback logCallback) {
    try {
      if (isEmpty(classicLBs)) {
        logCallback.saveExecutionLog(format("No classic load balancers to detach to: [%s]", asgName));
        return;
      }
      tracker.trackASGCall("Detach Load Balancers");
      AmazonAutoScalingClient amazonAutoScalingClient = getAmazonAutoScalingClient(Regions.fromName(region), awsConfig);
      DetachLoadBalancersRequest request =
          new DetachLoadBalancersRequest().withAutoScalingGroupName(asgName).withLoadBalancerNames(classicLBs);
      amazonAutoScalingClient.detachLoadBalancers(request);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
  }

  @Override
  public AwsAsgGetRunningCountData getCurrentlyRunningInstanceCount(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String infraMappingId) {
    try {
      String asgName = DEFAULT_AMI_ASG_NAME;
      int asgMin = DEFAULT_AMI_ASG_MIN_INSTANCES;
      int asgMax = DEFAULT_AMI_ASG_MAX_INSTANCES;
      int asgDesired = DEFAULT_AMI_ASG_DESIRED_INSTANCES;
      List<AutoScalingGroup> groups = listAllAsgs(awsConfig, encryptionDetails, region);
      if (isNotEmpty(groups)) {
        Optional<AutoScalingGroup> first =
            groups.stream()
                .filter(group
                    -> group.getTags().stream().anyMatch(
                        tagDescription -> isHarnessManagedTag(infraMappingId, tagDescription)))
                .filter(group -> group.getDesiredCapacity() > 0)
                .max(Comparator.comparing(AutoScalingGroup::getCreatedTime));
        if (first.isPresent()) {
          AutoScalingGroup autoScalingGroup = first.get();
          asgName = autoScalingGroup.getAutoScalingGroupName();
          asgMin = autoScalingGroup.getMinSize();
          asgMax = autoScalingGroup.getMaxSize();
          asgDesired = autoScalingGroup.getDesiredCapacity();
        }
      }
      return AwsAsgGetRunningCountData.builder()
          .asgMin(asgMin)
          .asgMax(asgMax)
          .asgDesired(asgDesired)
          .asgName(asgName)
          .build();
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return null;
  }

  private String getJSONForScalingPolicy(ScalingPolicy scalingPolicy, ExecutionLogCallback logCallback) {
    if (scalingPolicy == null) {
      return EMPTY;
    }
    ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.writeValueAsString(scalingPolicy);
    } catch (Exception ex) {
      String errorMessage = format("Exception: [%s] while extracting policy JSON for scaling policy: [%s]. Ignored.",
          ex.getMessage(), scalingPolicy.getPolicyARN());
      logger.error(errorMessage, ex);
      logCallback.saveExecutionLog(errorMessage);
      return EMPTY;
    }
  }

  private List<ScalingPolicy> listAllScalingPoliciesOfAsg(
      AmazonAutoScalingClient amazonAutoScalingClient, String asgName) {
    List<ScalingPolicy> scalingPolicies = newArrayList();
    String nextToken = null;
    DescribePoliciesRequest request = null;
    DescribePoliciesResult result = null;
    do {
      request = new DescribePoliciesRequest().withAutoScalingGroupName(asgName).withNextToken(nextToken);
      tracker.trackASGCall("Describe ASG Policies");
      result = amazonAutoScalingClient.describePolicies(request);
      if (isNotEmpty(result.getScalingPolicies())) {
        scalingPolicies.addAll(result.getScalingPolicies());
      }
      nextToken = result.getNextToken();
    } while (nextToken != null);
    return scalingPolicies;
  }

  @Override
  public List<String> getScalingPolicyJSONs(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, String asgName, ExecutionLogCallback logCallback) {
    try {
      logCallback.saveExecutionLog(format("Extracting scaling policy JSONs from: [%s]", asgName));
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonAutoScalingClient amazonAutoScalingClient = getAmazonAutoScalingClient(Regions.fromName(region), awsConfig);
      List<ScalingPolicy> scalingPolicies = listAllScalingPoliciesOfAsg(amazonAutoScalingClient, asgName);
      if (isEmpty(scalingPolicies)) {
        logCallback.saveExecutionLog("No policies found");
        return emptyList();
      }
      List<String> scalingPolicyJSONs = newArrayList();
      scalingPolicies.forEach(scalingPolicy -> {
        logCallback.saveExecutionLog(format("Found scaling policy: [%s]", scalingPolicy.getPolicyARN()));
        scalingPolicyJSONs.add(getJSONForScalingPolicy(scalingPolicy, logCallback));
      });
      return scalingPolicyJSONs;
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return emptyList();
  }

  @Override
  public void clearAllScalingPoliciesForAsg(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, String asgName, ExecutionLogCallback logCallback) {
    try {
      logCallback.saveExecutionLog(format("Clearing away all scaling policies for Asg: [%s]", asgName));
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonAutoScalingClient amazonAutoScalingClient = getAmazonAutoScalingClient(Regions.fromName(region), awsConfig);
      List<ScalingPolicy> scalingPolicies = listAllScalingPoliciesOfAsg(amazonAutoScalingClient, asgName);
      if (isEmpty(scalingPolicies)) {
        logCallback.saveExecutionLog("No policies found");
        return;
      }
      scalingPolicies.forEach(scalingPolicy -> {
        logCallback.saveExecutionLog(format("Found scaling policy: [%s]. Deleting it.", scalingPolicy.getPolicyARN()));
        DeletePolicyRequest deletePolicyRequest =
            new DeletePolicyRequest().withAutoScalingGroupName(asgName).withPolicyName(scalingPolicy.getPolicyARN());
        tracker.trackASGCall("Delete ASG Policies");
        amazonAutoScalingClient.deletePolicy(deletePolicyRequest);
      });
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
  }

  private ScalingPolicy getScalingPolicyFromJSON(String json, ExecutionLogCallback logCallback) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.readValue(json, ScalingPolicy.class);
    } catch (Exception ex) {
      String errorMessage = format("Exception: [%s] while desirializing cached JSON", ex.getMessage());
      logCallback.saveExecutionLog(errorMessage);
      logger.error(errorMessage, ex);
      return null;
    }
  }

  @Override
  public void attachScalingPoliciesToAsg(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, String asgName, List<String> scalingPolicyJSONs, ExecutionLogCallback logCallback) {
    try {
      logCallback.saveExecutionLog(format("Attaching scaling policies to Asg: [%s]", asgName));
      if (isEmpty(scalingPolicyJSONs)) {
        logCallback.saveExecutionLog("No policy to attach");
        return;
      }
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonAutoScalingClient amazonAutoScalingClient = getAmazonAutoScalingClient(Regions.fromName(region), awsConfig);
      scalingPolicyJSONs.forEach(scalingPolicyJSON -> {
        if (isNotEmpty(scalingPolicyJSON)) {
          ScalingPolicy scalingPolicy = getScalingPolicyFromJSON(scalingPolicyJSON, logCallback);
          if (scalingPolicy != null) {
            logCallback.saveExecutionLog(
                format("Found policy: [%s]. Attaching to: [%s]", scalingPolicy.getPolicyName(), asgName));
            PutScalingPolicyRequest putScalingPolicyRequest =
                new PutScalingPolicyRequest()
                    .withAutoScalingGroupName(asgName)
                    .withPolicyName(scalingPolicy.getPolicyName())
                    .withPolicyType(scalingPolicy.getPolicyType())
                    .withAdjustmentType(scalingPolicy.getAdjustmentType())
                    .withCooldown(scalingPolicy.getCooldown())
                    .withEstimatedInstanceWarmup(scalingPolicy.getEstimatedInstanceWarmup())
                    .withMetricAggregationType(scalingPolicy.getMetricAggregationType())
                    .withMinAdjustmentMagnitude(scalingPolicy.getMinAdjustmentMagnitude())
                    .withMinAdjustmentStep(scalingPolicy.getMinAdjustmentStep())
                    .withScalingAdjustment(scalingPolicy.getScalingAdjustment())
                    .withStepAdjustments(scalingPolicy.getStepAdjustments())
                    .withTargetTrackingConfiguration(scalingPolicy.getTargetTrackingConfiguration());
            tracker.trackASGCall("Put ASG Scaling Policy");
            PutScalingPolicyResult putScalingPolicyResult =
                amazonAutoScalingClient.putScalingPolicy(putScalingPolicyRequest);
            logCallback.saveExecutionLog(
                format("Created policy with Arn: [%s]", putScalingPolicyResult.getPolicyARN()));
          }
        }
      });
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
  }
}