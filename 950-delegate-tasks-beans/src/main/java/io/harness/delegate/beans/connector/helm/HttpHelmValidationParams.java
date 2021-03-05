package io.harness.delegate.beans.connector.helm;

import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HttpHelmValidationParams implements ConnectorValidationParams, ExecutionCapabilityDemander {
  HttpHelmConnectorDTO httpHelmConnectorDTO;
  List<EncryptedDataDetail> encryptionDataDetails;
  String connectorName;

  @Override
  public ConnectorType getConnectorType() {
    return ConnectorType.HTTP_HELM_REPO;
  }

  @Override
  public String getConnectorName() {
    return connectorName;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    final String httpHelmRepoUrl = httpHelmConnectorDTO.getHelmRepoUrl();
    return Collections.singletonList(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
        httpHelmRepoUrl, maskingEvaluator));
  }
}
