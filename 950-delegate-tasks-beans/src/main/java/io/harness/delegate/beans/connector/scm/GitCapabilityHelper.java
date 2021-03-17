package io.harness.delegate.beans.connector.scm;

import io.harness.delegate.beans.connector.ConnectorCapabilityBaseHelper;
import io.harness.delegate.beans.connector.scm.adapter.ScmConnectorMapper;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.GitConnectionNGCapability;
import io.harness.expression.ExpressionEvaluator;
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
    capabilityList.add(GitConnectionNGCapability.builder()
                           .encryptedDataDetails(encryptionDetails)
                           .gitConfig(ScmConnectorMapper.toGitConfigDTO(gitConfig))
                           .sshKeySpecDTO(sshKeySpecDTO)
                           .build());
    populateDelegateSelectorCapability(capabilityList, gitConfig.getDelegateSelectors());
    return capabilityList;
  }
}
