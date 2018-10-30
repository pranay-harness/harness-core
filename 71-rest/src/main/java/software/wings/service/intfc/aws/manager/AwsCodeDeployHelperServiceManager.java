package software.wings.service.intfc.aws.manager;

import com.amazonaws.services.ec2.model.Instance;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.aws.model.AwsCodeDeployS3LocationData;

import java.util.List;

public interface AwsCodeDeployHelperServiceManager {
  List<String> listApplications(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String appId);
  List<String> listDeploymentConfiguration(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptedDataDetails, String region, String appId);
  List<String> listDeploymentGroups(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptedDataDetails, String region, String appName, String appId);
  List<Instance> listDeploymentInstances(AwsConfig awsConfig, List<EncryptedDataDetail> encryptedDataDetails,
      String region, String deploymentId, String appId);
  AwsCodeDeployS3LocationData listAppRevision(AwsConfig awsConfig, List<EncryptedDataDetail> encryptedDataDetails,
      String region, String appName, String deploymentGroupName, String appId);
}