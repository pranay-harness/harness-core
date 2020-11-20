package software.wings.service.intfc.aws.delegate;

import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.AwsConfig;

import java.util.List;

public interface AwsServiceDiscoveryHelperServiceDelegate {
  String getRecordValueForService(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String serviceId);
}
