package software.wings.service.intfc.aws.delegate;

import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

public interface AwsServiceDiscoveryHelperServiceDelegate {
  String getRecordValueForService(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String serviceId);
}