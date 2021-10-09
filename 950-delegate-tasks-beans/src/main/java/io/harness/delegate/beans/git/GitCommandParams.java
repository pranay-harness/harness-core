package io.harness.delegate.beans.git;

import static io.harness.annotations.dev.HarnessTeam.CI;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.ConnectorTaskParams;
import io.harness.delegate.beans.connector.scm.GitCapabilityHelper;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.expression.ExpressionEvaluator;
import io.harness.git.model.GitBaseRequest;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@OwnedBy(CI)
public class GitCommandParams extends ConnectorTaskParams implements TaskParameters, ExecutionCapabilityDemander {
  GitConfigDTO gitConfig;
  ScmConnector scmConnector;
  GitCommandType gitCommandType;
  List<EncryptedDataDetail> encryptionDetails;
  GitBaseRequest gitCommandRequest;
  SSHKeySpecDTO sshKeySpecDTO;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return GitCapabilityHelper.fetchRequiredExecutionCapabilitiesSimpleCheck(gitConfig);
  }
}
