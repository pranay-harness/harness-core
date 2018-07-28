package software.wings.service.impl.aws.delegate;

import static io.harness.threading.Morpheus.sleep;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.ErrorCode.INIT_TIMEOUT;
import static software.wings.beans.Log.LogLevel.ERROR;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder;
import com.amazonaws.services.autoscaling.model.Activity;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupResult;
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationResult;
import com.amazonaws.services.autoscaling.model.DeleteAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.DeleteLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.DescribeLaunchConfigurationsRequest;
import com.amazonaws.services.autoscaling.model.DescribeScalingActivitiesRequest;
import com.amazonaws.services.autoscaling.model.DescribeScalingActivitiesResult;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.amazonaws.services.autoscaling.model.SetDesiredCapacityRequest;
import com.amazonaws.services.ec2.model.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AwsConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.command.LogCallback;
import software.wings.exception.InvalidRequestException;
import software.wings.exception.WingsException;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.aws.delegate.AwsAsgHelperServiceDelegate;
import software.wings.service.intfc.aws.delegate.AwsEc2HelperServiceDelegate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Singleton
public class AwsAsgHelperServiceDelegateImpl
    extends AwsHelperServiceDelegateBase implements AwsAsgHelperServiceDelegate {
  private static final Logger logger = LoggerFactory.getLogger(AwsAsgHelperServiceDelegateImpl.class);
  private static final long AUTOSCALING_REQUEST_STATUS_CHECK_INTERVAL = TimeUnit.SECONDS.toSeconds(15);
  @Inject private AwsEc2HelperServiceDelegate awsEc2HelperServiceDelegate;
  @Inject private TimeLimiter timeLimiter;

  @VisibleForTesting
  AmazonAutoScalingClient getAmazonAutoScalingClient(Regions region, String accessKey, char[] secretKey) {
    return (AmazonAutoScalingClient) AmazonAutoScalingClientBuilder.standard()
        .withRegion(region)
        .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, new String(secretKey))))
        .build();
  }

  @Override
  public List<String> listAutoScalingGroupNames(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    try {
      List<AutoScalingGroup> result = listAllAsgs(awsConfig, encryptionDetails, region);
      return result.stream().map(AutoScalingGroup::getAutoScalingGroupName).collect(toList());
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
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
      AmazonAutoScalingClient amazonAutoScalingClient =
          getAmazonAutoScalingClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());
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
      AmazonAutoScalingClient amazonAutoScalingClient =
          getAmazonAutoScalingClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());
      DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult =
          amazonAutoScalingClient.describeAutoScalingGroups(describeAutoScalingGroupsRequest);
      if (!describeAutoScalingGroupsResult.getAutoScalingGroups().isEmpty()) {
        return describeAutoScalingGroupsResult.getAutoScalingGroups().get(0);
      }
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return null;
  }

  @Override
  public LaunchConfiguration getLaunchConfiguration(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String launchConfigName) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonAutoScalingClient amazonAutoScalingClient =
          getAmazonAutoScalingClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());
      return amazonAutoScalingClient
          .describeLaunchConfigurations(
              new DescribeLaunchConfigurationsRequest().withLaunchConfigurationNames(launchConfigName))
          .getLaunchConfigurations()
          .stream()
          .findFirst()
          .orElse(null);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return null;
  }

  @Override
  public List<AutoScalingGroup> listAllAsgs(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonAutoScalingClient amazonAutoScalingClient =
          getAmazonAutoScalingClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());

      DescribeAutoScalingGroupsRequest describeAutoScalingGroupsRequest;
      DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult;
      String nextToken = null;
      List<AutoScalingGroup> result = new ArrayList<>();
      do {
        describeAutoScalingGroupsRequest =
            new DescribeAutoScalingGroupsRequest().withMaxRecords(100).withNextToken(nextToken);
        describeAutoScalingGroupsResult =
            amazonAutoScalingClient.describeAutoScalingGroups(describeAutoScalingGroupsRequest);
        result.addAll(describeAutoScalingGroupsResult.getAutoScalingGroups());
        nextToken = describeAutoScalingGroupsResult.getNextToken();
      } while (nextToken != null);
      return result;
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return emptyList();
  }

  @Override
  public void deleteLaunchConfig(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String autoScalingGroupName) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonAutoScalingClient amazonAutoScalingClient =
          getAmazonAutoScalingClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());
      amazonAutoScalingClient.deleteLaunchConfiguration(
          new DeleteLaunchConfigurationRequest().withLaunchConfigurationName(autoScalingGroupName));
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
  }

  @Override
  public CreateLaunchConfigurationResult createLaunchConfiguration(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region,
      CreateLaunchConfigurationRequest createLaunchConfigurationRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonAutoScalingClient amazonAutoScalingClient =
          getAmazonAutoScalingClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());
      return amazonAutoScalingClient.createLaunchConfiguration(createLaunchConfigurationRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
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
      amazonAutoScalingClient =
          getAmazonAutoScalingClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());
      return amazonAutoScalingClient.createAutoScalingGroup(createAutoScalingGroupRequest);
    } catch (AmazonServiceException amazonServiceException) {
      if (amazonAutoScalingClient != null && logCallback != null) {
        describeAutoScalingGroupActivities(amazonAutoScalingClient,
            createAutoScalingGroupRequest.getAutoScalingGroupName(), new HashSet<>(), logCallback, true);
      }
      handleAmazonServiceException(amazonServiceException);
    }
    return new CreateAutoScalingGroupResult();
  }

  @Override
  public void deleteAutoScalingGroups(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      List<AutoScalingGroup> autoScalingGroups, LogCallback callback) {
    AmazonAutoScalingClient amazonAutoScalingClient =
        getAmazonAutoScalingClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      autoScalingGroups.forEach(autoScalingGroup -> {
        try {
          amazonAutoScalingClient.deleteAutoScalingGroup(
              new DeleteAutoScalingGroupRequest().withAutoScalingGroupName(autoScalingGroup.getAutoScalingGroupName()));
          waitForAutoScalingGroupToBeDeleted(amazonAutoScalingClient, autoScalingGroup, callback);
        } catch (Exception ignored) {
          describeAutoScalingGroupActivities(
              amazonAutoScalingClient, autoScalingGroup.getAutoScalingGroupName(), new HashSet<>(), callback, true);
          logger.warn("Failed to delete ASG: [{}] [{}]", autoScalingGroup.getAutoScalingGroupName(), ignored);
        }
        try {
          amazonAutoScalingClient.deleteLaunchConfiguration(
              new DeleteLaunchConfigurationRequest().withLaunchConfigurationName(
                  autoScalingGroup.getLaunchConfigurationName()));
        } catch (Exception ignored) {
          describeAutoScalingGroupActivities(
              amazonAutoScalingClient, autoScalingGroup.getAutoScalingGroupName(), new HashSet<>(), callback, true);
          logger.warn("Failed to delete ASG: [{}] [{}]", autoScalingGroup.getAutoScalingGroupName(), ignored);
        }
      });
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
  }

  @Override
  public Map<String, Integer> getDesiredCapacitiesOfAsgs(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, List<String> asgs) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      Map<String, Integer> capacities = Maps.newHashMap();
      AmazonAutoScalingClient amazonAutoScalingClient =
          getAmazonAutoScalingClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());
      String nextToken = null;
      DescribeAutoScalingGroupsRequest request = null;
      DescribeAutoScalingGroupsResult result = null;
      do {
        request = new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(asgs).withNextToken(nextToken);
        result = amazonAutoScalingClient.describeAutoScalingGroups(request);
        List<AutoScalingGroup> groups = result.getAutoScalingGroups();
        groups.forEach(group -> { capacities.put(group.getAutoScalingGroupName(), group.getDesiredCapacity()); });
        nextToken = result.getNextToken();
      } while (nextToken != null);
      return capacities;
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return emptyMap();
  }

  @Override
  public void setAutoScalingGroupCapacityAndWaitForInstancesReadyState(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region, String autoScalingGroupName, Integer desiredCapacity,
      ExecutionLogCallback logCallback, Integer autoScalingSteadyStateTimeout) {
    encryptionService.decrypt(awsConfig, encryptionDetails);
    AmazonAutoScalingClient amazonAutoScalingClient =
        getAmazonAutoScalingClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());
    try {
      logCallback.saveExecutionLog(
          format("Set AutoScaling Group: [%s] desired capacity to [%s]", autoScalingGroupName, desiredCapacity));
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
    }
  }

  private void waitForAutoScalingGroupToBeDeleted(
      AmazonAutoScalingClient amazonAutoScalingClient, AutoScalingGroup autoScalingGroup, LogCallback callback) {
    try {
      timeLimiter.callWithTimeout(() -> {
        Set<String> completedActivities = new HashSet<>();
        while (true) {
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
      logger.info("Not describing autoScalingGroupActivities for [%s] since logCallback is null");
      return;
    }
    try {
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
              String statusMessage = activity.getStatusMessage();
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
            getAmazonAutoScalingClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());
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

  private boolean allInstanceInReadyState(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, List<String> instanceIds, ExecutionLogCallback logCallback) {
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
}
