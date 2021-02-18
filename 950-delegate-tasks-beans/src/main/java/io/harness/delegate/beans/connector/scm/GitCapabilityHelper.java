package io.harness.delegate.beans.connector.scm;

import io.harness.delegate.beans.connector.scm.adapter.ScmConnectorMapper;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.GitConnectionNGCapability;
import io.harness.delegate.beans.executioncapability.GitInstallationCapability;
import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;
import io.harness.expression.ExpressionEvaluator;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class GitCapabilityHelper {
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator,
      GitConfigDTO gitConfig, List<EncryptedDataDetail> encryptionDetails, SSHKeySpecDTO sshKeySpecDTO,
      boolean mergeCapabilities) {
    if (mergeCapabilities) {
      List<ExecutionCapability> executionCapabilities = new ArrayList<>();
      executionCapabilities.add(GitInstallationCapability.builder().build());
      executionCapabilities.add(HttpConnectionExecutionCapability.builder().url(gitConfig.getUrl()).build());
      return executionCapabilities;
    }
    return Collections.singletonList(GitConnectionNGCapability.builder()
                                         .encryptedDataDetails(encryptionDetails)
                                         .gitConfig(ScmConnectorMapper.toGitConfigDTO(gitConfig))
                                         .sshKeySpecDTO(sshKeySpecDTO)
                                         .build());
  }
}
