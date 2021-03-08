package io.harness.delegate.beans.nexus;

import io.harness.delegate.beans.connector.nexusconnector.NexusConnectorDTO;
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
public class NexusTaskParams implements ExecutionCapabilityDemander, TaskParameters {
  NexusConnectorDTO nexusConnectorDTO;
  List<EncryptedDataDetail> encryptedDataDetails;
  TaskType taskType;

  public enum TaskType { VALIDATE }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return NexusCapabilityHelper.fetchRequiredExecutionCapabilities(nexusConnectorDTO, maskingEvaluator);
  }
}
