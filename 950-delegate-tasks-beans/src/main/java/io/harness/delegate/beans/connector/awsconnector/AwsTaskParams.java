package io.harness.delegate.beans.connector.awsconnector;

import io.harness.delegate.beans.connector.ConnectorTaskParams;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class AwsTaskParams extends ConnectorTaskParams implements TaskParameters, ExecutionCapabilityDemander {
  AwsConnectorDTO awsConnector;
  AwsTaskType awsTaskType;
  List<EncryptedDataDetail> encryptionDetails;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return AwsCapabilityHelper.fetchRequiredExecutionCapabilities(awsConnector, maskingEvaluator);
  }
}
