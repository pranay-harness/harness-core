package io.harness.delegate.beans.connector.k8Connector;

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
public class KubernetesConnectionTaskParams implements TaskParameters, ExecutionCapabilityDemander {
  KubernetesClusterConfigDTO kubernetesClusterConfig;
  private List<EncryptedDataDetail> encryptionDetails;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return kubernetesClusterConfig.fetchRequiredExecutionCapabilities(maskingEvaluator);
  }
}
