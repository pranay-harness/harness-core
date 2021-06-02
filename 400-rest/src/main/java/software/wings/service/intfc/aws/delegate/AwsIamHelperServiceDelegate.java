package software.wings.service.intfc.aws.delegate;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;

import java.util.List;
import java.util.Map;

@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@OwnedBy(CDP)
public interface AwsIamHelperServiceDelegate {
  Map<String, String> listIAMRoles(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails);
  List<String> listIamInstanceRoles(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails);
}
