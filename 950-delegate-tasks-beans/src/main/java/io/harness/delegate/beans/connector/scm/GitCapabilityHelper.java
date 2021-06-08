package io.harness.delegate.beans.connector.scm;

import io.harness.delegate.beans.connector.ConnectorCapabilityBaseHelper;
import io.harness.delegate.beans.connector.scm.adapter.ScmConnectorMapper;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.GitConnectionNGCapability;
import io.harness.exception.UnknownEnumTypeException;
import io.harness.expression.ExpressionEvaluator;
import io.harness.helper.ScmGitCapabilityHelper;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class GitCapabilityHelper extends ConnectorCapabilityBaseHelper {
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator,
      GitConfigDTO gitConfig, List<EncryptedDataDetail> encryptionDetails, SSHKeySpecDTO sshKeySpecDTO) {
    List<ExecutionCapability> capabilityList = new ArrayList<>();
    GitAuthType gitAuthType = gitConfig.getGitAuthType();
    switch (gitAuthType) {
      case HTTP:
        capabilityList.addAll(ScmGitCapabilityHelper.getHttpConnectionCapability(gitConfig));
        break;
      case SSH:
        capabilityList.add(GitConnectionNGCapability.builder()
                               .encryptedDataDetails(encryptionDetails)
                               .gitConfig(ScmConnectorMapper.toGitConfigDTO(gitConfig))
                               .sshKeySpecDTO(sshKeySpecDTO)
                               .build());
        break;
      default:
        throw new UnknownEnumTypeException("gitAuthType", gitAuthType.getDisplayName());
    }

    populateDelegateSelectorCapability(capabilityList, gitConfig.getDelegateSelectors());
    return capabilityList;
  }
}
