package io.harness.delegate.beans.connector.scm;

import static io.harness.annotations.dev.HarnessTeam.CI;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.ConnectorCapabilityBaseHelper;
import io.harness.delegate.beans.connector.scm.adapter.ScmConnectorMapper;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.GitConnectionNGCapability;
import io.harness.delegate.beans.executioncapability.SocketConnectivityExecutionCapability;
import io.harness.exception.UnknownEnumTypeException;
import io.harness.expression.ExpressionEvaluator;
import io.harness.git.GitClientHelper;
import io.harness.helper.ScmGitCapabilityHelper;

import java.util.ArrayList;
import java.util.List;

import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(CI)
public class GitCapabilityHelper extends ConnectorCapabilityBaseHelper {
  public List<ExecutionCapability> fetchRequiredExecutionCapabilitiesSimpleCheck(GitConfigDTO gitConfig) {
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

  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(GitConfigDTO gitConfig) {
    List<ExecutionCapability> capabilityList = new ArrayList<>();

    capabilityList.add(SocketConnectivityExecutionCapability.builder()
            .hostName(getGitSSHHostname(gitConfig))
            .port(getGitSSHPort(gitConfig))
            .build());

    populateDelegateSelectorCapability(capabilityList, gitConfig.getDelegateSelectors());
    return capabilityList;
  }

  private String getGitSSHHostname(GitConfigDTO gitConfigDTO) {
    String url = gitConfigDTO.getUrl();
    if (gitConfigDTO.getGitConnectionType() == GitConnectionType.ACCOUNT && !url.endsWith("/")) {
      url += "/";
    }
    return GitClientHelper.getGitSCM(url);
  }

  private String getGitSSHPort(GitConfigDTO gitConfigDTO) {
    String url = gitConfigDTO.getUrl();
    if (gitConfigDTO.getGitConnectionType() == GitConnectionType.ACCOUNT && !url.endsWith("/")) {
      url += "/";
    }
    String port = GitClientHelper.getGitSCMPort(url);
    return port != null ? port : "22";
  }
}
