package io.harness.helper;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.connector.scm.adapter.ScmConnectorMapper;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;
import io.harness.exception.InvalidRequestException;

import java.util.Collections;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.DX)
public class ScmGitCapabilityHelper {
  public List<ExecutionCapability> getHttpConnectionCapability(ScmConnector scmConnector) {
    GitConfigDTO gitConfigDTO = ScmConnectorMapper.toGitConfigDTO(scmConnector);
    if (gitConfigDTO.getGitAuthType().equals(GitAuthType.HTTP)) {
      return Collections.singletonList(HttpConnectionExecutionCapability.builder().url(scmConnector.getUrl()).build());
    }
    throw new InvalidRequestException("HTTP authentication is required");
  }
}
