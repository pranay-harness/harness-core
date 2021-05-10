package software.wings.service.intfc.aws.manager;

import static io.harness.annotations.dev.HarnessModule._410_CG_REST;
import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;

import java.util.List;

@OwnedBy(CDP)
@TargetModule(_410_CG_REST)
public interface AwsS3HelperServiceManager {
  List<String> listBucketNames(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails);
}
