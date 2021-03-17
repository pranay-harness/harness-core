package io.harness.delegate.beans.connector.awscodecommitconnector;

import static java.util.Collections.singletonList;

import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitConnectorDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AwsCodeCommitValidationParams implements ConnectorValidationParams, ExecutionCapabilityDemander {
  private final String AWS_URL = "https://aws.amazon.com/";
  private AwsCodeCommitConnectorDTO awsConnector;
  private List<EncryptedDataDetail> encryptionDetails;
  private String connectorName;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return singletonList(
        HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(AWS_URL, maskingEvaluator));
  }

  @Override
  public ConnectorType getConnectorType() {
    return ConnectorType.CODECOMMIT;
  }
}
