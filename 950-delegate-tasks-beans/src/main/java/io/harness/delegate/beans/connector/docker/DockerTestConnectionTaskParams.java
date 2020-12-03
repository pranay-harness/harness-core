package io.harness.delegate.beans.connector.docker;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DockerTestConnectionTaskParams implements TaskParameters, ExecutionCapabilityDemander {
  DockerConnectorDTO dockerConnector;
  private List<EncryptedDataDetail> encryptionDetails;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return dockerConnector.fetchRequiredExecutionCapabilities(maskingEvaluator);
  }
}
