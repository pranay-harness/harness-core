package software.wings.service.intfc;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup;
import software.wings.beans.AwsConfig;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by anubhaw on 6/17/18.
 */
public interface AwsEc2Service {
  @DelegateTaskType(TaskType.AWS_VALIDATE)
  boolean validateAwsAccountCredential(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails);

  @DelegateTaskType(TaskType.AWS_GET_REGIONS)
  List<String> getRegions(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails);

  @DelegateTaskType(TaskType.AWS_GET_CLUSTERS)
  List<String> getClusters(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region);

  @DelegateTaskType(TaskType.AWS_GET_VPCS)
  List<String> getVPCs(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region);

  @DelegateTaskType(TaskType.AWS_GET_IAM_ROLES)
  Map<String, String> getIAMRoles(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails);

  @DelegateTaskType(TaskType.AWS_GET_ECR_IMAGE_URL)
  String getEcrImageUrl(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String imageName);

  @DelegateTaskType(TaskType.AWS_GET_ECR_AUTH_TOKEN)
  String getAmazonEcrAuthToken(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String awsAccount, String region);

  @DelegateTaskType(TaskType.AWS_GET_SUBNETS)
  List<String> getSubnets(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, List<String> vpcIds);

  @DelegateTaskType(TaskType.AWS_GET_SGS)
  List<String> getSGs(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, List<String> vpcIds);

  @DelegateTaskType(TaskType.AWS_GET_TAGS)
  Set<String> getTags(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region);

  @DelegateTaskType(TaskType.AWS_DESCRIBE_AUTO_SCALING_GROUP_INSTANCES)
  DescribeInstancesResult describeAutoScalingGroupInstances(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String autoScalingGroupName);

  @DelegateTaskType(TaskType.AWS_DESCRIBE_EC2_INSTANCES)
  DescribeInstancesResult describeEc2Instances(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, DescribeInstancesRequest describeInstancesRequest);

  @DelegateTaskType(TaskType.AWS_GET_IAM_INSTANCE_ROLES) List<String> getIAMInstanceRoles(AwsConfig awsConfig);

  @DelegateTaskType(TaskType.AWS_GET_APP_LBS)
  List<String> getApplicationLoadBalancers(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region);

  @DelegateTaskType(TaskType.AWS_GET_CLASSIC_LBS)
  List<String> getClassicLoadBalancers(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region);

  @DelegateTaskType(TaskType.AWS_GET_TARGET_GROUPS_FOR_ALBS)
  List<TargetGroup> getTargetGroupsForAlb(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String loadBalancerName);

  @DelegateTaskType(TaskType.AWS_GET_AUTO_SCALING_GROUPS)
  List<AutoScalingGroup> getAutoScalingGroups(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region);
}
