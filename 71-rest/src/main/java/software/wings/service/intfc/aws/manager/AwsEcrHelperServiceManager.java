package software.wings.service.intfc.aws.manager;

import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.AwsConfig;

import java.util.List;

public interface AwsEcrHelperServiceManager {
  String getAmazonEcrAuthToken(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String awsAccount, String region, String appId);
  String getEcrImageUrl(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String imageName, String appId);
}