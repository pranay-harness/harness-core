package software.wings.service.intfc.aws.manager;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;

import java.util.List;
import java.util.Map;

@OwnedBy(CDP)
public interface AwsIamHelperServiceManager {
  Map<String, String> listIamRoles(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String appId);
  List<String> listIamInstanceRoles(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String appId);
}
