package software.wings.service.intfc.aws.manager;

import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;
import java.util.Map;

public interface AwsIamHelperServiceManager {
  Map<String, String> listIamRoles(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails);
  List<String> listIamInstanceRoles(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails);
}