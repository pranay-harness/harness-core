package software.wings.service.intfc.aws.delegate;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.logging.LogCallback;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.service.impl.aws.model.AwsAsgGetRunningCountData;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupResult;
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationResult;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.amazonaws.services.ec2.model.Instance;
import java.util.List;
import java.util.Map;

@TargetModule(Module._930_DELEGATE_TASKS)
public interface AwsAsgHelperServiceDelegate {
  List<String> listAutoScalingGroupNames(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region);

  List<String> listAutoScalingGroupInstanceIds(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, String autoScalingGroupName, boolean isInstanceSync);

  List<Instance> listAutoScalingGroupInstances(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, String autoScalingGroupName, boolean isInstanceSync);

  AutoScalingGroup getAutoScalingGroup(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String autoScalingGroupName);
  LaunchConfiguration getLaunchConfiguration(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String launchConfigName);
  List<AutoScalingGroup> listAllAsgs(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region);
  void deleteLaunchConfig(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String autoScalingGroupName);
  CreateLaunchConfigurationResult createLaunchConfiguration(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region,
      CreateLaunchConfigurationRequest createLaunchConfigurationRequest);
  CreateAutoScalingGroupResult createAutoScalingGroup(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, CreateAutoScalingGroupRequest createAutoScalingGroupRequest, LogCallback logCallback);
  void deleteAutoScalingGroups(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      List<AutoScalingGroup> autoScalingGroups, LogCallback callback);
  Map<String, Integer> getDesiredCapacitiesOfAsgs(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, List<String> asgs);
  void setAutoScalingGroupCapacityAndWaitForInstancesReadyState(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region, String autoScalingGroupName, Integer desiredCapacity,
      ExecutionLogCallback logCallback, Integer autoScalingSteadyStateTimeout);
  void setAutoScalingGroupLimits(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      String autoScalingGroupName, Integer desiredCapacity, ExecutionLogCallback logCallback);
  void setMinInstancesForAsg(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      String autoScalingGroupName, int minCapacity, ExecutionLogCallback logCallback);
  void registerAsgWithTargetGroups(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      String asgName, List<String> targetGroupARNs, ExecutionLogCallback logCallback);
  void registerAsgWithClassicLBs(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      String asgName, List<String> classicLBs, ExecutionLogCallback logCallback);
  void deRegisterAsgWithTargetGroups(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      String asgName, List<String> targetGroupARNs, ExecutionLogCallback logCallback);
  void deRegisterAsgWithClassicLBs(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      String asgName, List<String> classicLBs, ExecutionLogCallback logCallback);
  AwsAsgGetRunningCountData getCurrentlyRunningInstanceCount(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String infraMappingId);
  List<String> getScalingPolicyJSONs(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      String asgName, ExecutionLogCallback logCallback);
  void clearAllScalingPoliciesForAsg(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      String asgName, ExecutionLogCallback logCallback);
  void attachScalingPoliciesToAsg(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      String asgName, List<String> scalingPolicyJSONs, ExecutionLogCallback logCallback);
  void addUpdateTagAutoScalingGroup(AwsConfig awsConfig, List<EncryptedDataDetail> encryptedDataDetails, String asgName,
      String region, String tagKey, String tagValue, ExecutionLogCallback executionLogCallback);
}
