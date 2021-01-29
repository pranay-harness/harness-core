package io.harness.delegate.beans.git;

import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.task.TaskParameters;
import io.harness.git.model.GitBaseRequest;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GitCommandParams implements TaskParameters {
  ScmConnector gitConfig;
  GitCommandType gitCommandType;
  List<EncryptedDataDetail> encryptionDetails;
  GitBaseRequest gitCommandRequest;
  SSHKeySpecDTO sshKeySpecDTO;
}
