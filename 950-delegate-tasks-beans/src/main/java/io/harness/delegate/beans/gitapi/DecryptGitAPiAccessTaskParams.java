package io.harness.delegate.beans.gitapi;

import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.task.TaskParameters;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DecryptGitAPiAccessTaskParams implements TaskParameters {
  ScmConnector scmConnector;
  List<EncryptedDataDetail> encryptedDataDetails;
}
