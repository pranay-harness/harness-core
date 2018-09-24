package software.wings.service.intfc.aws.manager;

import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

public interface AwsEcsHelperServiceManager {
  List<String> listClusters(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region);
}
