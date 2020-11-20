package software.wings.service.intfc.aws.manager;

import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.AwsConfig;

import java.util.List;
import java.util.Map;

public interface AwsIamHelperServiceManager {
  Map<String, String> listIamRoles(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String appId);
  List<String> listIamInstanceRoles(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String appId);
}
