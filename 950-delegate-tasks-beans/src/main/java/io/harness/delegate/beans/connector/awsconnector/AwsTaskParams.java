package io.harness.delegate.beans.connector.awsconnector;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AwsTaskParams implements TaskParameters, ExecutionCapabilityDemander {
  AwsConnectorDTO awsConnector;
  AwsTaskType awsTaskType;
  List<EncryptedDataDetail> encryptionDetails;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return AwsCapabilityHelper.fetchRequiredExecutionCapabilities(maskingEvaluator, awsConnector);
  }
}
