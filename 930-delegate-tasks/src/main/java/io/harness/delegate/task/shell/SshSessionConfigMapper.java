package io.harness.delegate.task.shell;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.shell.AuthenticationScheme.KERBEROS;
import static io.harness.shell.AuthenticationScheme.SSH_KEY;
import static io.harness.shell.SshSessionConfig.Builder.aSshSessionConfig;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.connector.task.shell.AbstractSSHSessionConfigMapper;
import io.harness.ng.core.dto.secrets.*;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.SecretDecryptionService;
import io.harness.shell.AccessType;
import io.harness.shell.KerberosConfig;
import io.harness.shell.KerberosConfig.KerberosConfigBuilder;
import io.harness.shell.SshSessionConfig;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;

@OwnedBy(CDP)
@Singleton
public class SshSessionConfigMapper extends AbstractSSHSessionConfigMapper {
  @Inject private SecretDecryptionService secretDecryptionService;

  public SshSessionConfig getSSHSessionConfig(
      SSHKeySpecDTO sshKeySpecDTO, List<EncryptedDataDetail> encryptionDetails) {
    return super.getSSHSessionConfig(sshKeySpecDTO, encryptionDetails);
  }

  @Override
  public DecryptableEntity decryptConfig(
      DecryptableEntity decryptableEntity, List<EncryptedDataDetail> encryptionDetails) {
    return secretDecryptionService.decrypt(decryptableEntity, encryptionDetails);
  }
}
