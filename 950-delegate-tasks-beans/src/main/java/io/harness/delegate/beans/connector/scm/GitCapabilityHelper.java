package io.harness.delegate.beans.connector.scm;

import io.harness.delegate.beans.connector.ConnectorCapabilityBaseHelper;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.SocketConnectivityExecutionCapability;
import io.harness.exception.UnknownEnumTypeException;
import io.harness.expression.ExpressionEvaluator;
import io.harness.git.GitClientHelper;
import io.harness.helper.ScmGitCapabilityHelper;

import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class GitCapabilityHelper extends ConnectorCapabilityBaseHelper {
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(
      ExpressionEvaluator maskingEvaluator, GitConfigDTO gitConfig) {
    List<ExecutionCapability> capabilityList = new ArrayList<>();
    GitAuthType gitAuthType = gitConfig.getGitAuthType();
    switch (gitAuthType) {
      case HTTP:
        capabilityList.addAll(ScmGitCapabilityHelper.getHttpConnectionCapability(gitConfig));
        break;
      case SSH:
        capabilityList.add(SocketConnectivityExecutionCapability.builder()
                               .hostName(getGitSSHHostname(gitConfig))
                               .port(getGitSSHPort(gitConfig))
                               .build());
        break;
      default:
        throw new UnknownEnumTypeException("gitAuthType", gitAuthType.getDisplayName());
    }

    populateDelegateSelectorCapability(capabilityList, gitConfig.getDelegateSelectors());
    return capabilityList;
  }

  private String getGitSSHHostname(GitConfigDTO gitConfigDTO) {
    return GitClientHelper.getGitSCM(gitConfigDTO.getUrl());
  }

  private String getGitSSHPort(GitConfigDTO gitConfigDTO) {
    String port = GitClientHelper.getGitSCMPort(gitConfigDTO.getUrl());
    return port != null ? port : "22";
  }
}
