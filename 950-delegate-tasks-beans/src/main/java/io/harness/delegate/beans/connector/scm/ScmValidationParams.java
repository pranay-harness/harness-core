package io.harness.delegate.beans.connector.scm;

import static io.harness.annotations.dev.HarnessTeam.CI;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.expression.ExpressionEvaluator;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(CI)
public class ScmValidationParams implements ConnectorValidationParams, ExecutionCapabilityDemander {
  ScmConnector scmConnector;
  GitConfigDTO gitConfigDTO;
  List<EncryptedDataDetail> encryptedDataDetails;
  SSHKeySpecDTO sshKeySpecDTO;
  String connectorName;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return GitCapabilityHelper.fetchRequiredExecutionCapabilitiesSimpleCheck(gitConfigDTO);
  }

  @Override
  public ConnectorType getConnectorType() {
    return ConnectorType.GIT;
  }
}
