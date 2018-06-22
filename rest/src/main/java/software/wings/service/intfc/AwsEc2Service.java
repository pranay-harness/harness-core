package software.wings.service.intfc;

import software.wings.beans.AwsConfig;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;
import java.util.Map;

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
}
