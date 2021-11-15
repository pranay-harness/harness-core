package io.harness.connector.task.shell;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.connector.DecryptableEntityHelper;
import io.harness.ng.core.DecryptableEntityWithEncryptionConsumers;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.remote.client.NGRestClientExecutor;
import io.harness.secrets.remote.SecretNGManagerClient;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.shell.SshSessionConfig;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;

@OwnedBy(CDP)
@Singleton
public class SSHSessionConfigMapperViaManager extends AbstractSSHSessionConfigMapper {
  @Inject SecretNGManagerClient ngSecretDecryptionClient;
  @Inject NGRestClientExecutor restClientExecutor;
  @Inject DecryptableEntityHelper decryptableEntityHelper;

  public SshSessionConfig getSSHSessionConfig(
      SSHKeySpecDTO sshKeySpecDTO, List<EncryptedDataDetail> encryptionDetails) {
    return super.getSSHSessionConfig(sshKeySpecDTO, encryptionDetails);
  }

  @Override
  public DecryptableEntity decryptConfig(
      DecryptableEntity decryptableEntity, List<EncryptedDataDetail> encryptionDetails) {
    final DecryptableEntityWithEncryptionConsumers build =
        decryptableEntityHelper.buildDecryptableEntityWithEncryptionConsumers(decryptableEntity, encryptionDetails);
    final DecryptableEntity decryptedGitAuth =
        restClientExecutor.getResponse(ngSecretDecryptionClient.decryptEncryptedDetails(build, null));
    return decryptedGitAuth;
  }
}
