package io.harness.delegate.task.git;

import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.task.shell.SshSessionConfigMapper;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.SecretDecryptionService;
import io.harness.shell.SshSessionConfig;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;

@Singleton
public class GitDecryptionHelper {
  @Inject private SecretDecryptionService decryptionService;
  @Inject private SshSessionConfigMapper sshSessionConfigMapper;

  public void decryptGitConfig(GitConfigDTO gitConfig, List<EncryptedDataDetail> encryptionDetails) {
    decryptionService.decrypt(gitConfig.getGitAuth(), encryptionDetails);
  }

  public SshSessionConfig getSSHSessionConfig(
      SSHKeySpecDTO sshKeySpecDTO, List<EncryptedDataDetail> encryptionDetails) {
    if (sshKeySpecDTO == null) {
      return null;
    }
    return sshSessionConfigMapper.getSSHSessionConfig(sshKeySpecDTO, encryptionDetails);
  }
}
