package io.harness.delegate.beans.connector.scm;

import static io.harness.annotations.dev.HarnessTeam.CI;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.ConnectorCapabilityBaseHelper;
import io.harness.delegate.beans.connector.scm.adapter.ScmConnectorMapper;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfig;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.GitConnectionNGCapability;
import io.harness.delegate.beans.executioncapability.SocketConnectivityExecutionCapability;
import io.harness.exception.UnknownEnumTypeException;
import io.harness.git.GitClientHelper;
import io.harness.helper.ScmGitCapabilityHelper;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(CI)
public class GitCapabilityHelper extends ConnectorCapabilityBaseHelper {
  public List<ExecutionCapability> fetchRequiredExecutionCapabilitiesSimpleCheck(GitConfig gitConfig) {
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

  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(
      GitConfig gitConfig, List<EncryptedDataDetail> encryptionDetails, SSHKeySpecDTO sshKeySpecDTO) {
    List<ExecutionCapability> capabilityList = new ArrayList<>();
    capabilityList.add(GitConnectionNGCapability.builder()
                           .encryptedDataDetails(encryptionDetails)
                           .gitConfig(ScmConnectorMapper.toGitConfigDTO(gitConfig))
                           .sshKeySpecDTO(sshKeySpecDTO)
                           .build());
    populateDelegateSelectorCapability(capabilityList, gitConfig.getDelegateSelectors());
    return capabilityList;
  }

  private String getGitSSHHostname(GitConfig gitConfig) {
    String url = gitConfig.getUrl();
    if (gitConfig.getGitConnectionType() == GitConnectionType.ACCOUNT && !url.endsWith("/")) {
      url += "/";
    }
    return GitClientHelper.getGitSCM(url);
  }

  private String getGitSSHPort(GitConfig gitConfig) {
    String url = gitConfig.getUrl();
    if (gitConfig.getGitConnectionType() == GitConnectionType.ACCOUNT && !url.endsWith("/")) {
      url += "/";
    }
    String port = GitClientHelper.getGitSCMPort(url);
    return port != null ? port : "22";
  }
}
