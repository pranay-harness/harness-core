package software.wings.service.intfc.aws.delegate;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;

import java.util.List;

@TargetModule(Module._930_DELEGATE_TASKS)
public interface AwsServiceDiscoveryHelperServiceDelegate {
  String getRecordValueForService(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String serviceId);
}
