package io.harness.delegate.beans.git;

import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.beans.executioncapability.GitConnectionNGCapability;
import io.harness.delegate.beans.git.GitCommand.GitCommandType;
import io.harness.delegate.task.TaskParameters;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Value;

import java.util.Collections;
import java.util.List;

@Value
@Builder
public class GitCommandParams implements TaskParameters, ExecutionCapabilityDemander {
  GitConfigDTO gitConfig;
  GitCommandType gitCommandType;
  List<EncryptedDataDetail> encryptionDetails;
  GitCommandRequest gitCommandRequest;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return Collections.singletonList(
        GitConnectionNGCapability.builder().encryptedDataDetails(encryptionDetails).gitConfig(gitConfig).build());
  }
}
