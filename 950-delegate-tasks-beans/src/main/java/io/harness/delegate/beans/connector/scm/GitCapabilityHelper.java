package io.harness.delegate.beans.connector.scm;

import io.harness.delegate.beans.connector.scm.adapter.ScmConnectorMapper;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.GitConnectionNGCapability;
import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;
import io.harness.delegate.beans.executioncapability.SSHHostValidationCapability;
import io.harness.expression.ExpressionEvaluator;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.transport.URIish;

@Slf4j
@UtilityClass
public class GitCapabilityHelper {
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator,
      GitConfigDTO gitConfig, List<EncryptedDataDetail> encryptionDetails, SSHKeySpecDTO sshKeySpecDTO,
      boolean mergeCapabilities) {
    if (mergeCapabilities) {
      try {
        List<ExecutionCapability> executionCapabilities = new ArrayList<>();
        URIish uri = new URIish(gitConfig.getUrl());
        if (uri.getScheme() == "http" || uri.getScheme() == "https") {
          executionCapabilities.add(HttpConnectionExecutionCapability.builder().url(gitConfig.getUrl()).build());
        } else {
          executionCapabilities.add(
              SSHHostValidationCapability.builder().host(uri.getHost()).port(uri.getPort()).build());
        }
        return executionCapabilities;
      } catch (Exception e) {
        log.error("malformed git capability url: " + gitConfig.getUrl());
        return new ArrayList<>();
      }
    }
    return Collections.singletonList(GitConnectionNGCapability.builder()
                                         .encryptedDataDetails(encryptionDetails)
                                         .gitConfig(ScmConnectorMapper.toGitConfigDTO(gitConfig))
                                         .sshKeySpecDTO(sshKeySpecDTO)
                                         .build());
  }
}
