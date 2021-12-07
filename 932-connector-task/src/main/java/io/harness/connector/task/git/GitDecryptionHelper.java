package io.harness.connector.task.git;

import static io.harness.connector.helper.GitApiAccessDecryptionHelper.getAPIAccessDecryptableEntity;
import static io.harness.connector.helper.GitApiAccessDecryptionHelper.hasApiAccess;

import io.harness.beans.DecryptableEntity;
import io.harness.connector.helper.DecryptionHelper;
import io.harness.connector.helper.GitApiAccessDecryptionHelper;
import io.harness.connector.task.shell.SshSessionConfigMapper;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.shell.SshSessionConfig;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;

@Singleton
public class GitDecryptionHelper {
  @Inject DecryptionHelper decryptionHelper;
  @Inject SshSessionConfigMapper sshSessionConfigMapper;

  public void decryptGitConfig(GitConfigDTO gitConfig, List<EncryptedDataDetail> encryptionDetails) {
    final DecryptableEntity decryptedGitAuth = decryptionHelper.decrypt(gitConfig.getGitAuth(), encryptionDetails);
    gitConfig.setGitAuth((GitAuthenticationDTO) decryptedGitAuth);
  }

  public SshSessionConfig getSSHSessionConfig(
      SSHKeySpecDTO sshKeySpecDTO, List<EncryptedDataDetail> encryptionDetails) {
    if (sshKeySpecDTO == null) {
      return null;
    }
    return sshSessionConfigMapper.getSSHSessionConfig(sshKeySpecDTO, encryptionDetails);
  }

  public void decryptApiAccessConfig(ScmConnector scmConnector, List<EncryptedDataDetail> encryptionDetails) {
    if (hasApiAccess(scmConnector)) {
      DecryptableEntity apiAccessDecryptableEntity = getAPIAccessDecryptableEntity(scmConnector);
      final DecryptableEntity decryptedScmSpec =
          decryptionHelper.decrypt(apiAccessDecryptableEntity, encryptionDetails);
      GitApiAccessDecryptionHelper.setAPIAccessDecryptableEntity(scmConnector, decryptedScmSpec);
    }
  }
}
