package software.wings.service.intfc.aws.delegate;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.AwsConfig;

import java.util.List;

@OwnedBy(CDC)
public interface AwsEcrHelperServiceDelegate {
  String getEcrImageUrl(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String imageName);
  String getAmazonEcrAuthToken(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String awsAccount, String region);
}