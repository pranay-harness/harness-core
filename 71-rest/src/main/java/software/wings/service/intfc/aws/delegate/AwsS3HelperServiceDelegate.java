package software.wings.service.intfc.aws.delegate;

import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.AwsConfig;

import java.util.List;

public interface AwsS3HelperServiceDelegate {
  List<String> listBucketNames(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails);
}
